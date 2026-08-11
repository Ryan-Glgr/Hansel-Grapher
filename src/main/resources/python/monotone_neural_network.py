#!/usr/bin/env python3
"""
monotonic_classifier.py

sklearn-compatible ordinal monotonic classifier using mononet's
PyTorch API.

Every input feature is constrained to be monotonically INCREASING (+1).
Any feature that should conceptually decrease the prediction as it
increases must be inverted upstream during preprocessing.

The classifier assumes class labels are contiguous integers:

    0, 1, 2, ..., K-1

and that these classes are ordinal, where larger class numbers represent
better outcomes.

The neural network produces a single monotonic latent score. Learned,
ordered thresholds convert that score into probabilities for the K
ordered classes.
"""

import sys
import numpy as np
import torch
import torch.nn as nn
from sklearn.base import BaseEstimator, ClassifierMixin
from sklearn.preprocessing import StandardScaler

from mononet.torch import MonoInput, MonoLinear
from mononet.core.types import MonotonicityMask


ELU_ACTIVATION = "elu"


class _MonotonicNet(nn.Module):
    """Monotonic network producing a single scalar score."""

    def __init__(
            self,
            num_attributes: int,
            hidden_layers: int = 64
    ):
        super().__init__()

        # the ones mask means that all attributes have positive monotonicity.
        mask = MonotonicityMask(
            np.ones(num_attributes, dtype=np.int8)
        )

        self.net = nn.Sequential(
            MonoInput(mask),

            MonoLinear(
                num_attributes,
                hidden_layers,
                activation=ELU_ACTIVATION
            ),

            MonoLinear(
                hidden_layers,
                hidden_layers,
                activation=ELU_ACTIVATION
            ),

            # One scalar monotonic score instead of one output per class.
            MonoLinear(
                hidden_layers,
                1
            ),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.net(x).squeeze(1)


class MonotonicNNClassifier(BaseEstimator, ClassifierMixin):
    """
    sklearn-compatible monotonic ordinal classifier.

    Every feature is assumed monotonically increasing.

    Class labels must be:
        0, 1, 2, ..., K-1

    where larger values represent better outcomes.

    Parameters
    ----------
    hidden : int
        Hidden layer width.

    epochs : int
        Number of training epochs.

    lr : float
        Adam learning rate.

    batch_size : int
        Training batch size.

    device : str
        "auto", "cuda", or "cpu".

    random_state : int
        Random seed.
    """

    def __init__(
            self,
            hidden=64,
            epochs=200,
            lr=1e-3,
            batch_size=64,
            device="auto",
            random_state=42
    ):
        self.hidden = hidden
        self.epochs = epochs
        self.lr = lr
        self.batch_size = batch_size
        self.device = device
        self.random_state = random_state

    def _resolve_device(self) -> str:
        if self.device == "auto":
            return "cuda" if torch.cuda.is_available() else "cpu"
        return self.device

    def _ordered_thresholds(self) -> torch.Tensor:
        """
        Convert unconstrained parameters into strictly ordered thresholds.
        """

        thresholds = self._threshold_base

        if len(self._threshold_deltas) == 0:
            return thresholds

        positive_deltas = torch.nn.functional.softplus(
            self._threshold_deltas
        )

        return torch.cat([
            thresholds,
            thresholds[-1:] + torch.cumsum(
                positive_deltas,
                dim=0
            )
        ])

    def fit(self, X: np.ndarray, y: np.ndarray):
        torch.manual_seed(self.random_state)

        X = np.asarray(X, dtype=np.float32)
        y = np.asarray(y, dtype=np.int64)

        n_features = X.shape[1]
        n_classes = int(y.max()) + 1

        # Require contiguous labels:
        # 0, 1, 2, ..., K-1
        expected_classes = np.arange(n_classes)

        if not np.array_equal(np.unique(y), expected_classes):
            raise ValueError(
                "Class labels must be contiguous integers "
                "0, 1, ..., K-1."
            )

        self.classes_ = expected_classes

        # Scaling preserves feature ordering because StandardScaler
        # divides by a positive standard deviation.
        self._scaler = StandardScaler()
        X_scaled = self._scaler.fit_transform(X)

        self._device = self._resolve_device()

        self._model = _MonotonicNet(
            n_features,
            self.hidden
        ).to(self._device)

        X_t = torch.tensor(
            X_scaled,
            dtype=torch.float32,
            device=self._device
        )

        y_t = torch.tensor(
            y,
            dtype=torch.long,
            device=self._device
        )

        # ------------------------------------------------------------
        # Ordered thresholds
        #
        # There are K-1 boundaries between K classes.
        #
        # We parameterize them so that:
        #
        #   t0 < t1 < t2 < ... < t(K-2)
        #
        # is guaranteed during training.
        # ------------------------------------------------------------

        num_thresholds = n_classes - 1

        self._threshold_base = nn.Parameter(
            torch.tensor(
                [0.0],
                dtype=torch.float32,
                device=self._device
            )
        )

        if num_thresholds > 1:
            self._threshold_deltas = nn.Parameter(
                torch.zeros(
                    num_thresholds - 1,
                    dtype=torch.float32,
                    device=self._device
                )
            )
        else:
            self._threshold_deltas = nn.Parameter(
                torch.empty(
                    0,
                    dtype=torch.float32,
                    device=self._device
                )
            )

        # Give the thresholds to the optimizer as trainable parameters.
        optimizer = torch.optim.Adam(
            list(self._model.parameters())
            + [self._threshold_base]
            + [self._threshold_deltas],
            lr=self.lr
        )

        dataset = torch.utils.data.TensorDataset(
            X_t,
            y_t
        )

        loader = torch.utils.data.DataLoader(
            dataset,
            batch_size=self.batch_size,
            shuffle=True
        )

        self._model.train()

        for epoch in range(self.epochs):

            epoch_loss = 0.0

            for xb, yb in loader:

                optimizer.zero_grad()

                # One scalar score per example.
                scores = self._model(xb)

                thresholds = self._ordered_thresholds()

                # Shape:
                #
                # scores:    [batch]
                # thresholds [K-1]
                #
                # Result:
                # ordinal_logits: [batch, K-1]
                ordinal_logits = (
                        scores.unsqueeze(1)
                        - thresholds.unsqueeze(0)
                )

                # For class y, the target is:
                #
                # class 0 -> [0, 0, 0, ...]
                # class 1 -> [1, 0, 0, ...]
                # class 2 -> [1, 1, 0, ...]
                # ...
                #
                # Each position asks:
                #
                # "Is Y greater than this threshold?"
                threshold_indices = torch.arange(
                    num_thresholds,
                    device=self._device
                )

                targets = (
                        yb.unsqueeze(1) > threshold_indices
                ).float()

                loss = nn.functional.binary_cross_entropy_with_logits(
                    ordinal_logits,
                    targets
                )

                loss.backward()
                optimizer.step()

                epoch_loss += loss.item() * xb.size(0)

            if (epoch + 1) % max(1, self.epochs // 10) == 0:
                print(
                    f"[MonotonicNNClassifier] "
                    f"epoch {epoch + 1}/{self.epochs} "
                    f"loss={epoch_loss / len(dataset):.4f}",
                    file=sys.stderr
                )

        self._model.eval()

        return self

    def _class_probabilities(
            self,
            scores: torch.Tensor
    ) -> torch.Tensor:

        thresholds = self._ordered_thresholds()

        # P(Y > k)
        cumulative = torch.sigmoid(
            scores.unsqueeze(1)
            - thresholds.unsqueeze(0)
        )

        # Convert cumulative probabilities into individual
        # class probabilities.
        #
        # For K=5:
        #
        # p0 = 1 - P(Y>0)
        # p1 = P(Y>0) - P(Y>1)
        # p2 = P(Y>1) - P(Y>2)
        # p3 = P(Y>2) - P(Y>3)
        # p4 = P(Y>3)

        first = 1.0 - cumulative[:, 0:1]

        middle = (
                cumulative[:, :-1]
                - cumulative[:, 1:]
        )

        last = cumulative[:, -1:]

        probabilities = torch.cat(
            [first, middle, last],
            dim=1
        )

        # Protect against tiny floating-point negative values.
        probabilities = torch.clamp(
            probabilities,
            min=0.0
        )

        # Renormalize to guarantee rows sum to 1.
        probabilities = (
                probabilities
                / probabilities.sum(dim=1, keepdim=True)
        )

        return probabilities

    def predict_proba(self, X: np.ndarray) -> np.ndarray:

        X = np.asarray(X, dtype=np.float32)

        X_scaled = self._scaler.transform(X)

        X_t = torch.tensor(
            X_scaled,
            dtype=torch.float32,
            device=self._device
        )

        with torch.no_grad():

            scores = self._model(X_t)

            probs = self._class_probabilities(
                scores
            )

        return probs.cpu().numpy()

    def predict(self, X: np.ndarray) -> np.ndarray:

        probs = self.predict_proba(X)

        return np.argmax(probs, axis=1)

    def score(self, X: np.ndarray, y: np.ndarray) -> float:

        y = np.asarray(y)

        return float(
            np.mean(self.predict(X) == y)
        )
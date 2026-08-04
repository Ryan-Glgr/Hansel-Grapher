#!/usr/bin/env python3
"""
monotonic_classifier.py

sklearn-compatible wrapper around mononet's PyTorch API
(mononet.torch.MonoLinear + mononet.core.types.MonotonicityMask),
confirmed via `help(mononet.torch)` against the installed package.

Simplifying assumption: every feature is treated as monotonically
INCREASING (+1). Any attribute that should conceptually push the
prediction down as it increases must be inverted upstream during
preprocessing (e.g. store `max_value - x` instead of `x`) — and that
inversion must be applied identically at both training and inference
time, or the monotonicity guarantee becomes meaningless. This is a
project-level convention, not something this file can enforce for you.

Public API:
    MonotonicNNClassifier — sklearn BaseEstimator/ClassifierMixin
"""

from __future__ import annotations

import sys
import numpy as np
import torch
import torch.nn as nn
from sklearn.base import BaseEstimator, ClassifierMixin
from sklearn.preprocessing import StandardScaler

from mononet.torch import MonoInput, MonoLinear
from mononet.core.types import MonotonicityMask


class _MonotonicNet(nn.Module):
    """All inputs constrained +1 (increasing). No free/unconstrained branch."""

    def __init__(self, n_features: int, n_classes: int, hidden: int = 64):
        super().__init__()
        mask = MonotonicityMask(np.ones(n_features, dtype=np.int8))
        self.net = nn.Sequential(
            MonoInput(mask),
            MonoLinear(n_features, hidden, activation="elu"),
            MonoLinear(hidden, hidden, activation="elu"),
            MonoLinear(hidden, n_classes),  # identity activation -> raw logits
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.net(x)


class MonotonicNNClassifier(BaseEstimator, ClassifierMixin):
    """
    Drop-in classifier for makeModel.py's registry. Matches the sklearn
    API (fit/predict/predict_proba/score) so train_and_evaluate(),
    cross_val_score(), and save_model() all work unmodified.

    Every feature is assumed monotonically increasing — see module
    docstring for the preprocessing convention this implies.

    Parameters
    ----------
    hidden : int
        Hidden layer width.
    epochs, lr, batch_size : training hyperparameters.
    device : "auto" | "cuda" | "cpu"
    """

    def __init__(self, hidden=64, epochs=200, lr=1e-3,
                 batch_size=64, device="auto", random_state=42):
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

    def fit(self, X: np.ndarray, y: np.ndarray):
        torch.manual_seed(self.random_state)

        X = np.asarray(X, dtype=np.float32)
        y = np.asarray(y, dtype=np.int64)

        self.classes_ = np.unique(y)
        n_features = X.shape[1]
        n_classes = len(self.classes_)

        # scaling matters a lot here — unscaled integer-coded features can
        # make the monotonic layers saturate badly.
        self._scaler = StandardScaler()
        X_scaled = self._scaler.fit_transform(X)

        self._device = self._resolve_device()
        self._model = _MonotonicNet(n_features, n_classes, self.hidden)
        self._model.to(self._device)

        X_t = torch.tensor(X_scaled, dtype=torch.float32, device=self._device)
        y_t = torch.tensor(np.searchsorted(self.classes_, y), dtype=torch.long, device=self._device)

        optimizer = torch.optim.Adam(self._model.parameters(), lr=self.lr)
        loss_fn = nn.CrossEntropyLoss()

        dataset = torch.utils.data.TensorDataset(X_t, y_t)
        loader = torch.utils.data.DataLoader(dataset, batch_size=self.batch_size, shuffle=True)

        self._model.train()
        for epoch in range(self.epochs):
            epoch_loss = 0.0
            for xb, yb in loader:
                optimizer.zero_grad()
                logits = self._model(xb)
                loss = loss_fn(logits, yb)
                loss.backward()
                optimizer.step()
                epoch_loss += loss.item() * xb.size(0)
            if (epoch + 1) % max(1, self.epochs // 10) == 0:
                print(f"[MonotonicNNClassifier] epoch {epoch+1}/{self.epochs}  "
                      f"loss={epoch_loss/len(dataset):.4f}", file=sys.stderr)

        self._model.eval()
        return self

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        X = np.asarray(X, dtype=np.float32)
        X_scaled = self._scaler.transform(X)
        X_t = torch.tensor(X_scaled, dtype=torch.float32, device=self._device)
        with torch.no_grad():
            logits = self._model(X_t)
            probs = torch.softmax(logits, dim=1).cpu().numpy()
        return probs

    def predict(self, X: np.ndarray) -> np.ndarray:
        probs = self.predict_proba(X)
        idx = np.argmax(probs, axis=1)
        return self.classes_[idx]

    def score(self, X: np.ndarray, y: np.ndarray) -> float:
        y = np.asarray(y)
        return float(np.mean(self.predict(X) == y))
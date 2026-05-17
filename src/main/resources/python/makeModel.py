#!/usr/bin/env python3
"""
makeModel.py

Handles model training, evaluation, and saving.
Called by beginPredictions.py when no saved model exists.

Expects a NormalizedDataset JSON file produced by the Java side.

Public API:
    makeModel(dataset_path, classifier_name, output_dir)
"""

import json
import os
import pickle
import sys

import numpy as np
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report

from classifiers import Classifier

# ---------------------------------------------------------------------------
# Classifier registry
# ---------------------------------------------------------------------------

def _build_sklearn_registry() -> dict:
    from sklearn.tree import DecisionTreeClassifier
    from sklearn.ensemble import (
        RandomForestClassifier,
        AdaBoostClassifier,
        HistGradientBoostingClassifier,
        ExtraTreesClassifier,
    )
    from sklearn.neural_network import MLPClassifier
    from sklearn.discriminant_analysis import LinearDiscriminantAnalysis

    return {
        Classifier.DECISION_TREE:                (DecisionTreeClassifier,         {}),
        Classifier.RANDOM_FOREST:                (RandomForestClassifier,         {}),
        Classifier.EXTRA_TREES:                  (ExtraTreesClassifier,           {}),
        Classifier.ADA_BOOST:                    (AdaBoostClassifier,             {}),
        Classifier.HIST_GRAD_BOOST:              (HistGradientBoostingClassifier, {}),
        Classifier.MLP:                          (MLPClassifier,                  {}),
        Classifier.LINEAR_DISCRIMINANT_ANALYSIS: (LinearDiscriminantAnalysis,     {}),
    }


def _build_custom_registry() -> dict:
    # Placeholder — add custom or third-party model classes here.
    return {}


def _build_full_registry() -> dict:
    registry = {}
    registry.update(_build_sklearn_registry())
    registry.update(_build_custom_registry())
    return registry


# ---------------------------------------------------------------------------
# Classifier construction
# ---------------------------------------------------------------------------

def build_classifier(name: str):
    registry = _build_full_registry()
    try:
        key = Classifier(name)
    except ValueError:
        known = ", ".join(c.value for c in Classifier)
        raise ValueError(f"Unknown classifier '{name}'.\nAvailable: {known}")
    cls, kwargs = registry[key]
    return cls(**kwargs)


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def load_dataset(dataset_path: str) -> tuple[np.ndarray, np.ndarray, str, list[str]]:
    """
    Read a NormalizedDataset JSON file produced by the Java side.
    Data arrives pre-encoded as integers — no transformation needed.

    Returns:
        X             : feature matrix (int)
        y             : label vector (int)
        dataset_name  : inputDatasetName field, used for model filename
        feature_names : attributeNames field
    """
    with open(dataset_path, "r") as f:
        data = json.load(f)

    dataset_name  = data["inputDatasetName"]
    feature_names = data["attributeNames"]
    datapoints    = np.array(data["allDatapoints"], dtype=int)

    X = datapoints[:, :-1]  # all columns except last
    y = datapoints[:, -1]   # last column is class index

    return X, y, dataset_name, feature_names


# ---------------------------------------------------------------------------
# Training and evaluation
# ---------------------------------------------------------------------------

def train_and_evaluate(clf, X: np.ndarray, y: np.ndarray) -> None:
    """Fit classifier and log test-set and cross-val evaluation to stderr."""
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    clf.fit(X_train, y_train)

    test_score = clf.score(X_test, y_test)
    y_pred     = clf.predict(X_test)
    report     = classification_report(y_test, y_pred, zero_division=1)
    cv_scores  = cross_val_score(clf, X, y, cv=10)

    print(f"[makeModel] Classifier : {clf.__class__.__name__}", file=sys.stderr)
    print(f"[makeModel] Test score : {test_score:.4f}",         file=sys.stderr)
    print(f"[makeModel] CV mean    : {cv_scores.mean():.4f}  std: {cv_scores.std():.4f}", file=sys.stderr)
    print(report, file=sys.stderr)


# ---------------------------------------------------------------------------
# Saving
# ---------------------------------------------------------------------------

def save_model(
        clf,
        feature_names:   list[str],
        classifier_name: str,
        dataset_name:    str,
        output_dir:      str,
) -> str:
    """
    Pickle the model bundle to output_dir.
    Returns the full path of the saved file.
    """
    filename = f"{classifier_name}_{dataset_name}.sav"
    filepath = os.path.join(output_dir, filename)

    bundle = {
        "model":         clf,
        "feature_names": feature_names,
    }

    with open(filepath, "wb") as f:
        pickle.dump(bundle, f)

    print(f"[makeModel] Model saved to {filepath}", file=sys.stderr)
    return filepath


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def makeModel(
        dataset_path:    str,
        classifier_name: str,
        output_dir:      str,
) -> str:
    """
    Full pipeline: load JSON → build classifier → train → evaluate → save.
    Returns the path to the saved model file.
    """
    print(f"[makeModel] Dataset    : {dataset_path}",    file=sys.stderr)
    print(f"[makeModel] Classifier : {classifier_name}", file=sys.stderr)
    print(f"[makeModel] Output dir : {output_dir}",      file=sys.stderr)

    X, y, dataset_name, feature_names = load_dataset(dataset_path)

    clf = build_classifier(classifier_name)

    train_and_evaluate(clf, X, y)

    return save_model(clf, feature_names, classifier_name, dataset_name, output_dir)
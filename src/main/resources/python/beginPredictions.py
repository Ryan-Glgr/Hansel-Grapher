#!/usr/bin/env python3
"""
beginPredictions.py

Entry point for the ML Oracle.

Usage:
    python beginPredictions.py --dataset <filename> --classifier <ClassifierName>

Example:
    python beginPredictions.py --dataset iris.json --classifier RandomForest

Layout assumption (this file lives in src/main/resources/python/):
    ../data/normalizeddatasets/ <- NormalizedDataset JSON files
    ../data/trainedmodels/      <- saved models (.sav)
"""

import argparse
import json
import os
import sys

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------

_SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DATASET_DIR  = os.path.normpath(os.path.join(_SCRIPT_DIR, "..", "data", "normalizeddatasets"))
OUTPUT_DIR   = os.path.normpath(os.path.join(_SCRIPT_DIR, "..", "data", "trainedmodels"))

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def resolve_dataset(dataset_arg: str) -> str:
    """
    Accept a bare filename ('iris.json') resolved against DATASET_DIR,
    or a full absolute path passed directly.
    """
    path = dataset_arg if os.path.isabs(dataset_arg) else os.path.join(DATASET_DIR, dataset_arg)
    if not os.path.isfile(path):
        print(f"[oracle] Dataset not found: {path}", file=sys.stderr)
        sys.exit(1)
    return path


def read_dataset_name(dataset_path: str) -> str:
    """Extract inputDatasetName from the JSON — the single source of truth for naming."""
    with open(dataset_path, "r") as f:
        return json.load(f)["inputDatasetName"]


def model_path(classifier_name: str, dataset_name: str) -> str:
    """<ClassifierName>_<datasetName>.sav inside OUTPUT_DIR."""
    return os.path.join(OUTPUT_DIR, f"{classifier_name}_{dataset_name}.sav")


# ---------------------------------------------------------------------------
# Model management
# ---------------------------------------------------------------------------

def model_exists(classifier_name: str, dataset_name: str) -> bool:
    return os.path.isfile(model_path(classifier_name, dataset_name))


def load_model(classifier_name: str, dataset_name: str) -> dict:
    import pickle
    path = model_path(classifier_name, dataset_name)
    print(f"[oracle] Loading model: {path}", file=sys.stderr)
    with open(path, "rb") as f:
        return pickle.load(f)


def create_model(classifier_name: str, dataset_path: str, dataset_name: str) -> dict:
    from makeModel import makeModel
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"[oracle] Training {classifier_name} on {dataset_path} ...", file=sys.stderr)
    makeModel(dataset_path, classifier_name, output_dir=OUTPUT_DIR)
    return load_model(classifier_name, dataset_name)


# ---------------------------------------------------------------------------
# Serve — stdin/stdout communication with Java host
# ---------------------------------------------------------------------------

STOP_SIGNAL = "exit"

def serve(model_bundle: dict) -> None:
    """
    Communicate with the Java host over stdin/stdout.
    Expects one JSON line per message:
        in:  {"values": [[1,2,3],[4,5,6],...]}
        out: {"predictions": [0, 1, ...]}
    Send 'exit' to shut down cleanly.

    ALL non-response output goes to stderr so stdout stays clean for Java.
    """
    import numpy as np

    clf = model_bundle["model"]

    sys.stdout.reconfigure(line_buffering=True)
    print("[oracle] Ready — waiting for input.", file=sys.stderr)

    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue

        if line.lower() == STOP_SIGNAL:
            print("[oracle] Shutdown signal received.", file=sys.stderr)
            break

        try:
            payload = json.loads(line)
            batch   = payload["values"]
        except (json.JSONDecodeError, KeyError) as e:
            print(f"[oracle] Parse error: {e}", file=sys.stderr)
            continue

        X           = np.array(batch, dtype=int)
        predictions = clf.predict(X).astype(int).tolist()

        response = json.dumps({"predictions": predictions})
        print(response)  # only JSON responses go to stdout
        print(f"[oracle] batch({len(batch)}) -> {predictions}", file=sys.stderr)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="ML Oracle — load or train a model, then serve predictions over stdin/stdout."
    )
    parser.add_argument("--dataset",    required=True, help="Dataset filename or full path.")
    parser.add_argument("--classifier", required=True, help="Classifier name (e.g. RandomForest, DecisionTree, MLP, ...).")
    return parser.parse_args()


def main() -> None:
    args      = parse_args()
    dataset_path = resolve_dataset(args.dataset)
    dataset_name = read_dataset_name(dataset_path)  # from JSON, not filename

    if model_exists(args.classifier, dataset_name):
        bundle = load_model(args.classifier, dataset_name)
    else:
        bundle = create_model(args.classifier, dataset_path, dataset_name)

    serve(bundle)


if __name__ == "__main__":
    main()
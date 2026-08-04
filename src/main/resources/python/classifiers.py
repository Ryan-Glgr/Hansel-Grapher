#!/usr/bin/env python3
"""
classifiers.py

Single source of truth for classifier names.
The enum string values are used as:
  - CLI arguments in beginPredictions.py
  - Registry keys in makeModel.py
  - Model filename prefixes (e.g. RandomForest_iris.sav)

Java side: use the enum constant name (e.g. RANDOM_FOREST)
with a pythonName field matching the string value here.
"""

from enum import Enum

class Classifier(str, Enum):
    DECISION_TREE               = "DecisionTree"
    RANDOM_FOREST               = "RandomForest"
    EXTRA_TREES                 = "ExtraTrees"
    ADA_BOOST                   = "AdaBoost"
    HIST_GRAD_BOOST             = "HistGradBoost"
    MLP                         = "MLP"
    LINEAR_DISCRIMINANT_ANALYSIS = "LinearDiscriminantAnalysis"
    MONOTONE_NEURAL_NETWORK     = "MonotoneNeuralNetwork"
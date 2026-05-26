package io.github.ryan_glgr.hansel_grapher.functionallogic.Interview;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MLModel {
    DECISION_TREE               ("DecisionTree"),
    RANDOM_FOREST               ("RandomForest"),
    EXTRA_TREES                 ("ExtraTrees"),
    ADA_BOOST                   ("AdaBoost"),
    HIST_GRAD_BOOST             ("HistGradBoost"),
    MLP                         ("MLP"),
    LINEAR_DISCRIMINANT_ANALYSIS("LinearDiscriminantAnalysis");

    public final String pythonName;
}
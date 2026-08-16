package io.github.ryan_glgr.hansel_grapher.helper;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;

// The balance ratio to determine how "good" a node is to be classified
public interface BalanceRatio {

    double computeBalanceRatio(Node node);

    // Balance ratio using the unity of the vector <aboveUmbrellaCases, underneathUmbrellaCases>
    // and the magnitude of the same vector
    BalanceRatio UNITY_BALANCE_RATIO = (node) -> {
        try {
            return node.umbrellaMagnitude *
                ((double) Math.min(node.aboveUmbrellaCases, node.underneathUmbrellaCases) /
                    Math.max(node.aboveUmbrellaCases, node.underneathUmbrellaCases));
        } catch(final ArithmeticException e) {
            return Double.NEGATIVE_INFINITY;
        }
    };

    // Balance ratio using the shannon entropy of the vector <aboveUmbrellaCases, underneathUmbrellaCases>
    // and the magnitude of the same vector
    BalanceRatio SHANNON_ENTROPY_BALANCE_RATIO = (node) -> {
        final double log2 = Math.log(2);
        try {
            double squaredMag = Math.pow(node.umbrellaMagnitude, 2);
            double distAbove = node.aboveUmbrellaCases / squaredMag;
            double distBelow = node.underneathUmbrellaCases / squaredMag;

            return node.umbrellaMagnitude * -1 * 
                (distAbove * Math.log(distAbove) / log2 + (distBelow) * Math.log(distBelow) / log2);
        } catch(final ArithmeticException e) {
            return Double.NEGATIVE_INFINITY;
        }
    };

    // Balance ratio which scales using the magnitude of the vector <aboveUmbrellaCases, underneathUmbrellaCases>
    // and the ratio between aboveCases and the totalCases, as well as the underneathCases and total cases
    BalanceRatio QUADRATIC_BALANCE_RATIO = (node) -> {
        try {
            return node.umbrellaMagnitude *
                node.aboveUmbrellaCases / (double)node.totalUmbrellaCases * 
                node.underneathUmbrellaCases / (double)node.totalUmbrellaCases;
        } catch(final ArithmeticException e) {
            return Double.NEGATIVE_INFINITY;
        }
    };
    
}

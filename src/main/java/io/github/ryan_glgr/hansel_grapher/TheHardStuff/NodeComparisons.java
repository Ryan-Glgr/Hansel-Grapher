package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.Comparator;

public class NodeComparisons {

    public static final Comparator<Node> HIGHEST_TOTAL_UMBRELLA = 
        (a, b) -> {
            return Integer.compare(a.totalUmbrellaCases, b.totalUmbrellaCases);
        };

    // Sort by BEST_BALANCE_RATIO_UMBRELLA_SORT: prefer nodes with higher balanceRatio
    public static final Comparator<Node> BEST_BALANCE_RATIO_UMBRELLA =
        Comparator.comparingDouble((Node n) -> n.balanceRatio);

    // Sort by SMALLEST_DIFFERENCE_UMBRELLA_SORT:
    // prefer nodes with smallest |above - below| difference, tie-breaker: larger totalUmbrellaCases
    public static final Comparator<Node> SMALLEST_DIFFERENCE_UMBRELLA =
        (x, y) -> {
            int diffX = Math.abs(x.aboveUmbrellaCases - x.underneathUmbrellaCases);
            int diffY = Math.abs(y.aboveUmbrellaCases - y.underneathUmbrellaCases);

            if (diffX == diffY) {
                return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases); // larger total preferred
            }
            return Integer.compare(diffX, diffY); // smaller difference first
        };

    // compares two nodes by their minimum number of confirmations. Then by the second lowest, third and so on. If a total tie, we go by the balanceRatio.
    public static final Comparator<Node> BY_MIN_CLASSIFICATIONS = (a, b) -> {
        for (int i = 0; i < a.possibleConfirmationsByClass.length; i++) {
            int cmp = Integer.compare(a.possibleConfirmationsByClass[i], b.possibleConfirmationsByClass[i]);
            if (cmp != 0) 
                return cmp;
        }
        return SMALLEST_DIFFERENCE_UMBRELLA.compare(a, b);
    };

    // Compare nodes lexicographically by their attribute arrays
    public static final Comparator<Node> LEXICOGRAPHIC_NODE_COMPARATOR = (a, b) -> {
        Integer[] A = a.values;
        Integer[] B = b.values;
        for (int i = 0; i < A.length; i++) {
            int cmp = Integer.compare(A[i], B[i]);
            
            if (cmp != 0) 
                return cmp;
        }
        return 0;
    };

    // Compare nodes by the ratio between the magnitude of their umbrella cases and the uniformity of their umbrella cases
    public static final Comparator<Node> UNIFORMITY_RATIO_COMPARATOR =
        (a, b) -> {
            double aMag = Math.sqrt(Math.pow(a.aboveUmbrellaCases, 2) + Math.pow(a.underneathUmbrellaCases, 2));
            double bMag = Math.sqrt(Math.pow(b.aboveUmbrellaCases, 2) + Math.pow(b.underneathUmbrellaCases, 2));
            double aUni = Math.min(a.aboveUmbrellaCases, a.underneathUmbrellaCases)
                / (double) Math.max(a.aboveUmbrellaCases, a.underneathUmbrellaCases);
            double bUni = Math.min(b.aboveUmbrellaCases, b.underneathUmbrellaCases)
                / (double) Math.max(b.aboveUmbrellaCases, b.underneathUmbrellaCases);
            return Double.compare(aMag * aUni, bMag * bUni);
        };

}


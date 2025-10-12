package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.Arrays;
import java.util.Comparator;

public class NodeComparisons {


    // compares two nodes by their minimum number of confirmations. Then by the second lowest, third and so on. If a total tie, we go by the balanceRatio.
    // returns negative when THIS node should be after.
    public static final Comparator<Node> BY_MIN_CLASSIFICATIONS =
    (a, b) -> {
        int[] aMins = Arrays.copyOf(a.possibleConfirmationsByClass, a.possibleConfirmationsByClass.length);
        int[] bMins = Arrays.copyOf(b.possibleConfirmationsByClass, b.possibleConfirmationsByClass.length);

        Arrays.sort(aMins); // ascending
        Arrays.sort(bMins); // ascending

        // Compare lexicographically, preferring LARGER minima first
        for (int i = 0; i < aMins.length; i++) {
            if (aMins[i] != bMins[i]) {
                return Integer.compare(aMins[i], bMins[i]); // positive means a > b
            }
        }

        // Tie-breaker: smaller balanceRatio preferred
        return Float.compare(b.balanceRatio, a.balanceRatio);
    };

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

}


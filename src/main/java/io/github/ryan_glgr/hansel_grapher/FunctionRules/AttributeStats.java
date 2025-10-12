package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import java.util.HashMap;

class AttributeStats {
    final int index;
    final HashMap<Integer, Integer> counts; // value -> occurrence
    final int branches; // distinct values
    final int maxGroupSize; // largest occurrence for a single value

    AttributeStats(int index, HashMap<Integer,Integer> counts) {
        this.index = index;
        this.counts = counts;
        this.branches = counts.size();
        int max = 0;
        for (int c : counts.values()) if (c > max) max = c;
        this.maxGroupSize = max;
    }
}
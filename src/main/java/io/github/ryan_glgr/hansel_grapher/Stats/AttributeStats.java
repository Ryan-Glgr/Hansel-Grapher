package io.github.ryan_glgr.hansel_grapher.Stats;

import java.util.HashMap;

public class AttributeStats {
    public final int index;
    public final HashMap<Integer, Integer> counts; // value -> occurrence
    public final int branches; // distinct values
    public final int maxGroupSize; // largest occurrence for a single value

    public AttributeStats(int index, HashMap<Integer,Integer> counts) {
        this.index = index;
        this.counts = counts;
        this.branches = counts.size();
        int max = 0;
        for (int c : counts.values()) if (c > max) max = c;
        this.maxGroupSize = max;
    }
}
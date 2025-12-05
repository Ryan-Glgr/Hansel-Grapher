package io.github.ryan_glgr.hansel_grapher.Stats;

import java.util.HashMap;

public class AttributeStats {
    public final int attributeIndex;
    public final HashMap<Integer, Integer> countsOfEachKValueForThisAttribute; // value -> occurrence
    public final int numberOfDistinctKValues; // distinct values
    public final int maxGroupSize; // largest occurrence for a single value

    public AttributeStats(int attributeIndex, HashMap<Integer,Integer> countsOfEachKValueForThisAttribute) {
        this.attributeIndex = attributeIndex;
        this.countsOfEachKValueForThisAttribute = countsOfEachKValueForThisAttribute;
        this.numberOfDistinctKValues = countsOfEachKValueForThisAttribute.size();
        int max = 0;
        for (int c : countsOfEachKValueForThisAttribute.values()) if (c > max) max = c;
        this.maxGroupSize = max;
    }
}
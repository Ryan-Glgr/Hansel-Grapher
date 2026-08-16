package io.github.ryan_glgr.hansel_grapher.functionrules;

import java.util.HashMap;

public class AttributeStats {
    public final int attributeIndex;
    public final HashMap<Integer, Integer> countsOfEachKValueForThisAttribute; // value -> occurrence
    public final int numberOfDistinctKValues; // distinct values
    public final int maxGroupSize; // largest occurrence for a single value

    public AttributeStats(final int attributeIndex, final HashMap<Integer,Integer> countsOfEachKValueForThisAttribute) {
        this.attributeIndex = attributeIndex;
        this.countsOfEachKValueForThisAttribute = countsOfEachKValueForThisAttribute;
        this.numberOfDistinctKValues = countsOfEachKValueForThisAttribute.size();
        this.maxGroupSize = countsOfEachKValueForThisAttribute.values().stream().max(Integer::compareTo).get();
    }
}
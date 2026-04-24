package io.github.ryan_glgr.hansel_grapher.functionrules;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.Interview;

public class Attribute {

    public static Float NO_WEIGHT = -1.0f;

    public Integer highestValuePossibleForThisAttribute;
    public Integer index;
    public Float weight;
    public Interview subFunction;
    public String name;

    public Attribute (final Integer highestValuePossibleForThisAttribute,
                      final Integer index,
                      final Float weight,
                      final Interview subFunction,
                      final String attributeName) {
        this.highestValuePossibleForThisAttribute = highestValuePossibleForThisAttribute;
        this.index = index;
        this.weight = weight;
        this.subFunction = subFunction;
        this.name = attributeName;
    }

}

package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.Interview;

public class Attribute {

    public static Float NO_WEIGHT = -1.0f;

    public Integer highestValuePossibleForThisAttribute;
    public Integer index;
    public Float weight;
    public Interview subFunction;
    public String name;

    public Attribute (Integer highestValuePossibleForThisAttribute,
                      Integer index,
                      Float weight,
                      Interview subFunction,
                      String attributeName) {
        this.highestValuePossibleForThisAttribute = highestValuePossibleForThisAttribute;
        this.index = index;
        this.weight = weight;
        this.subFunction = subFunction;
        this.name = attributeName;
    }

}

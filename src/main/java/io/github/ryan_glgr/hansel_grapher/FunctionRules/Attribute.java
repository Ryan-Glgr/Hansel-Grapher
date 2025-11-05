package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;

public class Attribute {

    public Integer highestValuePossibleForThisAttribute;
    public Integer index;

    public boolean hasSubFunction;
    public Interview subFunction;
    public String name;

    public Attribute (Integer highestValuePossibleForThisAttribute, Integer index, boolean hasSubFunction, Interview subFunction, String attributeName) {
        this.highestValuePossibleForThisAttribute = highestValuePossibleForThisAttribute;
        this.index = index;
        this.hasSubFunction = hasSubFunction;
        this.subFunction = subFunction;
        this.name = attributeName;
    }

}

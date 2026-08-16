package io.github.ryan_glgr.hansel_grapher.functionrules;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Attribute {
    public Integer highestValuePossibleForThisAttribute;
    public Integer index;
    public Float weight; // needs to be populated for sample linear function interviews. else it can be just null, doesn't really matter
    public Interview subFunction;
    public String name;
}

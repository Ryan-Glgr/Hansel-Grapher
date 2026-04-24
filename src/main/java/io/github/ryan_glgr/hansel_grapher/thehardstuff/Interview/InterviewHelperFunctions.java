package io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview;

import io.github.ryan_glgr.hansel_grapher.helper.Util;

public class InterviewHelperFunctions {
    public static Interview createSubFunction(final Integer[] kValues,
                                              final Float[] weights,
                                              final int numClasses,
                                              final InterviewMode mode,
                                              final MagicFunctionMode magicMode) {
        return new Interview(
                kValues,
                weights,
                mode,
                numClasses,
                Util.createDefaultAttributeNames(kValues.length),
                Util.createDefaultClassificationNames(numClasses),
                null,
                null,
                null,
                magicMode
        );
    }

}

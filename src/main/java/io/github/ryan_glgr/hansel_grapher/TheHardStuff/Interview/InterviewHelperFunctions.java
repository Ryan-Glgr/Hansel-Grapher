package io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;

import java.util.stream.IntStream;

public class InterviewHelperFunctions {
    public static Interview createSubFunction(Integer[] kValues,
                                              Float[] weights,
                                              int numClasses,
                                              InterviewMode mode,
                                              MagicFunctionMode magicMode) {
        return new Interview(
                kValues,
                weights,
                mode,
                numClasses,
                createDefaultAttributeNames(kValues.length),
                createDefaultClassificationNames(numClasses),
                null,
                new Interview[kValues.length],
                magicMode
        );
    }

    public static String[] createDefaultAttributeNames(int numAttributes) {
        return IntStream.range(0, numAttributes)
                .mapToObj(i -> "Attribute " + i)
                .toArray(String[]::new);
    }

    public static String[] createDefaultClassificationNames(int numClasses) {
        return IntStream.range(0, numClasses)
                .mapToObj(i -> "Classification " + i)
                .toArray(String[]::new);
    }
}

package io.github.ryan_glgr.hansel_grapher.helper;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Util {
    public static String[] createDefaultAttributeNames(final int numAttributes) {
        return IntStream.range(0, numAttributes)
                .mapToObj(i -> "Attribute " + i)
                .toArray(String[]::new);
    }

    public static String[] createDefaultClassificationNames(final int numClasses) {
        return IntStream.range(0, numClasses)
                .mapToObj(i -> "Classification " + i)
                .toArray(String[]::new);
    }

    public static String printListOfNodes(final List<Node> nodes) {
        final List<String> valuesStrings = nodes.stream()
                .map(node -> "\n" + Arrays.toString(node.values))
                .toList();
        return valuesStrings.toString();
    }

    // Helper method to create a properly initialized counter array for use with incrementCounter
    // Returns array filled with 0s except first element is -1, so first increment gives [0,0,0,...]
    public static Integer[] counterInitializer(final Integer[] kValues) {
        final Integer[] counter = new Integer[kValues.length];
        Arrays.fill(counter, 0);
        counter[0] = -1;
        return counter;
    }
}

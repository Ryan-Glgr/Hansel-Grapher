package io.github.ryan_glgr.hansel_grapher.helper;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;

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
}

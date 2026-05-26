package io.github.ryan_glgr.hansel_grapher.functionallogic.Interview;

import io.github.ryan_glgr.hansel_grapher.helper.Util;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;

import java.util.*;
import java.util.stream.Collectors;

public class InterviewHelperFunctions {
    public static Interview createSubFunction(final Integer[] kValues,
                                              final Float[] weights,
                                              final int numClasses,
                                              final InterviewMode mode,
                                              final MagicFunctionMode magicMode) {
        final Interview interview = new Interview(
                kValues,
                weights,
                numClasses,
                Util.createDefaultAttributeNames(kValues.length),
                Util.createDefaultClassificationNames(numClasses),
                null,
                null,
                null,
                magicMode,
                null,
                null
        );
        interview.beginInterview(mode);
        return interview;
    }

    public static Map<Integer, Set<Node>> getKnownLowUnitsOfEachClassification(final Map<Integer, Set<Integer[]>> setOfLowUnitsByClassification, final HashMap<Integer, Node> data) {
        if (Objects.isNull(setOfLowUnitsByClassification))
            return null;
        return setOfLowUnitsByClassification.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(lowUnit -> data.get(Node.hash(lowUnit)))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ));
    }

    // splits a list of nodes (part or whole hansel chain) on nodes which are confirmed
    public static ArrayList<ArrayList<Node>> splitChunkIntoPiecesHelper(final ArrayList<Node> chunk) {
        final ArrayList<ArrayList<Node>> newChunks = new ArrayList<>();
        final ArrayList<Node> currentChunk = new ArrayList<>();

        for (final Node node : chunk) {
            if (node.classificationConfirmed) {
                // End current chunk if we have nodes collected
                if (!currentChunk.isEmpty()) {
                    newChunks.add(new ArrayList<>(currentChunk));
                    currentChunk.clear();
                }
            } else {
                currentChunk.add(node);
            }
        }

        // Add last chunk if there are remaining nodes
        if (!currentChunk.isEmpty()) {
            newChunks.add(currentChunk);
        }
        return newChunks;
    }

}

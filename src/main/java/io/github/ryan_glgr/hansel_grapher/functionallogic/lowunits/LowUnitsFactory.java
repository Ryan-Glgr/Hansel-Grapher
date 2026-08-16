package io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.helper.NodeComparisons;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LowUnitsFactory {

    private static Map<Integer, Set<LowUnit>> findRegularLowUnits(@NonNull final ArrayList<ArrayList<Node>> hanselChainSet) {

        final Map<Integer, Set<LowUnit>> lowUnitMap = new HashMap<>();

        for(final ArrayList<Node> chain : hanselChainSet){
            final Map<Integer, LowUnit> lowestNodeOfEachClassInThisChain = new HashMap<>();

            for (final Node node : chain) {
                // taking only the first occurence of the classification in each chain. this is the definition of a low unit.
                lowestNodeOfEachClassInThisChain.putIfAbsent(node.classification, new LowUnit(node, LowUnit.Type.INCLUSIVE, node.classification));
            }

            // add to our collection of low units for each classification
            lowestNodeOfEachClassInThisChain.forEach((classification, lowUnit) -> {
                        lowUnitMap.putIfAbsent(classification, new HashSet<>());
                        lowUnitMap.get(classification).add(lowUnit);
                    });
        }
        return lowUnitMap;
    }

    private static Map<Integer, Set<LowUnit>> removeUselessLowUnits(final Map<Integer, Set<LowUnit>> regularLowUnits,
                                                                    final Map<Integer, Set<LowUnit>> exclusiveLowUnits) {

        final Set<Integer> classifications = regularLowUnits.keySet();

        final Map<Integer, Set<LowUnit>> newLowUnits = new HashMap<>();
        for (final Integer classification : classifications) {
            newLowUnits.put(classification, new HashSet<>());
        }

        for (final Map.Entry<Integer, Set<LowUnit>> regularLowUnitsOfThisClass : regularLowUnits.entrySet()) {
            final Integer classification = regularLowUnitsOfThisClass.getKey();
            final Set<LowUnit> lowUnits = regularLowUnitsOfThisClass.getValue();

            // add all the low units for this class
            newLowUnits.get(classification).addAll(lowUnits);

            final HashSet<LowUnit> redundantLowUnits = new HashSet<>();
            // for each node, if it is totally dominated by any of the other nodes, we know that we can remove that other node which is dominating us.
            for (final LowUnit lowUnit : lowUnits) {
                if (redundantLowUnits.contains(lowUnit))
                    continue;

                for (final LowUnit otherLowUnit : lowUnits) {
                    if (lowUnit == otherLowUnit || redundantLowUnits.contains(otherLowUnit)) {
                        continue;
                    }
                    // NOTE: this check ALSO relies on the fact that they are the same classification. since they are mapped by classification, that check is skipped.
                    if (lowUnit.classifies(otherLowUnit.getDatapoint())) {
                        redundantLowUnits.add(otherLowUnit);

                    }
                }
            }
            newLowUnits.get(classification).removeIf(redundantLowUnits::contains);
        }

        final Map<Integer, Set<LowUnit>> exclusiveLowUnitsToUse = new HashMap<>();
        for (final Integer classification : classifications) {
            exclusiveLowUnitsToUse.put(classification, new HashSet<>());
        }

        for (final Map.Entry<Integer, Set<LowUnit>> exclusiveLowUnitsOfThisClass : exclusiveLowUnits.entrySet()) {
            final Integer classification = exclusiveLowUnitsOfThisClass.getKey();
            final Set<LowUnit> lowUnitsOfThisClass = newLowUnits.get(classification);

            // sorting because we need deterministic behavior, and streams are not necessarily deterministic.
            final List<LowUnit> sortedExclusiveUnits = exclusiveLowUnitsOfThisClass.getValue()
                    .stream()
                    .sorted((a, b) ->
                            NodeComparisons.LEXICOGRAPHIC_NODE_COMPARATOR.compare(a.getDatapoint(), b.getDatapoint()))
                    .toList();

            for (final LowUnit exclusiveLowUnit : sortedExclusiveUnits) {
                final boolean removedSomething = lowUnitsOfThisClass.removeIf(regularLowUnit ->
                        exclusiveLowUnit.classifies(regularLowUnit.getDatapoint()));
                if (removedSomething) {
                    exclusiveLowUnitsToUse.get(classification).add(exclusiveLowUnit);
                }
            }
        }

        for (final Integer classification : classifications) {
            newLowUnits.get(classification).addAll(exclusiveLowUnitsToUse.get(classification));
        }
        return newLowUnits;
    }

    // a node is considered an "exclusive low unit" in this use case: it has a classification N, and all it's upstairs neighbors (nodes with + 1 in some attribute compared to itself)
    // have classification N + 1. We can assign this as a lowUnit of type EXCLUSIVE, and classification N + 1. Even though the node itself is not class N + 1.
    // this saves us in case we needed multiple low units to represent "anything greater than this node",
    // rather than exhaustively having each possible higher value, we just store this unit, and say anything larger is N + 1
    private static Map<Integer, Set<LowUnit>> findExclusiveLowUnits(@NonNull final Set<Node> dataset) {
        final HashMap<Integer, Set<LowUnit>> exclusiveLowUnits = new HashMap<>();
        for (final Node node : dataset) {

            // we will not allow an exclusive low unit to be of impossible classification itself. this would be fine, but it is
            // not possible anyways, because we have defined impossible units as anything >= to something anyways. So it can never
            // happen where this node is impossible but something above is possible. if we change that to be flexible (allowing impossibility
            // to go both up and down), then this is not necessary and we should try harder here.
            if (Node.IMPOSSIBLE_CLASSIFICATION.equals(node.classification))
                continue;

            boolean isExclusiveLowUnit = true;
            final Map<Integer, Integer> numberOfNeighborsOfEachClass = new HashMap<>();
            final Node[] directUpExpansions = node.upExpansions;
            for (final Node upExpandedNeighbor : directUpExpansions) {

                if (upExpandedNeighbor == null)
                    continue;

                final Integer neighborClass = upExpandedNeighbor.classification;

                // if the upstairs neighbor is this same class, we do not have the optimization of saying "anything
                // HIGHER than this node is class X", since upstairs neighbor is higher, yet the same class.
                if (neighborClass.equals(node.classification)) {
                    isExclusiveLowUnit = false;
                    break;
                }

                if (neighborClass < node.classification && !Node.IMPOSSIBLE_CLASSIFICATION.equals(neighborClass)) {
                    throw new IllegalStateException(String.format("Monotonicity has been violated by node: " +
                            "[%s] being lower class than node: [%s]", upExpandedNeighbor, node));
                }

                // upstairs neighbor is higher class. we may be able to use 'node' to say "anything > 'node' (exclusively) is
                // neighborClass. Importantly, neighbors can have multiple classes in a multiclass problem. so we will take the lowest class in the map.
                // we can also only use this optimization if all neighbors are at least 1 class higher than the current node.
                numberOfNeighborsOfEachClass.put(neighborClass, numberOfNeighborsOfEachClass.getOrDefault(neighborClass, 0) + 1);
            }
            // if the map has some entries, and we haven't explicitly marked it false (meaning that an upstairs neighbor has same class),
            // then we can say that all neighbors are some higher class. if the count is > 1 for the lowest class of the neighbors, then
            // it is worthwhile to make this an exclusive low unit.
            isExclusiveLowUnit &= !numberOfNeighborsOfEachClass.isEmpty();

            if (isExclusiveLowUnit) {
                final Map.Entry<Integer, Integer> lowestClassEntry = Collections.min(numberOfNeighborsOfEachClass.entrySet(),
                        Map.Entry.comparingByKey());

                // we only bother adding to the map if it would be able to classify more than 1 node. otherwise it is just additional complexity compared to an inclusive low unit.
                if (lowestClassEntry.getValue() != 0) {
                    exclusiveLowUnits.computeIfAbsent(lowestClassEntry.getKey(), k -> new HashSet<>())
                        .add(new LowUnit(node, LowUnit.Type.EXCLUSIVE, lowestClassEntry.getKey()));
                }
            }
        }
        return exclusiveLowUnits;
    }

    public static Map<Integer, Set<LowUnit>> findPrunedLowUnits(@NonNull final ArrayList<ArrayList<Node>> hanselChainSet) {
        final Map<Integer, Set<LowUnit>> inclusiveLowUnits = findRegularLowUnits(hanselChainSet);
        final Map<Integer, Set<LowUnit>> exclusiveLowUnits = findExclusiveLowUnits(
                hanselChainSet.stream().flatMap(ArrayList::stream).collect(Collectors.toSet()));
        final Map<Integer, Set<LowUnit>> prunedUnits = removeUselessLowUnits(inclusiveLowUnits, exclusiveLowUnits);

        int numInclusive = 0;
        int numExclusive = 0;
        for (final Set<LowUnit> lowUnits : prunedUnits.values()) {
            for (final LowUnit lowUnit : lowUnits) {
                if (LowUnit.Type.INCLUSIVE.equals(lowUnit.getLowUnitType()))
                    numInclusive++;
                else
                    numExclusive++;
            }
        }
        System.out.printf("Found: [%s] total low units. [%s] were inclusive, [%s] were exclusive.",
                numExclusive + numInclusive, numInclusive, numExclusive);
        return prunedUnits;
    }
}


package io.github.ryan_glgr.hansel_grapher.functionrules;

import java.util.*;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import lombok.NonNull;

public class RuleNode {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final Integer attributeIndex;
    public final Integer attributeValue;
    public RuleNode[] inclusiveRuleset;
    public RuleNode[] exclusiveRuleset;

    private transient final Set<Integer> attributesAlreadyUsed;
    private transient RuleNode parent; // transient => Gson skips it, avoids parent/child cycle

    private final static Comparator<AttributeStats> greedyLeastBranchesComparison = Comparator
            .comparingInt((AttributeStats a) -> a.numberOfDistinctKValues)
            .thenComparingInt(a -> -a.maxGroupSize)
            .thenComparingInt(a -> a.attributeIndex);

    static RuleNode createRuleNodes(@NonNull final Set<LowUnit> lowUnits, final int numAttributes) {
        if (lowUnits.isEmpty())
            return null;

        final ArrayList<Node> inclusiveLowUnitNodes = new ArrayList<>(lowUnits.stream()
                .filter(lowUnit -> LowUnit.Type.INCLUSIVE.equals(lowUnit.getLowUnitType()))
                .map(LowUnit::getDatapoint)
                .toList());

        final ArrayList<Node> exclusiveLowUnitNodes = new ArrayList<>(lowUnits.stream()
                .filter(lowUnit -> LowUnit.Type.EXCLUSIVE.equals(lowUnit.getLowUnitType()))
                .map(LowUnit::getDatapoint)
                .toList());

        final RuleNode root = new RuleNode(null, null, inclusiveLowUnitNodes, exclusiveLowUnitNodes, new HashSet<>(), numAttributes, 0);
        separateKidsFromParents(root, true);
        separateKidsFromParents(root, false);
        return root;
    }

    private RuleNode(final Integer attributeIndex,
                     final Integer attributeValue,
                     final ArrayList<Node> inclusiveLowUnitNodes,
                     final ArrayList<Node> exclusiveLowUnitsNodes,
                     final Set<Integer> attributesAlreadyUsed,
                     final int numAttributes,
                     final int depth) {
        this.attributeIndex = attributeIndex;
        this.attributeValue = attributeValue;
        this.attributesAlreadyUsed = new HashSet<>(attributesAlreadyUsed);

        this.inclusiveRuleset = findChildrenGreedyTechnique(inclusiveLowUnitNodes, numAttributes, depth, true);
        if (inclusiveRuleset != null) {
            for (final RuleNode kid : inclusiveRuleset) kid.parent = this;
        }
        this.exclusiveRuleset = findChildrenGreedyTechnique(exclusiveLowUnitsNodes, numAttributes, depth, false);
        if (exclusiveRuleset != null) {
            for (final RuleNode kid : exclusiveRuleset) kid.parent = this;
        }
    }

    private static RuleNode[] separateKidsFromParents(final RuleNode node, final boolean isInclusive) {
        if (node == null)
            return null;

        final RuleNode[] ruleset = isInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;

        if (ruleset == null) {
            if (node.parent != null && node.attributeValue == 0) {
                return null;
            } else {
                return new RuleNode[]{node};
            }
        }

        final List<RuleNode> newChildrenList = new ArrayList<>();
        for (final RuleNode child : ruleset) {
            final RuleNode[] replacement = separateKidsFromParents(child, isInclusive);
            if (replacement == null)
                continue;

            for (final RuleNode r : replacement) {
                r.parent = node;
                newChildrenList.add(r);
            }
        }

        if (node.parent != null && node.attributeValue == 0) {
            return newChildrenList.toArray(new RuleNode[0]);
        } else {
            final RuleNode[] newRuleset = newChildrenList.isEmpty()
                    ? null
                    : newChildrenList.toArray(new RuleNode[0]);
            if (isInclusive) node.inclusiveRuleset = newRuleset;
            else node.exclusiveRuleset = newRuleset;
            return new RuleNode[]{node};
        }
    }

    private RuleNode[] findChildrenGreedyTechnique(final ArrayList<Node> childrenNodes, final int dimension, final int depth, final boolean isInclusive) {
        if (childrenNodes == null || childrenNodes.isEmpty()) {
            return null;
        }

        final List<AttributeStats> stats = getAttributeStatsForUnusedAttributes(childrenNodes, dimension);

        if (stats.isEmpty()) {
            return null;
        }

        final AttributeStats best = stats.stream().min(greedyLeastBranchesComparison).orElse(null);

        return createChildNodesFromAttributeStats(childrenNodes, dimension, best, depth + 1, isInclusive);
    }

    private List<AttributeStats> getAttributeStatsForUnusedAttributes(final ArrayList<Node> childrenNodes, final int dimension) {
        return IntStream.range(0, dimension)
                .filter(i -> !attributesAlreadyUsed.contains(i))
                .mapToObj(i -> {
                    final HashMap<Integer, Integer> counts = new HashMap<>();
                    for (final Node n : childrenNodes) {
                        final int val = n.values[i];
                        counts.put(val, counts.getOrDefault(val, 0) + 1);
                    }
                    return new AttributeStats(i, counts);
                })
                .toList();
    }

    private RuleNode[] createChildNodesFromAttributeStats(final ArrayList<Node> childrenNodes,
                                                          final int dimension,
                                                          final AttributeStats attributeToSplitOn,
                                                          final int depth,
                                                          final boolean isInclusive) {
        final ArrayList<RuleNode> newChildren = new ArrayList<>();
        final List<Integer> distinctValuesForThisAttribute = new ArrayList<>(attributeToSplitOn.countsOfEachKValueForThisAttribute.keySet());
        Collections.sort(distinctValuesForThisAttribute);

        for (final int valueToFactorOut : distinctValuesForThisAttribute) {
            final ArrayList<Node> subsetofChildrenNodesForThisTree = new ArrayList<>();

            for (final Node n : childrenNodes) {
                if (n.values[attributeToSplitOn.attributeIndex] == valueToFactorOut)
                    subsetofChildrenNodesForThisTree.add(n);
            }

            final Set<Integer> childUsed = new HashSet<>(this.attributesAlreadyUsed);
            childUsed.add(attributeToSplitOn.attributeIndex);
            if (isInclusive) {
                newChildren.add(new RuleNode(attributeToSplitOn.attributeIndex, valueToFactorOut, subsetofChildrenNodesForThisTree, null, childUsed, dimension, depth));
            } else {
                newChildren.add(new RuleNode(attributeToSplitOn.attributeIndex, valueToFactorOut, null, subsetofChildrenNodesForThisTree, childUsed, dimension, depth));
            }
        }
        return newChildren.toArray(new RuleNode[0]);
    }

    /** Serialize this (sub)tree to pretty-printed JSON. */
    public String toJson() {
        return GSON.toJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public static int getNumberOfClauses(final RuleNode node, final boolean useInclusive) {
        if (node == null)
            return 0;

        final int count = node.attributeIndex == null ? 0 : 1;

        final RuleNode[] nodeChildren = useInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;
        if (nodeChildren != null) {
            return count + Arrays.stream(nodeChildren)
                    .mapToInt(ruleNode -> getNumberOfClauses(ruleNode, useInclusive)).sum();
        }
        return count;
    }
}
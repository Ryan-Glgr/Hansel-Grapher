package io.github.ryan_glgr.hansel_grapher.functionrules;

import java.util.*;
import java.util.stream.IntStream;

import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.stats.AttributeStats;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;

public class RuleNode {
    public final Integer attributeIndex;
    public final Integer attributeValue;
    public RuleNode[] inclusiveRuleset; // grouped children (one per attribute value)
    public RuleNode[] exclusiveRuleset; // grouped children (one per attribute value), under the "EXCLUSIVE" evaluation rules.
    public final Set<Integer> attributesAlreadyUsed; // immutable for a node (copy-per-node)
    private RuleNode parent;
    
    /**
     * Create the full decision-rule tree for the supplied nodes.
     * Returns the root RuleNode (attributeIndex and attributeValue will be null at root).
     * If nodes is null or empty, returns null.
     */
    public static RuleNode createRuleNodes(final ArrayList<LowUnit> nodes, final int numAttributes) {
        if (nodes == null || nodes.isEmpty()) 
            return null;

        final ArrayList<Node> inclusiveLowUnitNodes = new ArrayList<>(nodes.stream()
                .filter(lowUnit -> LowUnit.Type.INCLUSIVE.equals(lowUnit.getLowUnitType()))
                .map(LowUnit::getDatapoint)
                .toList());

        final ArrayList<Node> exclusiveLowUnitNodes  = new ArrayList<>(nodes.stream()
                .filter(lowUnit -> LowUnit.Type.EXCLUSIVE.equals(lowUnit.getLowUnitType()))
                .map(LowUnit::getDatapoint)
                .toList());

        // attributeIndex and attributeValue are null for the root (no attribute was used to get here)
        final RuleNode root = new RuleNode(null, null, inclusiveLowUnitNodes, exclusiveLowUnitNodes, new HashSet<>(), numAttributes, 0);
        removeDeadbeatParents(root);
        return root;
    }

    // Constructor requires the set of attributes already used along the path.
    private RuleNode(final Integer attributeIndex,
                     final Integer attributeValue,
                     final ArrayList<Node> inclusiveLowUnitNodes,
                     final ArrayList<Node> exclusiveLowUnitsNodes,
                     final Set<Integer> attributesAlreadyUsed,
                     final int numAttributes,
                     final int depth) {
        this.attributeIndex = attributeIndex;
        this.attributeValue = attributeValue;
        // store an unmodifiable copy for this node (or just a private copy)
        this.attributesAlreadyUsed = new HashSet<>(attributesAlreadyUsed);
        
        // Note: don't add attributeIndex here — attributeIndex is the attribute used to
        // get to this node. If you want this node's attribute to be considered "used"
        // for its children, add it when creating children below.

        // getting the nodes which make up the low unit sets
        this.inclusiveRuleset = findChildrenGreedyTechnique(inclusiveLowUnitNodes, numAttributes, depth, true);
        // take control of my children
        if (inclusiveRuleset != null)
            for(final RuleNode kid : inclusiveRuleset){
                kid.parent = this;
            }
        this.exclusiveRuleset = findChildrenGreedyTechnique(exclusiveLowUnitsNodes, numAttributes, depth, false);
        if (exclusiveRuleset != null)
            for(final RuleNode kid : exclusiveRuleset){
                kid.parent = this;
            }
    }

    private static void removeDeadbeatParents(final RuleNode root) {
        separateKidsFromParents(root, true);
        separateKidsFromParents(root, false);
    }

    private static RuleNode[] separateKidsFromParents(final RuleNode node, final boolean isInclusive) {
        if (node == null)
            return null;

        final RuleNode[] ruleset = isInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;

        // Leaf node: just keep or drop based on value
        if (ruleset == null) {
            if (node.parent != null && node.attributeValue == 0) {
                return null;
            } else {
                return new RuleNode[]{node};
            }
        }

        // Process children first (bottom-up)
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
            else             node.exclusiveRuleset = newRuleset;
            return new RuleNode[]{node};
        }
    }
    private final static Comparator<AttributeStats> greedyLeastBranchesComparison = Comparator
        .comparingInt((AttributeStats a) -> a.numberOfDistinctKValues)
        .thenComparingInt(a -> -a.maxGroupSize)
        .thenComparingInt(a -> a.attributeIndex);

    private RuleNode[] findChildrenGreedyTechnique(final ArrayList<Node> childrenNodes, final int dimension, final int depth, final boolean isInclusive) {
        if (childrenNodes == null || childrenNodes.isEmpty()) {
            return null; // leaf node
        }

        final List<AttributeStats> stats = getAttributeStatsForUnusedAttributes(childrenNodes, dimension);

        if (stats.isEmpty()) {
            return null;
        }

        final AttributeStats best = stats.stream().min(greedyLeastBranchesComparison).orElse(null);

        return createChildNodesFromAttributeStats(childrenNodes, dimension, best, depth + 1, isInclusive);
    }

    private List<AttributeStats> getAttributeStatsForUnusedAttributes(final ArrayList<Node> childrenNodes, final int dimension) {
        // Build stats for each unused attribute. Use sequential if Node.dimension small.
        return IntStream.range(0, dimension)
            .filter(i -> !attributesAlreadyUsed.contains(i))
            .mapToObj(i -> {
                final HashMap<Integer, Integer> counts = new HashMap<>();
                for (final Node n : childrenNodes) {
                    // if Node.values is Integer[], avoid NPE and use .intValue()
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
        // attribute to split on is the particular x attribute which we are factoring out of the rule tree next. the counts is a map of how many occurrences of each particular k value we see in the childNodes of this RuleNode.
        final ArrayList<RuleNode> newChildren = new ArrayList<>();
        final List<Integer> distinctValuesForThisAttribute = new ArrayList<>(attributeToSplitOn.countsOfEachKValueForThisAttribute.keySet());
        Collections.sort(distinctValuesForThisAttribute);

        for (final int valueToFactorOut : distinctValuesForThisAttribute) {
            final ArrayList<Node> subsetofChildrenNodesForThisTree = new ArrayList<>();
            
            for (final Node n : childrenNodes) {
                if (n.values[attributeToSplitOn.attributeIndex] == valueToFactorOut)
                    subsetofChildrenNodesForThisTree.add(n);
            }
            
            // Each child gets its own copy of used attributes (including attributeToSplitOn.index)
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

    public String toString(final boolean printSize, final int classification) {
        final StringBuilder sb = new StringBuilder();
        sb.append("INCLUSIVE RULES:\n");
        buildTreeString(sb, this, 0, printSize, classification, true);
        sb.append("\nEXCLUSIVE RULES:\n");
        buildTreeString(sb, this, 0, printSize, classification, false);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(false, -1);
    }

    private static String indent (final int depth) {
        return "\t".repeat(depth - 1) +
                "|----";
    }

    // Recursive helper for indentation-based printing (single-line format)
    // Indentation: each level adds "|- - " so it visually flows down
    private void buildTreeString(final StringBuilder sb, final RuleNode node, final int depth, final boolean printSize, final int classification, final boolean isInclusive) {
        if (node == null) return;

        final RuleNode[] ruleset = isInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;

        final String classificationString = Node.IMPOSSIBLE_CLASSIFICATION == classification ? "IMPOSSIBLE" : Integer.toString(classification);
        // root line
        if (depth == 0) {
            final StringBuilder rootLine = new StringBuilder();
            if (classification >= 0) {
                rootLine.append("CLASS: ")
                        .append(classificationString)
                        .append(" ROOT");
            } else {
                rootLine.append("ROOT");
            }
            if (printSize)
                rootLine.append(" [size: ")
                        .append(subtreeSize(node, isInclusive))
                        .append("]");
            sb.append(rootLine).append('\n');

            if (ruleset != null) {
                for (final RuleNode child : ruleset) {
                    buildTreeString(sb, child, depth + 1, printSize, classification, isInclusive);
                }
            }
            return;
        }

        // non-root nodes are handled by caller; this block used when recursing into children
        // but we still guard here so method can be called on arbitrary nodes too
        if (ruleset != null && subtreeSize(node, isInclusive) == 1) {

            // Collapse single-branch chain into one line
            final StringBuilder line = new StringBuilder();
            line.append(indent(depth));

            // collect chain pieces
            RuleNode cur = node;
            boolean first = true;
            while (cur != null) {
                final String attr = cur.parent == null ? "ROOT" : String.valueOf(cur.attributeIndex + 1);
                final String val = cur.parent == null ? "ROOT" : String.valueOf(cur.attributeValue);
                if (!first) line.append(" & ");
                line.append("X").append(attr).append(" >= ").append(val);
                first = false;

                // proceed only if exactly one child; otherwise stop
                final RuleNode[] curRuleset = isInclusive ? cur.inclusiveRuleset : cur.exclusiveRuleset;
                if (curRuleset == null)
                    break;
                cur = curRuleset[0];
            }

            if (printSize) {
                line.append(" [size: ").append(subtreeSize(node, isInclusive)).append("]");
            }
            sb.append(line).append('\n');
            // don't recurse down the collapsed chain
            return;
        }

        // regular-printing for nodes with multiple leaves in subtree
        final StringBuilder line = new StringBuilder();
        line.append(indent(depth))
                .append("X").append(node.attributeIndex + 1)
                .append(" >= ").append(node.attributeValue);
        if (printSize) {
            line.append(" [size: ").append(subtreeSize(node, isInclusive)).append("]");
        }
        sb.append(line).append('\n');

        if (ruleset != null) {
            for (final RuleNode child : ruleset) {
                buildTreeString(sb, child, depth + 1, printSize, classification, isInclusive);
            }
        }
    }

    // Counts leaf nodes only.
    public int subtreeSize(final RuleNode node, final boolean useInclusive) {
        if (node == null) 
            return 0;
        
        final RuleNode[] ruleset = useInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;
        if (ruleset == null)
            return 1;

        return Arrays.stream(ruleset)
            .mapToInt(child -> subtreeSize(child, useInclusive))
            .sum();
    }

    public static int getNumberOfClauses(final RuleNode node, final boolean useInclusive) {
        if (node == null)
            return 0;

        // Count this node as one clause (since it represents a comparison) IFF it is not null attributeIndex, since that would be the rootnode which is just a container.
        final int count = node.attributeIndex == null ? 0 : 1;

        final RuleNode[] nodeChildren = useInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;
        if (nodeChildren != null) {
            return count + Arrays.stream(nodeChildren)
                    .mapToInt(ruleNode -> getNumberOfClauses(ruleNode, useInclusive)).sum();
        }
        return count;
    }

}
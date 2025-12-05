package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import java.util.*;
import java.util.stream.IntStream;

import io.github.ryan_glgr.hansel_grapher.Stats.AttributeStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;

public class RuleNode {
    public final Integer attributeIndex;
    public final Integer attributeValue;
    public RuleNode[] children; // grouped children (one per attribute value)
    public final Set<Integer> attributesAlreadyUsed; // immutable for a node (copy-per-node)
    private RuleNode parent;
    
    /**
     * Create the full decision-rule tree for the supplied nodes.
     * Returns the root RuleNode (attributeIndex and attributeValue will be null at root).
     * If nodes is null or empty, returns null.
     */
    public static RuleNode createRuleNodes(final ArrayList<Node> nodes, final int numAttributes, final boolean findOptimalChildren) {
        if (nodes == null || nodes.isEmpty()) 
            return null;

        // attributeIndex and attributeValue are null for the root (no attribute was used to get here)
        final RuleNode root = new RuleNode(null, null, nodes, new HashSet<>(), numAttributes, findOptimalChildren);
        removeDeadbeatParents(root);
        return root;
    }

    // Constructor requires the set of attributes already used along the path.
    private RuleNode(final Integer attributeIndex,
                     final Integer attributeValue,
                     final ArrayList<Node> childrenNodes,
                     final Set<Integer> attributesAlreadyUsed,
                     final int numAttributes,
                     final boolean findingOptimalChildren) {
        this.attributeIndex = attributeIndex;
        this.attributeValue = attributeValue;
        // store an unmodifiable copy for this node (or just a private copy)
        this.attributesAlreadyUsed = new HashSet<>(attributesAlreadyUsed);
        
        // Note: don't add attributeIndex here — attributeIndex is the attribute used to
        // get to this node. If you want this node's attribute to be considered "used"
        // for its children, add it when creating children below.
        if (findingOptimalChildren) {
            this.children = findOptimalChildren(childrenNodes, numAttributes);
        }
        else {
            this.children = findChildrenGreedyTechnique(childrenNodes, numAttributes);
        }
        
        // take control of my children
        if (children != null)
            for(RuleNode kid : children){
                kid.parent = this;
            }
    }

    private static void removeDeadbeatParents(final RuleNode root){
        separateKidsFromParents(root);
    }

    // takes nodes which had 0 value, and gives their kids to the grandparents.
    private static RuleNode[] separateKidsFromParents(final RuleNode node){

        if (node == null)
            return null;

        // Leaf node: just keep or drop based on value
        if (node.children == null) {
            // if this node was an attribute with value 0, just delete it.
            if (node.parent != null && node.attributeValue == 0) {
                return null; // remove zero leaf
            } else {
                return new RuleNode[]{node}; // keep non-zero leaf
            }
        }

        // Process children first (bottom-up)
        final List<RuleNode> newChildrenList = new ArrayList<>();
        for (RuleNode child : node.children) {

            // recursive call! get all kids from belowm and slap those onto our list.
            RuleNode[] replacement = separateKidsFromParents(child);
            if (replacement == null)
                continue;

            for (RuleNode r : replacement) {
                r.parent = node; // rehome to current node
                newChildrenList.add(r);
            }
        }

        // If this node itself is zero, we don’t remove it here (only its children are lifted)
        if (node.parent != null && node.attributeValue == 0) {
            // node is zero: replace itself with its children for parent
            return newChildrenList.toArray(new RuleNode[0]);
        }
        else {
            // node is non-zero: update its children
            node.children = newChildrenList.isEmpty()
                    ? null
                    : newChildrenList.toArray(new RuleNode[0]);
            return new RuleNode[]{node};
        }
    }

    private final static Comparator<AttributeStats> greedyLeastBranchesComparison = Comparator
        .comparingInt((AttributeStats a) -> a.numberOfDistinctKValues)
        .thenComparingInt(a -> -a.maxGroupSize)
        .thenComparingInt(a -> a.attributeIndex);

    private RuleNode[] findOptimalChildren(final ArrayList<Node> childrenNodes, final int dimension) {
        if (childrenNodes == null || childrenNodes.isEmpty()) {
            return null; // leaf node
        }
        final List<AttributeStats> stats = getAttributeStatsForUnusedAttributes(childrenNodes, dimension);
        if (stats.isEmpty()) {
            return null;
        }
        
        final List<RuleNode[]> allPossibleChildTrees = stats.stream()
                .map(attributeToSplitOn -> createChildNodesFromAttributeStats(childrenNodes,
                        dimension,
                        attributeToSplitOn,
                        true))
                .toList();
        
        RuleNode[] mostOptimalChildren = null;
        int mostOptimalNumClauses = Integer.MAX_VALUE;
        for (RuleNode[] setOfChildNodes : allPossibleChildTrees) {
            final int numClauses = Arrays.stream(setOfChildNodes)
                    .mapToInt(RuleNode::getNumberOfClauses)
                    .sum();
            if (numClauses < mostOptimalNumClauses) {
                mostOptimalNumClauses = numClauses;
                mostOptimalChildren = setOfChildNodes;
            }
        }
        
        return mostOptimalChildren;
    }
    
    private RuleNode[] findChildrenGreedyTechnique(final ArrayList<Node> childrenNodes, final int dimension) {
        if (childrenNodes == null || childrenNodes.isEmpty()) {
            return null; // leaf node
        }

        final List<AttributeStats> stats = getAttributeStatsForUnusedAttributes(childrenNodes, dimension);

        if (stats.isEmpty()) {
            return null;
        }

        AttributeStats best = stats.stream().min(greedyLeastBranchesComparison).orElse(null);

        return createChildNodesFromAttributeStats(childrenNodes, dimension, best, false);
    }

    private List<AttributeStats> getAttributeStatsForUnusedAttributes(final ArrayList<Node> childrenNodes, final int dimension) {
        // Build stats for each unused attribute. Use sequential if Node.dimension small.
        final List<AttributeStats> stats = IntStream.range(0, dimension)
            .filter(i -> !attributesAlreadyUsed.contains(i))
            .mapToObj(i -> {
                HashMap<Integer, Integer> counts = new HashMap<>();
                for (Node n : childrenNodes) {
                    // if Node.values is Integer[], avoid NPE and use .intValue()
                    int val = n.values[i];
                    counts.put(val, counts.getOrDefault(val, 0) + 1);
                }
                return new AttributeStats(i, counts);
            })
            .toList();
        return stats;
    }

    private RuleNode[] createChildNodesFromAttributeStats(final ArrayList<Node> childrenNodes, final int dimension, final AttributeStats attributeToSplitOn, final boolean findingOptimalChildren) {
        // attribute to split on is the particular x attribute which we are factoring out of the rule tree next. the counts is a map of how many occurences of each particular k value we see in the childNodes of this RuleNode.
        final ArrayList<RuleNode> newChildren = new ArrayList<>();
        final List<Integer> distinctValuesForThisAttribute = new ArrayList<>(attributeToSplitOn.countsOfEachKValueForThisAttribute.keySet());
        Collections.sort(distinctValuesForThisAttribute);

        for (int valueToFactorOut : distinctValuesForThisAttribute) {
            final ArrayList<Node> subsetofChildrenNodesForThisTree = new ArrayList<>();
            
            for (Node n : childrenNodes) {
                if (n.values[attributeToSplitOn.attributeIndex] == valueToFactorOut)
                    subsetofChildrenNodesForThisTree.add(n);
            }
            
            // Each child gets its own copy of used attributes (including attributeToSplitOn.index)
            final Set<Integer> childUsed = new HashSet<>(this.attributesAlreadyUsed);
            childUsed.add(attributeToSplitOn.attributeIndex);
            newChildren.add(new RuleNode(attributeToSplitOn.attributeIndex, valueToFactorOut, subsetofChildrenNodesForThisTree, childUsed, dimension, findingOptimalChildren));
        }
        return newChildren.toArray(new RuleNode[0]);
    }

    public String toString(boolean printSize, int classification) {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, this, 0, printSize, classification);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(false, -1);
    }

    private static String indent (final int depth) {
        return new StringBuilder()
            .append("\t".repeat(depth - 1))
            .append("|----")
            .toString();
    }
    
    // Recursive helper for indentation-based printing (single-line format)
    // Indentation: each level adds "|- - " so it visually flows down
    private void buildTreeString(final StringBuilder sb, final RuleNode node, final int depth, final boolean printSize, final int classification) {
        if (node == null) return;

        // root line
        if (depth == 0) {
            StringBuilder rootLine = new StringBuilder();
            if (classification >= 0) {
                rootLine.append("CLASS: ")
                    .append(classification)
                    .append(" ROOT");
            } else {
                rootLine.append("ROOT");
            }
            if (printSize) 
                rootLine.append(" [size: ")
                    .append(subtreeSize(node))
                    .append("]");
            sb.append(rootLine).append('\n');

            if (node.children != null) {
                for (RuleNode child : node.children) {
                    buildTreeString(sb, child, depth + 1, printSize, classification);
                }
            }
            return;
        }

        // non-root nodes are handled by caller; this block used when recursing into children
        // but we still guard here so method can be called on arbitrary nodes too
        if (node.children != null && subtreeSize(node) == 1) {
            
            // Collapse single-branch chain into one line
            StringBuilder line = new StringBuilder();
            line.append(indent(depth));

            // collect chain pieces
            RuleNode cur = node;
            boolean first = true;
            while (cur != null) {
                String attr = cur.parent == null ? "ROOT" : String.valueOf(cur.attributeIndex + 1);
                String val = cur.parent == null ? "ROOT" : String.valueOf(cur.attributeValue);
                if (!first) line.append(" & ");
                line.append("X").append(attr).append(" >= ").append(val);
                first = false;

                // proceed only if exactly one child; otherwise stop
                if (cur.children == null) 
                    break;
                cur = cur.children[0];
            }

            if (printSize) {
                line.append(" [size: ").append(subtreeSize(node)).append("]");
            }
            sb.append(line).append('\n');
            // don't recurse down the collapsed chain
            return;
        }

        // regular-printing for nodes with multiple leaves in subtree
        StringBuilder line = new StringBuilder();
        line.append(indent(depth))
            .append("X").append(String.valueOf(node.attributeIndex + 1))
            .append(" >= ").append(String.valueOf(node.attributeValue));
        if (printSize) {
            line.append(" [size: ").append(subtreeSize(node)).append("]");
        }
        sb.append(line).append('\n');

        if (node.children != null) {
            for (RuleNode child : node.children) {
                buildTreeString(sb, child, depth + 1, printSize, classification);
            }
        }
    }

    // Counts leaf nodes only.
    public int subtreeSize(final RuleNode node) {
        if (node == null) 
            return 0;
        if (node.children == null) 
            return 1;

        return Arrays.stream(node.children)
            .mapToInt(ruleTree -> subtreeSize(ruleTree))
            .sum();
    }

    public static int getNumberOfClauses(final RuleNode node) {
        if (node == null)
            return 0;

        // Count this node as one clause (since it represents a comparison) IFF it is not null attributeIndex, since that would be the rootnode which is just a container.
        int count = node.attributeIndex == null ? 0 : 1;

        if (node.children != null) {
            count += Arrays.stream(node.children)
                    .mapToInt(RuleNode::getNumberOfClauses)
                    .sum();
        }
        return count;
    }

}
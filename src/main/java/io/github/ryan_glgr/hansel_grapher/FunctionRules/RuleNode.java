package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;

public class RuleNode {
    public final Integer attributeIndex;
    public final Integer attributeValue;
    public final RuleNode[] children; // grouped children (one per attribute value)
    public final Set<Integer> attributesAlreadyUsed; // immutable for a node (copy-per-node)


    /**
     * Create the full decision-rule tree for the supplied nodes.
     * Returns the root RuleNode (attributeIndex and attributeValue will be null at root).
     * If nodes is null or empty, returns null.
     */
    public static RuleNode createRuleNodes(ArrayList<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) 
            return null;

        // attributeIndex and attributeValue are null for the root (no attribute was used to get here)
        return new RuleNode(null, null, nodes, new HashSet<>());
    }

    // Constructor requires the set of attributes already used along the path.
    private RuleNode(Integer attributeIndex,
                    Integer attributeValue,
                    ArrayList<Node> childrenNodes,
                    Set<Integer> attributesAlreadyUsed) {
        this.attributeIndex = attributeIndex;
        this.attributeValue = attributeValue;
        // store an unmodifiable copy for this node (or just a private copy)
        this.attributesAlreadyUsed = new HashSet<>(attributesAlreadyUsed);
        // Note: don't add attributeIndex here — attributeIndex is the attribute used to
        // get to this node. If you want this node's attribute to be considered "used"
        // for its children, add it when creating children below.
        this.children = findChildren(childrenNodes);
    }

    private final static Comparator<AttributeStats> greedyLeastBranchesComparison = Comparator
        .comparingInt((AttributeStats a) -> a.branches)
        .thenComparingInt(a -> -a.maxGroupSize)
        .thenComparingInt(a -> a.index);

    private RuleNode[] findChildren(ArrayList<Node> childrenNodes) {

        if (childrenNodes == null || childrenNodes.isEmpty()) {
            return null; // consistent: empty array
        }

        // Build stats for each unused attribute. Use sequential if Node.dimension small.
        List<AttributeStats> stats = IntStream.range(0, Node.dimension)
            .parallel() // remove .parallel() if Node.dimension is small
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
            .collect(Collectors.toList());

        if (stats.isEmpty()) {
            return null;
        }

        AttributeStats best = stats.stream().min(greedyLeastBranchesComparison).orElse(null);
        if (best == null) 
            return null;

        ArrayList<RuleNode> children = new ArrayList<>(best.branches);
        List<Integer> distinctValues = new ArrayList<>(best.counts.keySet());
        Collections.sort(distinctValues);

        for (int val : distinctValues) {
            ArrayList<Node> subset = new ArrayList<>(best.counts.get(val));
            for (Node n : childrenNodes) {
                if (n.values[best.index] == val) subset.add(n);
            }

            // Each child gets its own copy of used attributes (including best.index)
            Set<Integer> childUsed = new HashSet<>(this.attributesAlreadyUsed);
            childUsed.add(best.index);

            RuleNode child = new RuleNode(best.index, val, subset, childUsed);
            children.add(child);
        }

        return children.toArray(new RuleNode[0]);
    }
                
    public void printTree(boolean printSize, int classification) {
        printTreeHelper(this, 0, printSize, classification);
    }

    private static final String indent (int depth) {
        return new StringBuilder()
            .append("\t".repeat(depth - 1))
            .append("|----")
            .toString();
    }
    
    // Recursive helper for indentation-based printing (single-line format)
    // Indentation: each level adds "|- - " so it visually flows down
    private void printTreeHelper(RuleNode node, int depth, boolean printSize, int classification) {
        if (node == null) return;

        // root line
        if (depth == 0) {
            StringBuilder rootLine = new StringBuilder("CLASS: " + classification + " ROOT");
            if (printSize) 
                rootLine.append(" [size: ")
                    .append(subtreeSize(node))
                    .append("]");
            System.out.println(rootLine.toString());

            if (node.children != null) {
                for (RuleNode child : node.children) {
                    printTreeHelper(child, depth + 1, printSize, classification);
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
                String attr = cur.attributeIndex == null ? "ROOT" : String.valueOf(cur.attributeIndex);
                String val = cur.attributeValue == null ? "ROOT" : String.valueOf(cur.attributeValue);
                if (!first) line.append(" v ");
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
            System.out.println(line.toString());
            // don't recurse down the collapsed chain
            return;
        }

        // regular-printing for nodes with multiple leaves in subtree
        StringBuilder line = new StringBuilder();
        line.append(indent(depth))
            .append("X").append(String.valueOf(node.attributeIndex))
            .append(" >= ").append(String.valueOf(node.attributeValue));
        if (printSize) {
            line.append(" [size: ").append(subtreeSize(node)).append("]");
        }
        System.out.println(line.toString());

        if (node.children != null) {
            for (RuleNode child : node.children) {
                printTreeHelper(child, depth + 1, printSize, classification);
            }
        }
    }

    // Counts leaf nodes only.
    public int subtreeSize(RuleNode node) {
        if (node == null) 
            return 0;
        if (node.children == null) 
            return 1;

        return Arrays.stream(node.children)
            .mapToInt(ruleTree -> subtreeSize(ruleTree))
            .sum();
    }
}
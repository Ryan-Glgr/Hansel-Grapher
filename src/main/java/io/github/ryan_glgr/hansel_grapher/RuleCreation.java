package io.github.ryan_glgr.hansel_grapher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class RuleCreation {

    public static class RuleNode {
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
                return new RuleNode[0]; // consistent: empty array
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
                return new RuleNode[0];
            }

            AttributeStats best = stats.stream().min(greedyLeastBranchesComparison).orElse(null);
            if (best == null) return new RuleNode[0];

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
    
        public void printTree() {
            printTreeHelper(this, 0);
        }

        // Recursive helper for indentation-based printing
        private void printTreeHelper(RuleNode node, int depth) {
            if (node == null) return;

            String indent = "\t".repeat(depth);
            System.out.println(indent + "Attribute: " + (node.attributeIndex == null ? "ROOT NODE" : node.attributeIndex));
            System.out.println(indent + "Value: " + (node.attributeValue == null ? "\t\tROOT NODE" : node.attributeValue));

            if (node.children != null) {
                for (RuleNode child : node.children) {
                    printTreeHelper(child, depth + 1);
                }
            }
        }

        // --- 2. Size computation ---
        public int getTreeSize() {
            return countNodes(this);
        }

        private int countNodes(RuleNode node) {
            if (node == null) return 0;

            int size = 1; // count this node
            if (node.children != null) {
                for (RuleNode child : node.children) {
                    size += countNodes(child);
                }
            }
            return size;
        }
    
    }

    static class AttributeStats {
        final int index;
        final HashMap<Integer, Integer> counts; // value -> occurrence
        final int branches; // distinct values
        final int maxGroupSize; // largest occurrence for a single value

        AttributeStats(int index, HashMap<Integer,Integer> counts) {
            this.index = index;
            this.counts = counts;
            this.branches = counts.size();
            int max = 0;
            for (int c : counts.values()) if (c > max) max = c;
            this.maxGroupSize = max;
        }
    }

}

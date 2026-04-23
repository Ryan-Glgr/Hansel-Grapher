package io.github.ryan_glgr.hansel_grapher.Visualizations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class VisualizationDOT {
    
    // --- Constants ---
    private static final String NODE_SHAPE = "rectangle";
    private static final String OUTPUT_DIRECTORY = "out";
    private static final String EXPANSIONS_FILE_NAME = "Expansions.dot";
    private static final String HANSEL_CHAINS_FILE_NAME = "HanselChains.dot";
    private static final String RULE_TREES_FILE_NAME = "RuleTrees.dot";
    private static final String COMPILE_SCRIPT_PATH = "Visualizations" + File.separator + "compile_dot.sh";
    private static final String PHONY_FILE_NAME = "phony.txt";
 
    private static ArrayList<ArrayList<Node>> sortChainsForVisualization(final ArrayList<ArrayList<Node>> chainSet){

        // Apply the sort result back to hanselChainSet
        chainSet.sort((final ArrayList<Node> a, final ArrayList<Node> b) -> b.size() - a.size());

        // now give them the diamond shape.
        final ArrayList<ArrayList<Node>> newOrdering = new ArrayList<ArrayList<Node>>();
        for(int i = 0; i < chainSet.size(); i++){
            // if even, put it in the front, if odd, the back. that is how we will alternate and get that shape.
            if (i % 2 == 0){
                newOrdering.add(chainSet.get(i));
            }
            else{
                newOrdering.addFirst(chainSet.get(i));
            }
        }
        // change the pointer to hanselChainSet to the reordered ones.
        return newOrdering;
    }

    // Generate a color for a given classification using HSV color space
    private static String getColorForClass(final int classification, final boolean isLowUnit) {
        // Special handling for impossible classifications - use dark gray
        if (classification == Node.IMPOSSIBLE_CLASSIFICATION) {
            // Dark gray color with transparency
            final int grayValue = 64; // Dark gray (0-255 scale)
            final float alpha = isLowUnit ? 1.0f : 0.65f;
            final int alphaInt = Math.round(alpha * 255);
            return String.format("#%02x%02x%02x%02x", grayValue, grayValue, grayValue, alphaInt);
        }

        // Use golden ratio to space out hues nicely
        final float goldenRatio = 0.618033988749895f;

        // Generate hue by spacing classifications evenly and offsetting by golden ratio
        final float hue = (classification * goldenRatio) % 1.0f;

        // Keep saturation and value high for vibrant, distinct colors
        final float saturation = 0.7f;
        final float value = 0.95f;

        // Convert HSV to RGB
        final int rgb = Color.HSBtoRGB(hue, saturation, value);
        final Color baseColor = new Color(rgb);

        // Adjust alpha based on whether the node is a low unit
        final float alpha = isLowUnit ? 1.0f : 0.65f;

        // Create RGBA color with transparency
        final Color colorWithAlpha = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), Math.round(alpha * 255));

        // Convert to hex format with alpha (Graphviz supports #RRGGBBAA)
        return String.format("#%02x%02x%02x%02x",
                colorWithAlpha.getRed(),
                colorWithAlpha.getGreen(),
                colorWithAlpha.getBlue(),
                colorWithAlpha.getAlpha());
    }

    // --- Escaping helper ---
    private static String escapeQuote(final String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    // --- Shared node-writing helper ---
    private static void writeNode(final FileWriter fw, final Node temp, final boolean isLow) throws IOException {
        final StringBuilder attr = new StringBuilder();
        final String label = nodeLabel(temp, isLow);
        attr.append("label = \"").append(escapeQuote(label)).append("\"");        
        final String nodeColor = getColorForClass(temp.classification, isLow);

        attr.append("label = \"").append(escapeQuote(label)).append("\"");
        attr.append(", shape = ").append(NODE_SHAPE);
        attr.append(", style = filled");
        attr.append(", fillcolor = \"").append(nodeColor).append("\"");

        fw.write(temp.hashCode() + " [" + attr.toString() + "];\n\t");
    }

    private static void writeRuleNode(final FileWriter fw,
                                      final RuleNode node,
                                      final int classification,
                                      final String color,
                                      final String[] attributeNames) throws IOException {

        final int id = System.identityHashCode(node);

        final String label;
        if (node.attributeIndex == null) {
            label = "ROOT";
        } else {
            label = attributeNames[node.attributeIndex] + " >= " + node.attributeValue;
        }

        fw.write(id + " [");
        fw.write("label = \"" + label + "\", ");
        fw.write("shape = rectangle, ");
        fw.write("style = filled, ");
        fw.write("fillcolor = \"" + color + "\"");
        fw.write("];\n\t");
    }

    private static String nodeLabel(final Node temp, final boolean isLow) {

        final String cls;
        if (Node.IMPOSSIBLE_CLASSIFICATION.equals(temp.classification)) {
            cls = "N/A";
        } else {
            cls = (isLow)
                ? String.format("*%s*", temp.classification)
                : String.format(" %s ", temp.classification);
        }
        return Arrays.toString(temp.values) + "\\nClassification: " + cls;
    }


    // --- makeExpansionsDOT ---
    public static void makeExpansionsDOT(final HashMap<Integer, Node> allNodes,
                                         final ArrayList<ArrayList<Node>> lowUnitsByClass,
                                         final Integer[] kValues) throws IOException {
        final HashSet<Node> lowSet = new HashSet<>();
        if (lowUnitsByClass != null)
            for (final ArrayList<Node> listForClass : lowUnitsByClass)
                if (listForClass != null) lowSet.addAll(listForClass);

        final Integer[] kValsToMakeNode = Node.counterInitializer(kValues);
        final HashMap<Node, Node> usedNodes = new HashMap<>();
        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + EXPANSIONS_FILE_NAME);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        while (Node.incrementCounter(kValsToMakeNode, kValues)) {
            final Node temp = allNodes.get(Node.hash(kValsToMakeNode));
            if (!usedNodes.containsKey(temp)) {
                usedNodes.put(temp, temp);
                writeNode(fw, temp, lowSet.contains(temp));
            }

            for (final Node ex : temp.upExpansions) {
                if (ex == null) continue;
                if (!usedNodes.containsKey(ex)) {
                    usedNodes.put(ex, ex);
                    writeNode(fw, ex, lowSet.contains(ex));
                }
                fw.write(temp.hashCode() + " -> " + ex.hashCode() +
                        " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
            }
        }

        fw.write("}");
        fw.close();
    }

    // --- makeHanselChainDOT ---
    public static void makeHanselChainDOT(ArrayList<ArrayList<Node>> chains, final ArrayList<ArrayList<Node>> lowUnitsByClass) throws IOException {
        chains = sortChainsForVisualization(chains);

        final HashSet<Node> lowSet = new HashSet<>();
        if (lowUnitsByClass != null)
            for (final ArrayList<Node> listForClass : lowUnitsByClass)
                if (listForClass != null) lowSet.addAll(listForClass);

        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + HANSEL_CHAINS_FILE_NAME);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        final ArrayList<Node> middleNodes = new ArrayList<>();

        for (final ArrayList<Node> chain : chains) {
            middleNodes.add(chain.get(chain.size() / 2));

            for (final Node temp : chain) {
                writeNode(fw, temp, lowSet.contains(temp));
            }

            for (int c = 0; c < chain.size() - 1; c++) {
                final Node temp = chain.get(c);
                final Node ex = chain.get(c + 1);
                fw.write(temp.hashCode() + " -> " + ex.hashCode() +
                        " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
            }
        }

        fw.write("{ rank = same; ");
        for (final Node mid : middleNodes) fw.write(mid.hashCode() + " ");
        fw.write("};\n");

        fw.write("}");
        fw.close();
    }

    private static void traverseRuleTree(final FileWriter fw,
                                         final RuleNode node,
                                         final String color,
                                         final String[] attributeNames) throws IOException {

        final int id = System.identityHashCode(node);

        final String label = (node.attributeIndex == null)
                ? "ROOT"
                : attributeNames[node.attributeIndex] + " >= " + node.attributeValue;

        fw.write(id + " [label=\"" + label + "\", style=filled, fillcolor=\"" + color + "\"];\n\t");

        if (node.children == null) return;

        for (final RuleNode child : node.children) {
            final int childId = System.identityHashCode(child);
            traverseRuleTree(fw, child, color, attributeNames);
            fw.write(id + " -> " + childId + ";\n\t");
        }
    }

    public static void makeRuleTreesDOT(final RuleNode[] ruleTrees,
                                        final String[] attributeNames) throws IOException {

        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + RULE_TREES_FILE_NAME);
        fw.write("digraph G {\n\trankdir=TB;\n\tbgcolor=white;\n\t");

        for (int c = 1; c < ruleTrees.length; c++) {
            if (ruleTrees[c] == null) continue;

            fw.write("subgraph cluster_" + c + " {\n\tstyle=invis;\n\t");

            final String color = getColorForClass(c, true);
            traverseRuleTree(fw, ruleTrees[c], color, attributeNames);

            fw.write("}\n\t");
        }

        fw.write("}");
        fw.close();
    }


    public static void compileDotAsync(final String dotPath) {
        CompletableFuture.runAsync(() -> {
            try {

                // ensure output directory exists
                final File outputDir = new File(OUTPUT_DIRECTORY + File.separator + PHONY_FILE_NAME).getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                final ProcessBuilder pb = new ProcessBuilder("." + File.separator + COMPILE_SCRIPT_PATH, dotPath);
                pb.directory(new File("."));
                final Process process = pb.start();
                process.onExit().thenAccept(p -> {
                    if (p.exitValue() != 0) {
                        System.err.println("compile_dot.sh exited with code " + p.exitValue() + " for " + dotPath);
                    }
                });
            } catch (final IOException ex) {
                System.err.println("Failed to compile DOT file: " + dotPath);
                ex.printStackTrace();
            }
        });
    }

}

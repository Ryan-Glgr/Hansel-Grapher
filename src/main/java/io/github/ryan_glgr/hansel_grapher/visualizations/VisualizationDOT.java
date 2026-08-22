package io.github.ryan_glgr.hansel_grapher.visualizations;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.functionrules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.helper.Util;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_HEIGHT;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_WIDTH;

public class VisualizationDOT {

    // --- Constants ---
    private static final String NODE_SHAPE = "rectangle";
    private static final String OUTPUT_DIRECTORY = "out";
    private static final String EXPANSIONS_FILE_NAME = "Expansions.dot";
    private static final String HANSEL_CHAINS_FILE_NAME = "HanselChains.dot";
    private static final String RULE_TREES_FILE_NAME = "RuleTrees.dot";
    private static final String COMPILE_SCRIPT_PATH = "visualizationscripts" + File.separator + "compile_pdf.sh";
    private static final String PHONY_FILE_NAME = "phony.txt";
    private static final int POINTS_PER_INCH = 72;
    private static final float FONT_HEIGHT_FRACTION = 0.16f; // fraction of node height per line of text


    // world-units -> inches. Graphviz interprets pos/width/height in inches by default.
    private static final float DOT_SCALE = 0.3f;

    // --- Escaping helper ---
    private static String escapeQuote(final String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private static void writeNode(final FileWriter fw, final Node temp, final LowUnit.Type lowUnitType) throws IOException {
        final String[] labelParts = GUIHelper.nodeLabelArray(temp, lowUnitType);
        final String label = String.join("\\n", labelParts);

        final String attr = "label = \"" + escapeQuote(label) + "\"" +
                ", shape = " + NODE_SHAPE +
                ", style = filled" +
                ", fillcolor = \"" + GUIHelper.colorToHex(GUIHelper.getColorForClass(temp.classification, Objects.isNull(lowUnitType))) + "\"";

        fw.write(temp.hashCode() + " [" + attr + "];\n\t");
    }

    private static void writeNodeAtPosition(final FileWriter fw, final Node temp, final LowUnit.Type lowUnitType,
                                            final float worldX, final float worldY) throws IOException {
        final String[] labelParts = GUIHelper.nodeLabelArray(temp, lowUnitType);
        final String label = String.join("\\n", labelParts);

        final float posX = worldX * DOT_SCALE;
        final float posY = worldY * DOT_SCALE;
        final float nodeHeightInches = HanselChainLayout.NODE_HEIGHT * DOT_SCALE;
        final int fontSize = Math.max(6, Math.round(nodeHeightInches * POINTS_PER_INCH * FONT_HEIGHT_FRACTION));

        final String attr = "label = \"" + escapeQuote(label) + "\"" +
                ", shape = " + NODE_SHAPE +
                ", style = filled" +
                ", pos = \"" + posX + "," + posY + "!\"" +
                ", width = " + (HanselChainLayout.NODE_WIDTH * DOT_SCALE) +
                ", height = " + nodeHeightInches +
                ", fixedsize = true" +
                ", fontsize = " + fontSize +
                ", fillcolor = \"" + GUIHelper.colorToHex(GUIHelper.getColorForClass(temp.classification, Objects.isNull(lowUnitType))) + "\"";

        fw.write(temp.hashCode() + " [" + attr + "];\n\t");
    }

    // --- makeExpansionsDOT --- (unchanged — no known layout for this graph, dot still lays it out)
    public static void makeExpansionsDOT(final HashMap<Integer, Node> allNodes,
                                         final Map<Integer, Set<LowUnit>> lowUnitsByClass,
                                         final Integer[] kValues) throws IOException {

        final Map<Node, LowUnit> nodeLowUnitMap = lowUnitsByClass.values()
                .stream()
                .flatMap(Set<LowUnit>::stream)
                .collect(Collectors.toMap(LowUnit::getDatapoint, Function.identity()));

        final Integer[] kValsToMakeNode = Util.counterInitializer(kValues);
        final HashMap<Node, Node> usedNodes = new HashMap<>();
        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + EXPANSIONS_FILE_NAME);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        while (Node.incrementCounter(kValsToMakeNode, kValues)) {
            final Node temp = allNodes.get(Node.hash(kValsToMakeNode));
            if (!usedNodes.containsKey(temp)) {
                usedNodes.put(temp, temp);
                final LowUnit lowUnit = nodeLowUnitMap.get(temp);
                writeNode(fw, temp, Objects.isNull(lowUnit) ? null : lowUnit.getLowUnitType());
            }

            for (final Node ex : temp.upExpansions) {
                if (ex == null) continue;
                if (!usedNodes.containsKey(ex)) {
                    usedNodes.put(ex, ex);
                    final LowUnit lowUnit = nodeLowUnitMap.get(ex);
                    writeNode(fw, ex, Objects.isNull(lowUnit) ? null : lowUnit.getLowUnitType());
                }
                fw.write(temp.hashCode() + " -> " + ex.hashCode() +
                        " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
            }
        }

        fw.write("}");
        fw.close();
    }

    // --- makeHanselChainDOT --- now driven by HanselChainLayout: exact positions, no rank tricks needed.
    public static void makeHanselChainDOT(final ArrayList<ArrayList<Node>> chains,
                                          final Map<Integer, Set<LowUnit>> lowUnitsByClass) throws IOException {

        final HanselChainLayout layout = new HanselChainLayout(chains, lowUnitsByClass);
        final Node[][] nodeGrid = layout.getNodeGrid();

        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + HANSEL_CHAINS_FILE_NAME);
        fw.write("digraph G {\n\tbgcolor = white;\n\tsplines = line;\n\t");

        for (int c = 0; c < nodeGrid.length; c++) {
            final float worldX = layout.getX(c);

            for (int row = 0; row < nodeGrid[c].length; row++) {
                final Node node = nodeGrid[c][row];
                if (node == null) continue;

                writeNodeAtPosition(fw, node, layout.getLowUnitType(node), worldX, layout.getY(row));
            }

            for (int row = 0; row < nodeGrid[c].length - 1; row++) {
                final Node from = nodeGrid[c][row];
                final Node to   = nodeGrid[c][row + 1];
                if (from == null || to == null) continue;

                fw.write(from.hashCode() + " -> " + to.hashCode() +
                        " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
            }
        }

        fw.write("}");
        fw.close();
    }

    private static void traverseRuleTree(final FileWriter fw,
                                         final RuleNode node,
                                         final String color,
                                         final String[] attributeNames,
                                         final int classification,
                                         final boolean isInclusive) throws IOException {

        final int id = System.identityHashCode(node);

        final String label = (node.attributeIndex == null)
                ? "CLASS: " + classification + " ROOT"
                : attributeNames[node.attributeIndex] + " >= " + node.attributeValue;

        fw.write(id + " [label=\"" + label + "\", style=filled, fillcolor=\"" + color + "\"];\n\t");

        final RuleNode[] ruleset = isInclusive ? node.inclusiveRuleset : node.exclusiveRuleset;
        if (ruleset == null) return;

        for (final RuleNode child : ruleset) {
            final int childId = System.identityHashCode(child);
            traverseRuleTree(fw, child, color, attributeNames, classification, isInclusive);
            fw.write(id + " -> " + childId + ";\n\t");
        }
    }

    public static void makeRuleTreesDOT(final RuleNode[] ruleTrees,
                                        final String[] attributeNames,
                                        final LowUnit.Type lowUnitType) throws IOException {
        final boolean isInclusive = LowUnit.Type.INCLUSIVE.equals(lowUnitType);

        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + RULE_TREES_FILE_NAME);
        fw.write("digraph G {\n\trankdir=TB;\n\tbgcolor=white;\n\t");

        for (int classification = 1; classification < ruleTrees.length; classification++) {
            if (ruleTrees[classification] == null) continue;

            fw.write("subgraph cluster_" + classification + " {\n\tstyle=invis;\n\t");

            final String color = GUIHelper.colorToHex(GUIHelper.getColorForClass(classification, Objects.nonNull(lowUnitType)));
            traverseRuleTree(fw, ruleTrees[classification], color, attributeNames, classification, isInclusive);

            fw.write("}\n\t");
        }

        fw.write("}");
        fw.close();
    }

    // engine: "dot" (default, auto-layout) or "neato" (fixed positions via pos="x,y!")
    public static void compileDotAsync(final String dotPath) {
        CompletableFuture.runAsync(() -> {
            try {
                final File outputDir = new File(OUTPUT_DIRECTORY + File.separator + PHONY_FILE_NAME).getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                final ProcessBuilder pb = new ProcessBuilder("." + File.separator + COMPILE_SCRIPT_PATH, dotPath, "dot");
                pb.directory(new File("."));
                final Process process = pb.start();
                process.onExit().thenAccept(p -> {
                    if (p.exitValue() != 0) {
                        System.err.println("compiling exited with code " + p.exitValue() + " for " + dotPath);
                    }
                });
            } catch (final IOException ex) {
                System.err.println("Failed to compile DOT file: " + dotPath);
                ex.printStackTrace();
            }
        });
    }
}
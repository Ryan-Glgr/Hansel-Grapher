package io.github.ryan_glgr.hansel_grapher.visualizations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.functionrules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.helper.Util;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VisualizationDOT {
    
    // --- Constants ---
    private static final String NODE_SHAPE = "rectangle";
    private static final String OUTPUT_DIRECTORY = "out";
    private static final String EXPANSIONS_FILE_NAME = "Expansions.dot";
    private static final String HANSEL_CHAINS_FILE_NAME = "HanselChains.dot";
    private static final String RULE_TREES_FILE_NAME = "RuleTrees.dot";
    private static final String COMPILE_SCRIPT_PATH = "visualizationscripts" + File.separator + "compile_dot.sh";
    private static final String PHONY_FILE_NAME = "phony.txt";


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

    // --- makeExpansionsDOT ---
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

    // --- makeHanselChainDOT ---
    public static void makeHanselChainDOT(ArrayList<ArrayList<Node>> chains, final Map<Integer, Set<LowUnit>> lowUnitsByClass) throws IOException {
        chains = GUIHelper.sortChainsForVisualization(chains);

        final Map<Node, LowUnit> reverseMap = lowUnitsByClass.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toMap(LowUnit::getDatapoint, Function.identity()));

        final FileWriter fw = new FileWriter(OUTPUT_DIRECTORY + File.separator + HANSEL_CHAINS_FILE_NAME);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        final ArrayList<Node> middleNodes = new ArrayList<>();

        for (final ArrayList<Node> chain : chains) {
            middleNodes.add(chain.get(chain.size() / 2));

            for (final Node temp : chain) {
                final LowUnit lowUnit = reverseMap.get(temp);
                writeNode(fw, temp, Objects.isNull(lowUnit) ? null : lowUnit.getLowUnitType());
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

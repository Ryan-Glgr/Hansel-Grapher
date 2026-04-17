package io.github.ryan_glgr.hansel_grapher;

import com.formdev.flatlaf.FlatIntelliJLaf;
import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.*;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.MagicFunctionMode;
import io.github.ryan_glgr.hansel_grapher.Visualizations.GUI.MainScreen;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
// import java.awt.*;

public class Main {

    public static ArrayList<ArrayList<Node>> duplicateChain(final ArrayList<ArrayList<Node>> chains) {
        final ArrayList<ArrayList<Node>> dupChains = new ArrayList<>(chains.size());
        for (final ArrayList<Node> chain : chains) {
            final ArrayList<Node> dupChain = new ArrayList<Node>(chain.size());
            for (final Node n : chain) {
                dupChain.add(new Node(n));
            }
            dupChains.add(dupChain);
        }
        return dupChains;
    }

    public static void generateChains(final Integer[] kVals, final int numClasses) {
        final HashMap<Integer, Node> data = Node.makeNodes(kVals, numClasses);
        ArrayList<ArrayList<Node>> hanselChains;
        ArrayList<ArrayList<Node>> defaultChains = HanselChains.generateHanselChainSet(kVals, data);
        final HashSet<HashSet<Node>> lowUnits = new HashSet<>();
        final int[] sizes = defaultChains.stream().mapToInt(ArrayList::size).toArray();
        final int[] lowValueIndices = new int[sizes.length];
        final MagicFunctionMode magicFunctionMode = MagicFunctionMode.KNOWN_LOW_UNITS_MODE;

        long count = 0;

        while (true) {
            int index = sizes.length - 1;
            while (index >= 0) {
                lowValueIndices[index]++;
                
                if (lowValueIndices[index] <= sizes[index]) {
                    hanselChains = defaultChains;
                    defaultChains = duplicateChain(hanselChains);
                    hanselChains.getFirst().getLast().permeateClassification(0);
                    count++;
                    System.out.println(Arrays.toString(lowValueIndices));
                    for(int i = 0; i < lowValueIndices.length; i++) {
                        if(lowValueIndices[i] > 0) {
                            hanselChains.get(i).get(lowValueIndices[i] - 1).permeateClassification(1);
                        }
                    }
                    final var lowUnitsByClass = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
                    final var adjustedLowUnitsByClass = HanselChains.removeUselessLowUnits(lowUnitsByClass);
                    lowUnits.add(new HashSet<>(adjustedLowUnitsByClass.get(1)));
                    // for(Node n : adjustedLowUnitsByClass.get(1)) {
                    //     System.out.print(Arrays.toString(n.values));
                    //     System.out.print(" ");
                    // }
                    // System.out.println();
                    break;
                } else {
                    lowValueIndices[index] = 0;
                    index--;
                }
            }
            if (index < 0) {
                break;
            }
        }
        System.out.println(lowUnits.size());

        final Float[] fakeWeights = new Float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        final String[] fakeNames = new String[]{"","","","",""};
        final Interview[] subFunctionsForEachAttribute = new Interview[kVals.length];
        final InterviewMode[] modes = InterviewMode.values();
        final float[] questions = new float[modes.length];
        count = 0;

        for(final Set<Node> nodes: lowUnits) {
            final Set<Integer[]>[] lowUnitsToMakeTheFunctionTrue = new Set[2];
            lowUnitsToMakeTheFunctionTrue[1] = nodes.stream()
                    .map(node -> node.values)
                    .collect(Collectors.toSet());

            for(int i = 0; i < modes.length; i++) {
                final Interview interview = new Interview(kVals,
                    fakeWeights,
                    modes[i],
                    numClasses,
                    fakeNames,
                    fakeNames,
                    lowUnitsToMakeTheFunctionTrue,
                    subFunctionsForEachAttribute,
                    magicFunctionMode);

                final InterviewStats interviewStats = interview.interviewStats;
                questions[i] += interviewStats.nodesAsked.size();
            }
            count++;
            System.out.printf("%d/%d\n", count, lowUnits.size());
        }
        
        for(int i = 0; i < modes.length; i++) {
            questions[i] /= (float)lowUnits.size();
            System.out.println(modes[i].toString() + ": " + questions[i]);
        }
    }

    private static void visualizeStatistics(final Interview interview) {
        try{
            // ensure output directory exists
            final File outputDir = new File("out/phony.txt").getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }
            // visualize our results
            VisualizationDOT.makeHanselChainDOT(interview.hanselChains, interview.adjustedLowUnitsByClass);

            // make the expansions picture
            final Integer[] kValues = interview.kVals;
            VisualizationDOT.makeExpansionsDOT(interview.data, interview.adjustedLowUnitsByClass, kValues);
            VisualizationDOT.makeRuleTreesDOT(interview.ruleTrees, interview.attributeNames);

            final InterviewMode interviewMode = interview.interviewStats.interviewMode;
            final String interviewStatsOutputString = interviewMode + " Interview Stats";
            InterviewStatsVisualizer.savePDF(interview.interviewStats,
                "out/" + interviewStatsOutputString + ".pdf",
                    interviewMode);

            VisualizationDOT.compileDotAsync("out/HanselChains.dot");
            VisualizationDOT.compileDotAsync("out/Expansions.dot");
            VisualizationDOT.compileDotAsync("out/RuleTrees.dot");
        }
        catch (final Exception e){
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) {
        final boolean GUI = args.length > 0 && args[0].equals("--gui");
        if (GUI) {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    // Make Swing decorations (titlebar, borders) use the L&F
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    JDialog.setDefaultLookAndFeelDecorated(true);
                } catch (final UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                final MainScreen mainScreen = new MainScreen();

                final JFrame frame = new JFrame("Hansel Grapher");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(mainScreen.getMainPanel());
                frame.setMinimumSize(new Dimension(960, 640));
                frame.setPreferredSize(new Dimension(960, 640));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        } else {
            for (final InterviewMode mode: InterviewMode.values()) {
                InterviewCreationTestCases.createHeartFailureInterview(mode);
            }
        }
    }
}

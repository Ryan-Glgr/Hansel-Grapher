package io.github.ryan_glgr.hansel_grapher;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.*;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

import java.io.File;
import java.util.*;
// import java.awt.*;

public class Main {

    public static ArrayList<ArrayList<Node>> duplicateChain(ArrayList<ArrayList<Node>> chains) {
        ArrayList<ArrayList<Node>> dupChains = new ArrayList<>(chains.size());
        for (ArrayList<Node> chain : chains) {
            ArrayList<Node> dupChain = new ArrayList<Node>(chain.size());
            for (Node n : chain) {
                dupChain.add(new Node(n));
            }
            dupChains.add(dupChain);
        }
        return dupChains;
    }

    public static void generateChains(Integer[] kVals, int numClasses) {
        HashMap<Integer, Node> data = Node.makeNodes(kVals, numClasses);
        ArrayList<ArrayList<Node>> hanselChains;
        ArrayList<ArrayList<Node>> defaultChains = HanselChains.generateHanselChainSet(kVals, data);
        HashSet<HashSet<Node>> lowUnits = new HashSet<>();
        int[] sizes = defaultChains.stream().mapToInt(ArrayList::size).toArray();
        int[] lowValueIndices = new int[sizes.length];
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
                    var lowUnitsByClass = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
                    var adjustedLowUnitsByClass = HanselChains.removeUselessLowUnits(lowUnitsByClass);
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

        Float[] fakeWeights = new Float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        String[] fakeNames = new String[]{"","","","",""};
        Interview[] subFunctionsForEachAttribute = new Interview[kVals.length];
        InterviewMode[] modes = InterviewMode.values();
        float[] questions = new float[modes.length];
        count = 0;

        for(Set<Node> nodes: lowUnits) {
            Set<Node>[] lowUnitsToMakeTheFunctionTrue = new Set[2];
            lowUnitsToMakeTheFunctionTrue[1] = nodes;

            for(int i = 0; i < modes.length; i++) {
                Interview interview = new Interview(kVals,
                    fakeWeights,
                    modes[i],
                    numClasses,
                    fakeNames,
                    fakeNames,
                    lowUnitsToMakeTheFunctionTrue,
                    subFunctionsForEachAttribute,
                    magicFunctionMode);

                InterviewStats interviewStats = interview.interviewStats;
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

    private static void visualizeStatistics(Interview interview) {
        try{
            // ensure output directory exists
            File outputDir = new File("out/phony.txt").getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }
            // visualize our results
            VisualizationDOT.makeHanselChainDOT(interview.hanselChains, interview.adjustedLowUnitsByClass);

            // make the expansions picture
            Integer[] kValues = interview.kVals;
            VisualizationDOT.makeExpansionsDOT(interview.data, interview.adjustedLowUnitsByClass, kValues, kValues.length);

            InterviewMode interviewMode = interview.interviewStats.interviewMode;
            String interviewStatsOutputString = interviewMode + " Interview Stats";
            InterviewStatsVisualizer.savePDF(interview.interviewStats,
                "out/" + interviewStatsOutputString + ".pdf",
                    interviewMode);

            VisualizationDOT.compileDotAsync("out/HanselChains.dot");
            VisualizationDOT.compileDotAsync("out/Expansions.dot");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Interview interview = InterviewCreationTestCases.createBasicInterviewWithSubfunctions(InterviewMode.BEST_MINIMUM_CONFIRMED,
                new Integer[]{5, 6, 6, 4, 3, 5},
                new Float[]{.75f, 2.25f, 3.4f, 2.1f, 4.6f, 3.25f});

        visualizeStatistics(interview);

//        SwingUtilities.invokeLater(() -> {
//            try {
//                UIManager.setLookAndFeel(new FlatIntelliJLaf());
//                // Make Swing decorations (titlebar, borders) use the L&F
//                JFrame.setDefaultLookAndFeelDecorated(true);
//                JDialog.setDefaultLookAndFeelDecorated(true);
//            } catch (UnsupportedLookAndFeelException ex) {
//                ex.printStackTrace();
//            }
//
//            MainScreen mainScreen = new MainScreen();
//
//            JFrame frame = new JFrame("Hansel Grapher");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.setContentPane(mainScreen.getMainPanel());
//            frame.setMinimumSize(new Dimension(960, 640));
//            frame.setPreferredSize(new Dimension(960, 640));
//            frame.pack();
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });
    }
}

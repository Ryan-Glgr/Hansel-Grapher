package io.github.ryan_glgr.hansel_grapher;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.formdev.flatlaf.FlatIntelliJLaf;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.Attribute;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleCreation;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.HanselChains;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;
import io.github.ryan_glgr.hansel_grapher.Visualizations.GUI.MainScreen;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

import javax.swing.*;
// import java.awt.*;

public class Main {

    public static ArrayList<ArrayList<Node>> duplicateChain(ArrayList<ArrayList<Node>> chains) {
        ArrayList<ArrayList<Node>> dupChains = new ArrayList<>(chains.size());
        for(int i = 0; i < chains.size(); i++) {
            ArrayList<Node> dupChain = new ArrayList<Node>(chains.get(i).size());
            for(Node n: chains.get(i)) {
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
        boolean[] subFunctionsForEachAttributeEnabled = new boolean[kVals.length];
        for (int i = 0; i < kVals.length; i++) {
            subFunctionsForEachAttributeEnabled[i] = false;
        }
        Interview[] subFunctionsForEachAttribute = new Interview[kVals.length];
        InterviewMode[] modes = InterviewMode.values();
        float[] questions = new float[modes.length];
        count = 0;
        for(Set<Node> nodes: lowUnits) {
            Interview.magicLowUnits = nodes;
            for(int i = 0; i < modes.length; i++) {
                    Interview interview = new Interview(kVals,
                    fakeWeights,
                    modes[i],
                    numClasses,
                    fakeNames,
                    fakeNames,
                    subFunctionsForEachAttributeEnabled,
                    subFunctionsForEachAttribute);
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

    public static void main(String[] args) {
        Integer[] kVals = new Integer[] {2, 2, 2, 2, 2};
        int numClasses = 2;
        generateChains(kVals, numClasses);

//        makeClassifyAndSaveNodes(Interview.InterviewMode.BEST_MINIMUM_CONFIRMED,
//                new Integer[]{5, 3, 2, 4},
//                new Float[]{1.0f, 1.0f, 1.0f, 1.0f});

        // SwingUtilities.invokeLater(() -> {
        //     try {
        //         UIManager.setLookAndFeel(new FlatIntelliJLaf());
        //         // Make Swing decorations (titlebar, borders) use the L&F
        //         JFrame.setDefaultLookAndFeelDecorated(true);
        //         JDialog.setDefaultLookAndFeelDecorated(true);
        //     } catch (UnsupportedLookAndFeelException ex) {
        //         ex.printStackTrace();
        //     }

        //     MainScreen mainScreen = new MainScreen();

        //     JFrame frame = new JFrame("Hansel Grapher");
        //     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //     frame.setContentPane(mainScreen.getMainPanel());
        //     frame.setMinimumSize(new Dimension(960, 640));
        //     frame.setPreferredSize(new Dimension(960, 640));
        //     frame.pack();
        //     frame.setLocationRelativeTo(null);
        //     frame.setVisible(true);
        // });
    }

    public static void makeClassifyAndSaveNodes(InterviewMode interviewMode, Integer[] kValues, Float[] weights) {


        int maxSum = 0;
        for (int i = 0; i < kValues.length; i++) {
            maxSum += (int) ((kValues[i] - 1) * weights[i]);
        }
        int highestPossibleClassification = maxSum / kValues.length;
        int numClasses = highestPossibleClassification + 1;

        String[] attributeNames = new String[kValues.length];
        for (int i = 0; i < kValues.length; i++) {
            attributeNames[i] = "Attribute " + i;
        }

        String[] classificationNames = new String[numClasses];
        for (int i = 0; i < numClasses; i++) {
            classificationNames[i] = "Classification " + i;
        }

        boolean[] subFunctionsForEachAttributeEnabled = new boolean[kValues.length];
        for (int i = 0; i < kValues.length; i++) {
            subFunctionsForEachAttributeEnabled[i] = false;
        }

        Interview[] subFunctionsForEachAttribute = new Interview[kValues.length];

        // classify all our data
        Interview interview = new Interview(kValues,
                weights,
                interviewMode,
                numClasses,
                attributeNames,
                classificationNames,
                subFunctionsForEachAttributeEnabled,
                subFunctionsForEachAttribute);

        InterviewStats interviewStats = interview.interviewStats;
        ArrayList<ArrayList<Node>> hanselChains = interview.hanselChains;
        HashMap<Integer, Node> nodes = interview.data;

        System.out.println(interviewMode + " INTERVIEW COMPLETE!");
        System.out.println(interview);

        try{
            // ensure output directory exists
            File outputDir = new File("out/phony.txt").getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }
            // visualize our results
            VisualizationDOT.makeHanselChainDOT(hanselChains, interview.adjustedLowUnitsByClass);

            // make the expansions picture
            VisualizationDOT.makeExpansionsDOT(nodes, interview.adjustedLowUnitsByClass, kValues, kValues.length);

            String interviewStatsOutputString = interviewMode + " Interview Stats";
            InterviewStatsVisualizer.savePDF(interviewStats,
                "out/" + interviewStatsOutputString + ".pdf",
                interviewMode);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}

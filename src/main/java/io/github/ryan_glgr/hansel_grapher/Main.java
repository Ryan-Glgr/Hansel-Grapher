package io.github.ryan_glgr.hansel_grapher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.formdev.flatlaf.FlatIntelliJLaf;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.Attribute;
import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;
import io.github.ryan_glgr.hansel_grapher.Visualizations.GUI.MainScreen;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

import javax.swing.*;
import java.awt.*;

public class Main {

    // Calculate the highest possible classification at compile time

    public static void main(String[] args) {

//        makeClassifyAndSaveNodes(Interview.InterviewMode.BEST_MINIMUM_CONFIRMED,
//                new Integer[]{5, 3, 2, 4},
//                new Float[]{1.0f, 1.0f, 1.0f, 1.0f});

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
                // Make Swing decorations (titlebar, borders) use the L&F
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
            } catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }

            MainScreen mainScreen = new MainScreen();

            JFrame frame = new JFrame("Hansel Grapher");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(mainScreen.getMainPanel());
            frame.setMinimumSize(new Dimension(960, 640));
            frame.setPreferredSize(new Dimension(960, 640));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
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

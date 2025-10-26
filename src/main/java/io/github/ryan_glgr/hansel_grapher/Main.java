package io.github.ryan_glgr.hansel_grapher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;
import io.github.ryan_glgr.hansel_grapher.Visualizations.GUI.MainScreen;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

import javax.swing.*;
import java.awt.Dimension;

public class Main {

    public static Integer[] kValues = {3, 4, 5, 2, 4};
    public static Float[] weights = {2.25f, 1.0f, 0.75f, 2.65f, .80f}; // will be used when we are just doing a magic linear function interview for testing.
    static {
        int maxSum = 0;
        for (int i = 0; i < kValues.length; i++) {
            maxSum += (int)((kValues[i] - 1) * weights[i]);
        }
        highestPossibleClassification = maxSum / kValues.length;
        Node.dimension = kValues.length; // TODO: Node.dimension shouldn't be set here.
    }
    // Calculate the highest possible classification at compile time
    public static Integer highestPossibleClassification;
    
    public static void main(String[] args) {

//        for (Interview.InterviewMode mode : Interview.InterviewMode.values()){
//            makeClassifyAndSaveNodes(mode);
//        }
//        makeClassifyAndSaveNodes(Interview.InterviewMode.BEST_MINIMUM_CONFIRMED);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                // Make Swing decorations (titlebar, borders) use the L&F
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
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

    public static void makeClassifyAndSaveNodes(Interview.InterviewMode interviewMode) {

        int numClasses = highestPossibleClassification + 1;

        // classify all our data
        Interview interview = new Interview(kValues, weights, interviewMode, numClasses, null, null);
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
            VisualizationDOT.makeExpansionsDOT(nodes, interview.adjustedLowUnitsByClass);

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

package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI.MainScreen;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.HanselChains;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MainScreen {
    private JPanel mainPanel;
    private JPanel buttonPanel;
    private JButton newInterviewButton;
    private JButton hanselChainsOrExpansionsButton;
    private JButton viewRuleTreesButton;
    private JButton viewInterviewStatisticsButton;
    private JButton exportChainsExpansionsButton;
    private JButton exportInterviewStatisticsGraphsButton;
    private JPanel viewButtonsPanel;
    private JPanel exportButtonsPanel;
    private JPanel mainDisplayPanel;

    Interview interview;
    HashMap<Integer, Node> nodes;
    ArrayList<ArrayList<Node>> hanselChains;



    public MainScreen(){

        setUpButtons();


    }

    private final static String hanselChainsView = "HANSEL CHAINS VIEWING";
    private final static String expansionsView = "EXPANSIONS VIEWING";

    public void setUpButtons(){

        // TODO: add the rest of the logic needed when we toggle the view.
        hanselChainsOrExpansionsButton.addActionListener(e -> {
            if (hanselChainsOrExpansionsButton.getText().equals(hanselChainsView)){
                hanselChainsOrExpansionsButton.setText(expansionsView);
            }
            else{
                hanselChainsOrExpansionsButton.setText(hanselChainsView);
            }
        });

        // TODO: create pop up window for when we want to make a new interview.
        //       needs to allow us to specify k values, and weights (optionally)
        newInterviewButton.addActionListener(e -> {
            // needs to somehow get k values and number of classes. then call our two methods. Then run the interview by calling constructor like we do in Main.
//            Integer[] kValues = getKvals();
//            int numClasses = getNumClasses();
//            SwingUtilities.invokeLater(() -> {
//                nodes = Node.makeNodes(kValues, numClasses);
//                hanselChains = HanselChains.generateHanselChainSet(kValues, nodes);
//            });
        });

        // TODO: new window when we want to look at the interview stats as well.
        //       this will come up and show our graph, then it will have buttons to toggle
        //       each PermeationStat on or off.
        viewInterviewStatisticsButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {

            });
        });

        // TODO: make this one visualize like a tree. can be done in main panel.
        viewRuleTreesButton.addActionListener(e -> {
               SwingUtilities.invokeLater(() -> {

                   // create the XChart panel
                   XChartPanel<XYChart> chartPanel = InterviewStatsVisualizer.getChartPanel(interview.interviewStats, interview.interviewStats.interviewMode);

                   // remove everything currently on the panel
                   mainDisplayPanel.removeAll();

                   // add chart, center it, let it resize with the panel
                   mainDisplayPanel.add(chartPanel, BorderLayout.CENTER);
                   mainDisplayPanel.revalidate();
                   mainDisplayPanel.repaint();
               });
        });

        // TODO: make this export pdf's of the chains and expansions.
        exportChainsExpansionsButton.addActionListener(e -> {

        });

        // TODO: make this export our pdf's of the interview stats. just call InterviewStatsVisualizer.savePDF
        exportInterviewStatisticsGraphsButton.addActionListener(e -> {

        });

    }







}

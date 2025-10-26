package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import javax.swing.plaf.PanelUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    public MainScreen(){

        setUpButtons();

    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private final static String hanselChainsView = "HANSEL CHAINS VIEWING";
    private final static String expansionsView = "EXPANSIONS VIEWING";

    public void setUpButtons(){

        hanselChainsOrExpansionsButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                if (hanselChainsOrExpansionsButton.getText().equals(hanselChainsView)){
                    hanselChainsOrExpansionsButton.setText(expansionsView);

                    mainDisplayPanel.setUI(new PanelUI() {
                    }/* TODO: add the hansel chain vis */);
                }
                else{
                    hanselChainsOrExpansionsButton.setText(hanselChainsView);
                    mainDisplayPanel.setUI(new PanelUI() {
                    }/* TODO: add the expansion vis */);
                }
            });
        });

        // TODO: create pop up window for when we want to make a new interview.
        //       needs to allow us to specify k values, and weights (optionally)
        newInterviewButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                CreateFunctionWindow functionWindow = new CreateFunctionWindow();
                CompletableFuture<Interview> interviewFuture = functionWindow.createFunctionAndReturnInterviewObject();

                interviewFuture.thenAccept(createdInterview -> {
                    if (createdInterview == null) {
                        return;
                    }
                    SwingUtilities.invokeLater(() -> {
                        interview = createdInterview;
                        JOptionPane.showMessageDialog(mainPanel,
                                "Interview ran successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                }).exceptionally(ex -> {
                    Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainPanel,
                            "Failed to create interview: " + (cause != null ? cause.getMessage() : ex),
                            "Error",
                            JOptionPane.ERROR_MESSAGE));

                    ex.printStackTrace();
                    return null;
                });
            });
        });

        // TODO: new window when we want to look at the interview stats as well.
        //       this will come up and show our graph, then it will have buttons to toggle
        //       each PermeationStat on or off.
        viewInterviewStatisticsButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {

            });
        });

        // TODO: make this one actually visualize like a tree. can be done in main panel.
        //       probably easiest to do however we do the expansions and chains.
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

        exportChainsExpansionsButton.addActionListener(e -> {
                SwingUtilities.invokeLater(() ->
                {
                    try {
                        VisualizationDOT.makeExpansionsDOT(interview.data, interview.adjustedLowUnitsByClass);
                        VisualizationDOT.makeHanselChainDOT(interview.hanselChains, interview.adjustedLowUnitsByClass);
                        compileDotAsync("out/Expansions.dot");
                        compileDotAsync("out/HanselChains.dot");
                    } catch (IOException ex) {
                        System.out.println("Making expansion file failed!");
                        ex.printStackTrace();
                    }
                });
        });

        exportInterviewStatisticsGraphsButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                InterviewStats interviewStats = interview.interviewStats;
                Interview.InterviewMode interviewMode = interviewStats.interviewMode;
                String interviewStatsOutputString = interviewMode + " InterviewStats.pdf";
                try {
                    InterviewStatsVisualizer.savePDF(interviewStats, "out/" + interviewStatsOutputString, interviewStats.interviewMode);
                }
                catch (IOException ioException){
                    System.out.println("Saving interview stats failed!");
                    ioException.printStackTrace();
                }
            });
        });

    }

    private void compileDotAsync(String dotPath) {
        CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("./Visualizations/compile_dot.sh", dotPath);
                pb.directory(new File("."));
                Process process = pb.start();
                process.onExit().thenAccept(p -> {
                    if (p.exitValue() != 0) {
                        System.err.println("compile_dot.sh exited with code " + p.exitValue() + " for " + dotPath);
                    }
                });
            } catch (IOException ex) {
                System.err.println("Failed to compile DOT file: " + dotPath);
                ex.printStackTrace();
            }
        });
    }

}

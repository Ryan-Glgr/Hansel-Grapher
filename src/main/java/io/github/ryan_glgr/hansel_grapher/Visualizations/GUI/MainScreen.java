package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

import javax.swing.*;
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

    private Interview interview;


    public MainScreen(){
        setUpButtons();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private final static String hanselChainsView = "HANSEL CHAINS VIEWING";
    private final static String expansionsView = "EXPANSIONS VIEWING";
    // file paths the helper produces (now using SVG)
    private final String hanselSvgPath = "./out/HanselChains.svg";
    private final String expansionsSvgPath = "./out/Expansions.svg";

    public void setUpButtons() {
        setupNewInterviewButton();
    }

    private void setupNewInterviewButton() {
        newInterviewButton.addActionListener(e -> SwingUtilities.invokeLater(this::handleNewInterview));
    }

    private void handleNewInterview() {
        final CreateFunctionWindow functionWindow = new CreateFunctionWindow();
        final CompletableFuture<Interview> interviewFuture = functionWindow.createFunctionAndReturnInterviewObject("Create Interview");

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
            final Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainPanel,
                    "Failed to create interview: " + (cause != null ? cause.getMessage() : ex),
                    "Error",
                    JOptionPane.ERROR_MESSAGE));
            ex.printStackTrace();
            return null;
        });

        SwingUtilities.invokeLater(this::exportChainsAndExpansions);
        SwingUtilities.invokeLater(this::exportInterviewStatistics);
        openInBrowser();

    }

    private void exportChainsAndExpansions() {
        try {
            VisualizationDOT.makeExpansionsDOT(interview.data,
                    interview.adjustedLowUnitsByClass,
                    interview.interviewStats.kValues);
            VisualizationDOT.makeHanselChainDOT(interview.hanselChains, interview.adjustedLowUnitsByClass);
            VisualizationDOT.compileDotAsync("out/Expansions.dot");
            VisualizationDOT.compileDotAsync("out/HanselChains.dot");
        } catch (final IOException ex) {
            System.out.println("Making expansion file failed!");
            ex.printStackTrace();
        }
    }

    private void exportInterviewStatistics() {
        final InterviewStats interviewStats = interview.interviewStats;
        final InterviewMode interviewMode = interviewStats.interviewMode;
        final String interviewStatsOutputString = interviewMode + " InterviewStats.pdf";
        try {
            InterviewStatsVisualizer.savePDF(interviewStats,
                    "out/" + interviewStatsOutputString,
                    interviewStats.interviewMode);
        } catch (final IOException ioException) {
            System.out.println("Saving interview stats failed!");
            ioException.printStackTrace();
        }
    }

    private void openInBrowser() {
        // TODO: create method which invokes the browser to open the files we have generated.
        //       the files would be created by this point. we just need to somehow invoke the browser to render them.
    }

}

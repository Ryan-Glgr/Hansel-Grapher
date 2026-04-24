package io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens;

import io.github.ryan_glgr.hansel_grapher.stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.visualizations.VisualizationDOT;

import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL2;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.BlankRenderer;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.ExpansionRenderer;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.HanselChainRenderer;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.RuleTreeRenderer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class MainWindow {
    private final JPanel mainPanel;
    private final JPanel buttonPanel;
    private final JButton newInterviewButton;

    private static final String HANSEL_CHAIN_VIEW = "HANSEL_CHAIN_VIEW";
    private static final String EXPANSION_VIEW = "EXPANSION_VIEW";
    private static final String RULE_TREE_VIEW = "RULE_TREE_VIEW";
    private static final String NO_INTERVIEW_YET = "NO_INTERVIEW_YET";

    private final GLJPanel glPanel;
    private GLEventListener currentListener;
    private String currentView;

    private final JButton hanselChainsButton;
    private final JButton expansionsButton;
    private final JButton ruleTreesButton;

    private final JButton exportVisualizationsButton;
    private final JButton exportInterviewStatisticsGraphsButton;

    private Interview interview;


    public MainWindow() {
        currentView = NO_INTERVIEW_YET;
        currentListener = new BlankRenderer();

        // --- OpenGL setup ---
        final GLProfile profile = GLProfile.get(GLProfile.GL2);
        final GLCapabilities caps = new GLCapabilities(profile);
        glPanel = new GLJPanel(caps);
        glPanel.addGLEventListener(currentListener);

        // --- Buttons ---
        newInterviewButton = new JButton("Conduct New Interview");
        newInterviewButton.addActionListener(e ->
                SwingUtilities.invokeLater(this::handleNewInterview));

        hanselChainsButton = new JButton("View Hansel Chains.");
        hanselChainsButton.addActionListener(e ->
                SwingUtilities.invokeLater(() -> handleVisualizationChange(HANSEL_CHAIN_VIEW)));

        expansionsButton = new JButton("View Node Expansions.");
        expansionsButton.addActionListener(e ->
                SwingUtilities.invokeLater(() -> handleVisualizationChange(EXPANSION_VIEW)));

        ruleTreesButton = new JButton("View Rule Trees.");
        ruleTreesButton.addActionListener(e ->
                SwingUtilities.invokeLater(() -> handleVisualizationChange(RULE_TREE_VIEW)));

        exportVisualizationsButton = new JButton("Save Visualizations");
        exportVisualizationsButton.addActionListener(e ->
                SwingUtilities.invokeLater(this::exportChainsAndExpansions));

        exportInterviewStatisticsGraphsButton = new JButton("Save Interview Statistics Graphs");
        exportInterviewStatisticsGraphsButton.addActionListener(e ->
                SwingUtilities.invokeLater(this::exportInterviewStatistics));



        buttonPanel = new JPanel();
        buttonPanel.add(newInterviewButton);
        buttonPanel.add(hanselChainsButton);
        buttonPanel.add(expansionsButton);
        buttonPanel.add(ruleTreesButton);
        buttonPanel.add(exportVisualizationsButton);
        buttonPanel.add(exportInterviewStatisticsGraphsButton);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(glPanel, BorderLayout.CENTER);

    }

    public JPanel getMainPanel() {
        return mainPanel;
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

    private void handleVisualizationChange(final String viewToApply) {
        currentView = viewToApply;

        final GLEventListener next;
        switch (currentView) {
            case HANSEL_CHAIN_VIEW:
                next = new HanselChainRenderer();
                break;
            case EXPANSION_VIEW:
                next = new ExpansionRenderer();
                break;
            case RULE_TREE_VIEW:
                next = new RuleTreeRenderer();
                break;
            case NO_INTERVIEW_YET:
            default:
                next = new BlankRenderer();
                break;
        }

        glPanel.removeGLEventListener(currentListener);
        glPanel.addGLEventListener(next);
        currentListener = next;
        glPanel.repaint();
    }

}

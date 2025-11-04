package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsDisplay;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;
import org.icepdf.ri.common.MyAnnotationCallback;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.common.views.DocumentViewControllerImpl;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;
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

    private Interview interview;

    // track the current ICEpdf controller so we can close documents and free resources
    private SwingController currentPdfController = null;
    private JPanel currentPdfViewerPanel = null;


    public MainScreen(){

        setUpButtons();

    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private final static String hanselChainsView = "HANSEL CHAINS VIEWING";
    private final static String expansionsView = "EXPANSIONS VIEWING";
    // file paths the helper produces
    private final String hanselPdfPath = "out/HanselChains.pdf";
    private final String expansionsPdfPath = "out/Expansions.pdf";

    public void setUpButtons() {
        setupHanselChainsButton();
        setupNewInterviewButton();
        setupViewInterviewStatsButton();
        setupViewRuleTreesButton();
        setupExportChainsButton();
        setupExportStatsButton();
    }

    private void setupHanselChainsButton() {
        hanselChainsOrExpansionsButton.addActionListener(e -> {
            // toggle text immediately on EDT for snappy feedback
            SwingUtilities.invokeLater(() -> {
                boolean switchingToExpansions = hanselChainsOrExpansionsButton.getText().equals(hanselChainsView);
                if (switchingToExpansions) {
                    hanselChainsOrExpansionsButton.setText(expansionsView);
                    loadPdfIntoPanelAsync(expansionsPdfPath);
                } else {
                    hanselChainsOrExpansionsButton.setText(hanselChainsView);
                    loadPdfIntoPanelAsync(hanselPdfPath);
                }
            });
        });
    }

    private void setupNewInterviewButton() {
        newInterviewButton.addActionListener(e -> SwingUtilities.invokeLater(this::handleNewInterview));
    }

    private void handleNewInterview() {
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
    }

    private void setupViewInterviewStatsButton() {
        viewInterviewStatisticsButton.addActionListener(e -> showInterviewStatistics());
    }

    private void showInterviewStatistics() {
        if (interview != null && interview.interviewStats != null) {
            // Display the statistics in a new window with toggle buttons
            InterviewStatsDisplay.display(
                interview.interviewStats,
                interview.interviewStats.interviewMode
            );
        } else {
            JOptionPane.showMessageDialog(
                mainPanel,
                "No interview data available. Please run an interview first.",
                "No Data",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void setupViewRuleTreesButton() {
        viewRuleTreesButton.addActionListener(e -> SwingUtilities.invokeLater(this::showRuleTrees));
    }

    private void showRuleTrees() {
        // TODO: make this one actually visualize like a tree. can be done in main panel.
        //       probably easiest to do however we do the expansions and chains.
        // create the XChart panel
        XChartPanel<XYChart> chartPanel = InterviewStatsVisualizer.getChartPanel(
            interview.interviewStats, 
            interview.interviewStats.interviewMode
        );

        // remove everything currently on the panel
        mainDisplayPanel.removeAll();

        // Wrap chart in a panel with proper constraints
        JPanel chartContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        chartContainer.add(chartPanel, gbc);

        // add chart container to main display panel
        mainDisplayPanel.setLayout(new BorderLayout());
        mainDisplayPanel.add(chartContainer, BorderLayout.CENTER);

        mainDisplayPanel.revalidate();
        mainDisplayPanel.repaint();
    }

    private void setupExportChainsButton() {
        exportChainsExpansionsButton.addActionListener(e -> SwingUtilities.invokeLater(this::exportChainsAndExpansions));
    }

    private void exportChainsAndExpansions() {
        try {
            VisualizationDOT.makeExpansionsDOT(interview.data, 
                interview.adjustedLowUnitsByClass, 
                interview.interviewStats.kValues, 
                interview.interviewStats.kValues.length);
            VisualizationDOT.makeHanselChainDOT(interview.hanselChains, interview.adjustedLowUnitsByClass);
            compileDotAsync("out/Expansions.dot");
            compileDotAsync("out/HanselChains.dot");
        } catch (IOException ex) {
            System.out.println("Making expansion file failed!");
            ex.printStackTrace();
        }
    }

    private void setupExportStatsButton() {
        exportInterviewStatisticsGraphsButton.addActionListener(e -> SwingUtilities.invokeLater(this::exportInterviewStatistics));
    }

    private void exportInterviewStatistics() {
        InterviewStats interviewStats = interview.interviewStats;
        Interview.InterviewMode interviewMode = interviewStats.interviewMode;
        String interviewStatsOutputString = interviewMode + " InterviewStats.pdf";
        try {
            InterviewStatsVisualizer.savePDF(interviewStats, 
                "out/" + interviewStatsOutputString, 
                interviewStats.interviewMode);
        } catch (IOException ioException) {
            System.out.println("Saving interview stats failed!");
            ioException.printStackTrace();
        }
    }

    /**
     * Load a PDF in the background and put the viewer into mainDisplayPanel on the EDT.
     * This method disposes the previous controller/viewer safely before swapping.
     */
    private void loadPdfIntoPanelAsync(String pdfPath) {
        // If file missing, you can detect and report here
        File f = new File(pdfPath);
        if (!f.exists()) {
            // Show placeholder on EDT
            SwingUtilities.invokeLater(() -> {
                showPlaceholder("PDF not found: " + pdfPath);
            });
            return;
        }

        // Show loading message
        SwingUtilities.invokeLater(() -> {
            showPlaceholder("Loading PDF...");
        });

        // Background loading + parsing (avoid blocking EDT)
        CompletableFuture.supplyAsync(() -> {
            try {
                SwingController controller = new SwingController();
                // Configure controller for better performance with large PDFs
                controller.getDocumentViewController()
                        .setAnnotationCallback(new MyAnnotationCallback(new DocumentViewControllerImpl(controller)));

                // Build the viewer panel with the controller
                SwingViewBuilder factory = new SwingViewBuilder(controller);
                JPanel viewerPanel = factory.buildViewerPanel();
                
                // Configure the viewer panel for better performance
                viewerPanel.setDoubleBuffered(true);
                
                // Open the document
                controller.openDocument(pdfPath);
                
                // Set the document view type (show single page at a time)
                controller.setPageViewMode(controller.getDocumentViewController()
                        .getDocumentViewModel().getViewCurrentPageIndex(), false);
                
                // Set the document to fit the window
                controller.setPageFitMode(controller.getDocumentViewController().getDocumentViewModel().getPageBoundary(), true);
                
                return new Object[] { controller, viewerPanel };
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load PDF: " + e.getMessage(), e);
            }
        }).thenAccept(arr -> {
            if (arr == null) return;
            
            SwingController newController = (SwingController) arr[0];
            JPanel newViewerPanel = (JPanel) arr[1];

            // Swap on EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    // clean up previous controller/viewer (if any)
                    disposeCurrentPdfViewer();

                    // install the new viewer into mainDisplayPanel
                    mainDisplayPanel.removeAll();
                    
                    // Create a scroll pane for the PDF viewer with no border
                    JScrollPane scrollPane = new JScrollPane(newViewerPanel);
                    scrollPane.setBorder(null);
                    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                    
                    // Add the scroll pane to the main display panel
                    mainDisplayPanel.setLayout(new BorderLayout());
                    mainDisplayPanel.add(scrollPane, BorderLayout.CENTER);
                    
                    // Force the PDF to fit the available space
                    newController.setPageFitMode(newController.getDocumentViewController().getFitMode(), true);
                    
                    mainDisplayPanel.revalidate();
                    mainDisplayPanel.repaint();

                    // store references for later cleanup
                    currentPdfController = newController;
                    currentPdfViewerPanel = newViewerPanel;
                } catch (Exception e) {
                    e.printStackTrace();
                    showPlaceholder("Error displaying PDF: " + e.getMessage());
                }
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> 
                showPlaceholder("Failed to load PDF: " + 
                    (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()))
            );
            return null;
        });
    }

    private void showPlaceholder(String message) {
        mainDisplayPanel.removeAll();
        mainDisplayPanel.setLayout(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        mainDisplayPanel.add(label, BorderLayout.CENTER);
        mainDisplayPanel.revalidate();
        mainDisplayPanel.repaint();
    }

    private void disposeCurrentPdfViewer() {
        if (currentPdfController != null) {
            try {
                // closeDocument frees resources (streams, caches)
                currentPdfController.closeDocument();
            } catch (Exception ignored) {}
            currentPdfController = null;
        }
        if (currentPdfViewerPanel != null) {
            // remove the panel and let GC collect UI components
            mainDisplayPanel.remove(currentPdfViewerPanel);
            currentPdfViewerPanel = null;
        }
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

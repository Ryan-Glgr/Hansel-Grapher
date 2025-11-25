package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsDisplay;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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

    // track the current SVG viewer panel so we can remove and free references
    private JPanel currentSvgViewerPanel = null;

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
                    loadSvgIntoPanelAsync(expansionsSvgPath);
                } else {
                    hanselChainsOrExpansionsButton.setText(hanselChainsView);
                    loadSvgIntoPanelAsync(hanselSvgPath);
                }
            });
        });
    }

    private void setupNewInterviewButton() {
        newInterviewButton.addActionListener(e -> SwingUtilities.invokeLater(this::handleNewInterview));
    }

    private void handleNewInterview() {
        CreateFunctionWindow functionWindow = new CreateFunctionWindow();
        CompletableFuture<Interview> interviewFuture = functionWindow.createFunctionAndReturnInterviewObject("Create Interview");

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
            VisualizationDOT.compileDotAsync("out/Expansions.dot");
            VisualizationDOT.compileDotAsync("out/HanselChains.dot");
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
        InterviewMode interviewMode = interviewStats.interviewMode;
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
     * Load an SVG in the background and put the viewer into mainDisplayPanel on the EDT.
     * This method disposes the previous viewer safely before swapping.
     */
    private void loadSvgIntoPanelAsync(String svgPath) {
        File f = new File(svgPath);
        if (!f.exists()) {
            SwingUtilities.invokeLater(() -> showPlaceholder("SVG not found: " + svgPath));
            return;
        }

        // Show loading message immediately
        SwingUtilities.invokeLater(() -> showPlaceholder("Loading SVG..."));

        // Parse and prepare the viewer off the EDT, then swap in the panel on the EDT
        CompletableFuture.supplyAsync(() -> {
            try {
                // parse SVG and build an SvgViewerPanel (parsing may be moderately heavy)
                SvgViewerPanel panel = new SvgViewerPanel(f);
                return panel;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load SVG: " + e.getMessage(), e);
            }
        }).thenAccept(panel -> {
            if (panel == null) return;
            SwingUtilities.invokeLater(() -> {
                try {
                    disposeCurrentSvgViewer();

                    mainDisplayPanel.removeAll();

                    JScrollPane scrollPane = new JScrollPane(panel);
                    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setBorder(null);
                    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                    mainDisplayPanel.setLayout(new BorderLayout());
                    mainDisplayPanel.add(scrollPane, BorderLayout.CENTER);

                    mainDisplayPanel.setLayout(new BorderLayout());
                    mainDisplayPanel.add(scrollPane, BorderLayout.CENTER);

                    mainDisplayPanel.revalidate();
                    mainDisplayPanel.repaint();

                    currentSvgViewerPanel = panel;
                } catch (Exception e) {
                    e.printStackTrace();
                    showPlaceholder("Error displaying SVG: " + e.getMessage());
                }
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    showPlaceholder("Failed to load SVG: " +
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

    private void disposeCurrentSvgViewer() {
        if (currentSvgViewerPanel != null) {
            try {
                mainDisplayPanel.remove(currentSvgViewerPanel);
            } catch (Exception ignored) {}
            currentSvgViewerPanel = null;
        }
    }

    /**
     * Small, self-contained SVG viewer panel using svgSalamander.
     * - Parses SVG once in the constructor (called off-EDT above).
     * - Caches a raster for the current zoom/size/offset and re-renders only on view changes.
     * - Simple mouse wheel zoom and drag-to-pan.
     */
    private static class SvgViewerPanel extends JPanel {
        private final SVGUniverse universe = new SVGUniverse();
        private final SVGDiagram diagram;

        // view state
        private double zoom = 1.0;
        private double offsetX = 0;
        private double offsetY = 0;

        private Point lastDragPoint;

        // simple cache
        private BufferedImage cacheImage;
        private double cacheZoom = Double.NaN;
        private Dimension cacheSize = new Dimension();

        public SvgViewerPanel(File svgFile) throws Exception {
            // Parsing the SVG can be moderately heavy, so caller should call this off the EDT.
            URI uri = universe.loadSVG(svgFile.toURI().toURL());
            diagram = universe.getDiagram(uri);

            // Set preferred size to something reasonable; actual panel will be in a scroll pane
            Rectangle view = diagram != null ? diagram.getDeviceViewport() : null;
            if (view != null) {
                setPreferredSize(new Dimension(Math.max(800, view.width), Math.max(600, view.height)));
            } else {
                setPreferredSize(new Dimension(800, 600));
            }

            // Enable key focus for fit-to-panel shortcut
            setFocusable(true);
            requestFocusInWindow();

            // ---- MOUSE WHEEL ZOOM ----
            addMouseWheelListener(e -> {
                double delta = 0.1 * e.getPreciseWheelRotation();
                double oldZoom = zoom;
                zoom *= (1 - delta);
                zoom = Math.max(0.01, Math.min(zoom, 100)); // clamp zoom

                // Keep mouse position stable while zooming
                double mx = e.getX();
                double my = e.getY();
                offsetX = mx - (mx - offsetX) * (zoom / oldZoom);
                offsetY = my - (my - offsetY) * (zoom / oldZoom);

                invalidateCacheAndRepaint();
            });

            // ---- CLICK-AND-DRAG PAN ----
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastDragPoint = e.getPoint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    Point p = e.getPoint();
                    offsetX += p.x - lastDragPoint.x;
                    offsetY += p.y - lastDragPoint.y;
                    lastDragPoint = p;
                    invalidateCacheAndRepaint();
                }
            });

            // ---- FIT-TO-PANEL KEY SHORTCUT ----
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_F) {
                        fitToPanel();
                    }
                }
            });
        }

        private void invalidateCacheAndRepaint() {
            cacheImage = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (diagram == null) return;
            Graphics2D g = (Graphics2D) g0.create();

            int w = getWidth(), h = getHeight();

            // Recreate cached raster if zoom/size changed
            if (cacheImage == null || Double.isNaN(cacheZoom) || cacheZoom != zoom
                    || cacheSize.width != w || cacheSize.height != h) {

                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D cg = img.createGraphics();
                cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                cg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // clear background
                cg.setColor(getBackground());
                cg.fillRect(0, 0, w, h);

                // apply transform: translate (offsets) then scale
                AffineTransform at = new AffineTransform();
                at.translate(offsetX, offsetY);
                at.scale(zoom, zoom);
                cg.transform(at);

                try {
                    diagram.render(cg);
                } catch (SVGException ex) {
                    ex.printStackTrace();
                } finally {
                    cg.dispose();
                }

                cacheImage = img;
                cacheZoom = zoom;
                cacheSize.setSize(w, h);
            }

            // paint cached raster
            g.drawImage(cacheImage, 0, 0, null);
            g.dispose();
        }

        /**
         * Fit the entire SVG to the panel, centering it and adjusting zoom.
         */
        public void fitToPanel() {
            Rectangle view = diagram != null ? diagram.getDeviceViewport() : null;
            if (view == null) return;

            double zx = getWidth() / (double) view.width;
            double zy = getHeight() / (double) view.height;
            zoom = Math.min(zx, zy);

            offsetX = (getWidth() - view.width * zoom) / 2.0;
            offsetY = (getHeight() - view.height * zoom) / 2.0;
            invalidateCacheAndRepaint();
        }
    }

}

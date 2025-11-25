package io.github.ryan_glgr.hansel_grapher.Visualizations;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XChartPanel;
import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats.PermeationStatistic;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterviewStatsDisplay extends JFrame {
    private final XChartPanel<XYChart> chartPanel;
    private final Map<PermeationStatistic, JToggleButton> toggleButtons;
    private final InterviewStats stats;
    private final InterviewMode interviewMode;

    public InterviewStatsDisplay(InterviewStats stats, InterviewMode interviewMode) {
        this.stats = stats;
        this.interviewMode = interviewMode;
        this.toggleButtons = new HashMap<>();
        
        setTitle("Interview Statistics");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create chart panel
        chartPanel = InterviewStatsVisualizer.getChartPanel(stats, interviewMode);
        add(chartPanel, BorderLayout.CENTER);
        
        // Create toggle buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (PermeationStatistic stat : PermeationStatistic.values()) {
            JToggleButton button = new JToggleButton(stat.toString(), true);
            button.addActionListener(e -> toggleSeries(stat, button.isSelected()));
            toggleButtons.put(stat, button);
            buttonPanel.add(button);
        }
        
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }
    
    private void toggleSeries(PermeationStatistic stat, boolean show) {
        XYChart chart = chartPanel.getChart();
        String seriesName = stat.toString();
        
        if (show) {
            // Show the series by re-adding it
            List<Integer> yData = InterviewStatsVisualizer.permeationStatLists.get(seriesName);
            if (yData != null) {
                chart.addSeries(seriesName, InterviewStatsVisualizer.xData, yData);
            }
        } else {
            // Hide the series by removing it
            chart.removeSeries(seriesName);
        }
        
        chartPanel.revalidate();
        chartPanel.repaint();
    }
    
    public static void display(InterviewStats stats, InterviewMode interviewMode) {
        SwingUtilities.invokeLater(() -> {
            InterviewStatsDisplay display = new InterviewStatsDisplay(stats, interviewMode);
            display.setVisible(true);
        });
    }
}

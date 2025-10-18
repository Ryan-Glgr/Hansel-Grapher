package io.github.ryan_glgr.hansel_grapher.Visualizations;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats.PermeationStatistic;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;
import java.io.IOException;

import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat;

/**
 * Static utility for visualizing InterviewStats.
 * Produces JPanel for GUI embedding or saves chart as PNG/PDF/SVG.
 */
public class InterviewStatsVisualizer {

    /**
     * Returns a JPanel displaying a chart of a specific PermeationStats field
     * @param stats InterviewStats object
     * @param fieldName Name of the field (used for title)
     * @param fieldExtractor Lambda extracting Integer from PermeationStats
     */
    public static JPanel getChartPanel(
            InterviewStats stats,
            List<PermeationStatistic> fieldsToGraph,
            InterviewMode interviewMode) {

        XYChart chart = buildChart(stats, fieldsToGraph, interviewMode);
        return new XChartPanel<>(chart);
    }

    /**
     * Save chart as PNG file
     */
    public static void savePNG(
            InterviewStats stats,
            List<PermeationStatistic> fieldsToGraph,
            String filePath,
            InterviewMode interviewMode) throws IOException {

        XYChart chart = buildChart(stats, fieldsToGraph, interviewMode);
        BitmapEncoder.saveBitmap(chart, filePath, BitmapFormat.PNG);
    }

    /**
     * Save chart as PDF file (vector, high-quality for print)
     */
    public static void savePDF(
            InterviewStats stats,
            List<PermeationStatistic> fieldsToGraph,
            String filePath,
            InterviewMode interviewMode) throws IOException {

        XYChart chart = buildChart(stats, fieldsToGraph, interviewMode);
        VectorGraphicsEncoder.saveVectorGraphic(chart, filePath, VectorGraphicsFormat.PDF);
    }

    /**
     * Private helper: builds XYChart from InterviewStats and fieldExtractor
     */
    private static XYChart buildChart(
            InterviewStats stats,
            List<PermeationStatistic> fieldsToGraph,
            InterviewMode interviewMode) {

        List<PermeationStats> questions = stats.permeationStatsForEachNodeAsked;
        PermeationStats[] permeationStats = questions.toArray(new PermeationStats[0]);

        List<Integer> xData = new ArrayList<>();
        for (int i = 0; i < permeationStats.length; i++) {
            xData.add(i + 1); // Question index
        }

        String title = "Permeation stats through " + interviewMode.toString() + " Interview";
        XYChart chart = new XYChartBuilder()
                .width(900)
                .height(540)
                .title(title)
                .xAxisTitle("Question #")
                .yAxisTitle("Value")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(6);
        chart.getStyler().setXAxisTickMarkSpacingHint(50);

        for (PermeationStatistic field : fieldsToGraph) {
            
            // make a new list, and all all the questions' stats for that field.
            List<Integer> yData = new ArrayList<>();
            for (int i = 0; i < permeationStats.length; i++) {
                yData.add(switch(field) {
                    case NUMBER_OF_CONFIRMATIONS -> permeationStats[i].numberOfConfirmations;
                    case NUMBER_OF_NODES_TOUCHED_ABOVE -> permeationStats[i].numberOfNodesTouchedAbove;
                    case NUMBER_OF_NODES_TOUCHED_BELOW -> permeationStats[i].numberOfNodesTouchedBelow;
                    case TOTAL_NUMBER_OF_NODES_TOUCHED -> permeationStats[i].totalNumberOfNodesTouched;  
                });
            }
            chart.addSeries(field.toString(), xData, yData);   

        }
        return chart;
    }

}


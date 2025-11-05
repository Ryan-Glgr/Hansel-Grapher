package io.github.ryan_glgr.hansel_grapher.Visualizations;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats.PermeationStatistic;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.InterviewMode;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.util.*;
import java.io.IOException;

import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat;

/**
 * Static utility for visualizing InterviewStats.
 * Produces JPanel for GUI embedding or saves chart as PNG/PDF/SVG.
 */
public class InterviewStatsVisualizer {

    public static XChartPanel<XYChart> getChartPanel(
            InterviewStats stats,
            InterviewMode interviewMode) {
        return new XChartPanel<>(buildChart(stats, interviewMode));
    }

    /**
     * Save chart as PDF file (vector, high-quality for print)
     */
    public static void savePDF(
            InterviewStats stats,
            String filePath,
            InterviewMode interviewMode) throws IOException {

        XYChart chart = buildChart(stats, interviewMode);
        VectorGraphicsEncoder.saveVectorGraphic(chart, filePath, VectorGraphicsFormat.PDF);
    }

    // name of the field is the key. the list of values is the list.
    public static HashMap<String, List<Integer>> permeationStatLists;
    public static List<Integer> xData;
    /**
     * Private helper: builds XYChart from InterviewStats and fieldExtractor
     */
    private static XYChart buildChart(
            InterviewStats stats,
            InterviewMode interviewMode) {

        List<PermeationStats> questions = stats.permeationStatsForEachNodeAsked;
        PermeationStats[] permeationStats = questions.toArray(new PermeationStats[0]);

        // re initialize these each time we make a new chart. they need to be fields so that we can use them in the toggle, when
        // we have the same chart, just wanting to add or remove a series.
        xData = new ArrayList<>();
        permeationStatLists = new HashMap<>();

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

        for (PermeationStatistic field : PermeationStatistic.values()) {
            
            // make a new list, and add all the questions' respective stats for that field.
            List<Integer> yData = new ArrayList<>();
            for (PermeationStats permeationStat : permeationStats) {
                yData.add(switch (field) {
                    case NUMBER_OF_CONFIRMATIONS -> permeationStat.numberOfConfirmations;
                    case NUMBER_OF_NODES_TOUCHED_ABOVE -> permeationStat.numberOfNodesTouchedAbove;
                    case NUMBER_OF_NODES_TOUCHED_BELOW -> permeationStat.numberOfNodesTouchedBelow;
                    case TOTAL_NUMBER_OF_NODES_TOUCHED -> permeationStat.totalNumberOfNodesTouched;
                });
            }
            permeationStatLists.put(field.toString(), yData);
            try {
                chart.addSeries(field.toString(), xData, yData);
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("X DATA: " + xData);
                System.out.println("Y DATA: " + yData);
            }
        }
        return chart;
    }

    public static void toggleStatistic(XYChart currentChart, PermeationStatistic statToToggle){
        if (currentChart.getSeriesMap().containsKey(statToToggle.toString())){
            currentChart.removeSeries(statToToggle.toString());
        }
        else {
            currentChart.addSeries(statToToggle.toString(), xData, permeationStatLists.get(statToToggle.toString()));
        }
    }
}

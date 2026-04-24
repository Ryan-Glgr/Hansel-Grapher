package io.github.ryan_glgr.hansel_grapher;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow;
import io.github.ryan_glgr.hansel_grapher.visualizations.InterviewStatsVisualizer;

import javax.swing.*;
import java.awt.*;
// import java.awt.*;

public class Main {

    public static void main(final String[] args) {
        final boolean GUI = args.length > 0 && args[0].equals("--gui");
        if (GUI) {
            SwingUtilities.invokeLater(() -> {
                final MainWindow mainScreen = new MainWindow();
                final JFrame frame = new JFrame("Hansel Grapher");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(mainScreen.getMainPanel());
                frame.setMinimumSize(new Dimension(960, 640));
                frame.setPreferredSize(new Dimension(960, 640));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        } else {
            InterviewStatsVisualizer.visualizeStatistics(InterviewCreationTestCases.createBasicInterviewWithSubfunctions(InterviewMode.TRADITIONAL_BINARY_SEARCH));
//            for (final InterviewMode mode: InterviewMode.values()) {
//                InterviewCreationTestCases.createHeartFailureInterview(mode);
//            }
        }
    }
}

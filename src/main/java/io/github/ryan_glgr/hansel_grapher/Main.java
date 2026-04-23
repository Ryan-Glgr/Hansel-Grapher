package io.github.ryan_glgr.hansel_grapher;

import com.formdev.flatlaf.FlatIntelliJLaf;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.Visualizations.GUI.MainScreen;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;

import javax.swing.*;
import java.awt.*;
// import java.awt.*;

public class Main {

    public static void main(final String[] args) {
        final boolean GUI = args.length > 0 && args[0].equals("--gui");
        if (GUI) {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    // Make Swing decorations (titlebar, borders) use the L&F
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    JDialog.setDefaultLookAndFeelDecorated(true);
                } catch (final UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                final MainScreen mainScreen = new MainScreen();

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

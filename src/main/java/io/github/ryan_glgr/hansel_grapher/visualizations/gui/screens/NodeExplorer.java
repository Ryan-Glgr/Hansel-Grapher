package io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * A popup window that appears when a node is clicked.
 * Contains one button per {@link NodeExplorerResult}; the pressed button's result is returned.
 */
public class NodeExplorer {

    @AllArgsConstructor
    @Getter
    public enum NodeExplorerResult {
        SEE_EXPANSIONS("See Node Expansions"),
        ADJUST_WITH_EXPANSION("Adjust and Expand New Classification"),
        ADJUST_WITHOUT_EXPANSION("Adjust without Expanding New Classification"),
        ;

        private final String buttonTitle;
    }

    private final JDialog dialog;

    // Null until a button is pressed; stays null if the window is simply closed.
    private NodeExplorerResult result;

    public static NodeExplorerResult createNodeExplorerWindowAndGetResult() {
        final NodeExplorer explorer = new NodeExplorer();
        explorer.show(); // modal: returns once the dialog is disposed
        return explorer.result;
    }

    private NodeExplorer() {

        dialog = new JDialog(null, "Node Explorer", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        for (final NodeExplorerResult option : NodeExplorerResult.values()) {
            final JButton button = new JButton(option.getButtonTitle());
            button.addActionListener(e -> choose(option));
            panel.add(button);
        }

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setResizable(false);
        dialog.toFront();
        positionOverMouse();
    }

    private void choose(final NodeExplorerResult chosen) {
        this.result = chosen;
        closeWindow();
    }

    public void show() {
        dialog.setVisible(true);
    }

    public void closeWindow() {
        dialog.dispose();
    }

    // Centers the dialog on the mouse pointer, kept fully on the screen the pointer is on.
    private void positionOverMouse() {
        final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            dialog.setLocationRelativeTo(null); // no pointer available: center on screen
            return;
        }

        final Point mouse = pointerInfo.getLocation();
        final GraphicsConfiguration config = pointerInfo.getDevice().getDefaultConfiguration();
        final Rectangle screen = config.getBounds();
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config); // taskbars, docks

        final int minX = screen.x + insets.left;
        final int minY = screen.y + insets.top;
        final int maxX = screen.x + screen.width - insets.right - dialog.getWidth();
        final int maxY = screen.y + screen.height - insets.bottom - dialog.getHeight();

        final int x = Math.max(minX, Math.min(maxX, mouse.x - dialog.getWidth() / 2));
        final int y = Math.max(minY, Math.min(maxY, mouse.y - dialog.getHeight() / 2));

        dialog.setLocation(x, y);
    }

}
package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;

// Identifies both the node and its position in the layout.
public record NodeHit(Node node, int column, int row) {
}

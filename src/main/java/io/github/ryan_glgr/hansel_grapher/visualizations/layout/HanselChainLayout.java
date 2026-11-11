package io.github.ryan_glgr.hansel_grapher.visualizations.layout;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure layout computation for the Hansel-chain grid: given the chains and their low-unit
 * classifications, computes per-node world-space positions, the padded row/column grid,
 * and the overall bounding box. Contains no rendering-specific (GL, DOT, ...) code, so it
 * can be shared by every consumer that needs to know "where does this node go."
 */
@Getter
public class HanselChainLayout {

    // --- Layout constants, shared by every consumer of this layout ---
    public static final float NODE_WIDTH = 4.0f;
    public static final float NODE_HEIGHT = 2.0f;
    public static final float SIDE_SPACING = NODE_WIDTH / 10f;
    public static final float VERTICAL_SPACING = NODE_HEIGHT / 6f;
    public static final float MARGIN = SIDE_SPACING + VERTICAL_SPACING;
    public static final float ROW_STEP = NODE_HEIGHT + VERTICAL_SPACING;
    public static final float COL_STEP = NODE_WIDTH + SIDE_SPACING;

    private final ArrayList<ArrayList<Node>> chains;      // sorted for visualization
    private final Map<Node, LowUnit> lowUnitNodes;

    private final int totalNodes;

    private final int numExclusiveLowUnits;

    private final int longestChainHeight;

    private final Node[][] nodeGrid;        // [chainIndex][rowIndex], null = empty cell

    private final String[][][] labelGrid;   // [chainIndex][rowIndex] -> label lines, null = empty

    private final float[] columnX;          // columnX[c] = center X of chain c

    private final float baseRowY;           // world Y of row 0 (bottom row)

    private final float[] worldBounds;      // { minX, maxX, minY, maxY }


    public HanselChainLayout(final ArrayList<ArrayList<Node>> chains,
                             final Map<Integer, Set<LowUnit>> lowUnitsByClass) {

        this.chains = GUIHelper.sortChainsForVisualization(chains);
        this.totalNodes = this.chains.stream().mapToInt(List::size).sum();

        this.lowUnitNodes = lowUnitsByClass.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toMap(LowUnit::getDatapoint, Function.identity()));

        this.numExclusiveLowUnits = (int) lowUnitNodes.values().stream()
                .filter(lowUnit -> LowUnit.Type.EXCLUSIVE.equals(lowUnit.getLowUnitType()))
                .count();

        this.longestChainHeight = this.chains.stream().mapToInt(List::size).max().orElseThrow();

        final int numChains = this.chains.size();
        this.nodeGrid  = new Node[numChains][longestChainHeight];
        this.labelGrid = new String[numChains][longestChainHeight][];
        this.columnX   = new float[numChains];

        final float totalHeightMax = longestChainHeight * NODE_HEIGHT + (longestChainHeight - 1) * VERTICAL_SPACING;
        this.baseRowY = -(totalHeightMax / 2.0f) + NODE_HEIGHT / 2.0f;

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (int c = 0; c < numChains; c++) {
            final ArrayList<Node> chain = this.chains.get(c);
            columnX[c] = c * COL_STEP;

            final int startRow = (longestChainHeight - chain.size()) / 2;  // centers within the padded grid
            for (int i = 0; i < chain.size(); i++) {
                final int row = startRow + i;
                final Node node = chain.get(i);

                nodeGrid[c][row]  = node;
                labelGrid[c][row] = GUIHelper.nodeLabelArray(node, getLowUnitType(node));

                final float cx = columnX[c];
                final float cy = baseRowY + row * ROW_STEP;

                minX = Math.min(minX, cx - NODE_WIDTH  / 2f);
                maxX = Math.max(maxX, cx + NODE_WIDTH  / 2f);
                minY = Math.min(minY, cy - NODE_HEIGHT / 2f);
                maxY = Math.max(maxY, cy + NODE_HEIGHT / 2f);
            }
        }

        this.worldBounds = new float[]{ minX - MARGIN, maxX + MARGIN, minY - MARGIN, maxY + MARGIN };
    }

    public LowUnit.Type getLowUnitType(final Node node) {
        final LowUnit lowUnit = lowUnitNodes.get(node);
        return lowUnit == null ? null : lowUnit.getLowUnitType();
    }

    public float getX(final int chainIndex) { return columnX[chainIndex]; }
    public float getY(final int row)        { return baseRowY + row * ROW_STEP; }

}
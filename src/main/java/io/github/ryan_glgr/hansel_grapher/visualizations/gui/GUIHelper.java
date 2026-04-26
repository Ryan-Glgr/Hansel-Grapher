package io.github.ryan_glgr.hansel_grapher.visualizations.gui;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class GUIHelper {

    private static final String LEFT_FLOOR  = "⌊";
    private static final String RIGHT_FLOOR = "⌋";

    // Returns a Color object (with alpha baked in)
    public static Color getColorForClass(final int classification, final boolean isLowUnit) {
        if (classification == Node.IMPOSSIBLE_CLASSIFICATION) {
            final int grayValue = 64;
            final int alphaInt = Math.round((isLowUnit ? 1.0f : 0.65f) * 255);
            return new Color(grayValue, grayValue, grayValue, alphaInt);
        }

        final float goldenRatio = 0.618033988749895f;
        final float hue = (classification * goldenRatio) % 1.0f;
        final int rgb = Color.HSBtoRGB(hue, 0.7f, 0.95f);
        final Color baseColor = new Color(rgb);
        final int alphaInt = Math.round((isLowUnit ? 1.0f : 0.65f) * 255);
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaInt);
    }

    // Converts a Color to Graphviz-compatible #RRGGBBAA hex string
    public static String colorToHex(final Color c) {
        return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public static String nodeLabel(final Node temp, final boolean isLow) {

        final String cls;
        if (Node.IMPOSSIBLE_CLASSIFICATION.equals(temp.classification)) {
            cls = "N/A";
        } else {
            cls = (isLow)
                ? String.format("%s%s%s", LEFT_FLOOR, temp.classification, RIGHT_FLOOR)
                : String.format(" %s ", temp.classification);
        }
        return Arrays.toString(temp.values) + "\\nClassification: " + cls;
    }

    public static ArrayList<ArrayList<Node>> sortChainsForVisualization(final ArrayList<ArrayList<Node>> chainSet){

        // Apply the sort result back to hanselChainSet
        chainSet.sort((final ArrayList<Node> a, final ArrayList<Node> b) -> b.size() - a.size());

        // now give them the diamond shape.
        final ArrayList<ArrayList<Node>> newOrdering = new ArrayList<ArrayList<Node>>();
        for(int i = 0; i < chainSet.size(); i++){
            // if even, put it in the front, if odd, the back. that is how we will alternate and get that shape.
            if (i % 2 == 0){
                newOrdering.add(chainSet.get(i));
            }
            else{
                newOrdering.addFirst(chainSet.get(i));
            }
        }
        // change the pointer to hanselChainSet to the reordered ones.
        return newOrdering;
    }
}

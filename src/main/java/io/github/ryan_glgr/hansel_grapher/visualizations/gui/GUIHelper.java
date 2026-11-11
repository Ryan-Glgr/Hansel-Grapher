package io.github.ryan_glgr.hansel_grapher.visualizations.gui;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;

import java.awt.*;
import java.util.ArrayList;

public class GUIHelper {

    private static final String LEFT_FLOOR  = "⌊";
    private static final String RIGHT_FLOOR = "⌋";
    private static final String LEFT_CEILING = "⌈";
    private static final String RIGHT_CEILING = "⌉";
    private static final float NON_LOW_UNIT_alpha = 0.65f;
    private static final float LOW_UNIT_ALPHA = 0.85f;
    private static final int IMPOSSIBLE_CLASS_GREY = 140;
    private static final float GOLDEN_RATIO = 0.618033988749895f;
    private static final float REGULAR_SATURATION = 0.6f;
    private static final float REGULAR_BRIGHTNESS = 0.95f;

    private static final String INDENT = "  ";
    private static final String CLASSIFICATION_KEY = "class";

    // Returns a Color object (with alpha baked in)
    public static Color getColorForClass(final int classification, final boolean isLowUnit) {

        if (classification == Node.IMPOSSIBLE_CLASSIFICATION) {
            final int alphaInt = Math.round((isLowUnit ? LOW_UNIT_ALPHA : NON_LOW_UNIT_alpha) * 255);
            return new Color(IMPOSSIBLE_CLASS_GREY, IMPOSSIBLE_CLASS_GREY, IMPOSSIBLE_CLASS_GREY, alphaInt);
        }

        final float saturation = isLowUnit
                ? 1.0f
                : REGULAR_SATURATION;
        final float hue = (classification * GOLDEN_RATIO) % 1.0f;
        final int rgb = Color.HSBtoRGB(hue, saturation, REGULAR_BRIGHTNESS);
        final Color baseColor = new Color(rgb);
        final int alphaInt = Math.round((isLowUnit ? LOW_UNIT_ALPHA : NON_LOW_UNIT_alpha) * 255);
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaInt);
    }

    // Converts a Color to Graphviz-compatible #RRGGBBAA hex string
    public static String colorToHex(final Color c) {
        return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    // ---------------------------------------------------------------------
    // Attribute names
    // ---------------------------------------------------------------------

    // Default attribute names: x0 ... x(n-1)
    private static String[] defaultAttributeNames(final int count) {
        final String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = "x" + i;
        }
        return names;
    }

    // Throws if the node/names can't be rendered together.
    private static void validate(final Node node, final String[] attributeNames) {
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null.");
        }
        if (node.values == null) {
            throw new IllegalArgumentException("Node has no values to print.");
        }
        if (attributeNames == null) {
            throw new IllegalArgumentException("Attribute names must not be null.");
        }
        if (attributeNames.length != node.values.length) {
            throw new IllegalArgumentException(String.format(
                    "Expected %d attribute names (one per node value) but got %d.",
                    node.values.length, attributeNames.length));
        }
        for (int i = 0; i < attributeNames.length; i++) {
            if (attributeNames[i] == null) {
                throw new IllegalArgumentException("Attribute name at index " + i + " is null.");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Labels
    // ---------------------------------------------------------------------

    // Same signature as before; now prints JSON-style using default names x0..xn.
    // Lines are joined with a literal backslash-n for Graphviz.
    public static String nodeLabel(final Node temp, final boolean isLow) {
        if (temp == null || temp.values == null) {
            throw new IllegalArgumentException("Node and its values must not be null.");
        }
        return nodeLabel(temp, isLow, defaultAttributeNames(temp.values.length));
    }

    // New overload: custom attribute names. Throws IllegalArgumentException for unsupported input
    // (null node/values/names, or names count not matching the number of values).
    public static String nodeLabel(final Node temp, final boolean isLow, final String[] attributeNames) {
        validate(temp, attributeNames);
        final LowUnit.Type type = isLow ? LowUnit.Type.INCLUSIVE : null;
        final String[] lines = nodeLabelArray(temp, type, attributeNames);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\\n");
            }
            // quotes must be escaped inside a Graphviz label="..." string
            sb.append(lines[i].replace("\"", "\\\""));
        }
        return sb.toString();
    }

    // Same signature as before; now returns JSON-style lines using default names x0..xn.
    public static String[] nodeLabelArray(final Node temp, final LowUnit.Type lowUnitType) {
        if (temp == null || temp.values == null) {
            throw new IllegalArgumentException("Node and its values must not be null.");
        }
        return nodeLabelArray(temp, lowUnitType, defaultAttributeNames(temp.values.length));
    }

    // New overload: custom attribute names. Returns one array element per line of pretty-printed JSON:
    //
    //   {
    //     "x0": 1,
    //     "x1": 2,
    //     "classification": "⌊3⌋"
    //   }
    //
    // Throws IllegalArgumentException for unsupported input (see validate).
    public static String[] nodeLabelArray(final Node temp, final LowUnit.Type lowUnitType, final String[] attributeNames) {
        validate(temp, attributeNames);

        final String cls;
        if (Node.IMPOSSIBLE_CLASSIFICATION.equals(temp.classification)) {
            cls = "N/A";
        } else {
            cls = switch (lowUnitType) {
                case INCLUSIVE -> String.format("%s%s%s", LEFT_FLOOR, temp.classification, RIGHT_FLOOR);
                case EXCLUSIVE -> String.format("%s%s%s", LEFT_CEILING, temp.classification, RIGHT_CEILING);
                case null, default -> String.format("%s", temp.classification);
            };
        }

        final int n = attributeNames.length;
        final String[] lines = new String[n + 1]; // "{" + n attributes + classification + "}"
        for (int i = 0; i < n; i++) {
            lines[i] = INDENT + jsonString(attributeNames[i]) + ": " + temp.values[i] + ",";
        }
        lines[n] = INDENT + jsonString(CLASSIFICATION_KEY) + ": " + jsonString(cls);
        return lines;
    }

    private static String jsonString(final String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
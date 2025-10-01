import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.awt.Color;

public class Visualization {
    
    private static ArrayList<ArrayList<Node>> sortChainsForVisualization(ArrayList<ArrayList<Node>> chainSet){

        // Apply the sort result back to hanselChainSet
        chainSet.sort((ArrayList<Node> a, ArrayList<Node> b) -> {
            return b.size() - a.size();
        });

        // now give them the diamond shape.
        ArrayList<ArrayList<Node>> newOrdering = new ArrayList<ArrayList<Node>>();
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

    // Generate a color for a given classification using HSV color space
    private static String getColorForClass(int classification) {
            // Use golden ratio to space out hues nicely
            float goldenRatio = 0.618033988749895f;
            
            // Generate hue by spacing classifications evenly and offsetting by golden ratio
            float hue = (classification * goldenRatio) % 1.0f;
            
            // Keep saturation and value high for vibrant, distinct colors
            float saturation = 0.7f;
            float value = 0.95f;
            
            // Convert HSV to RGB
            int rgb = Color.HSBtoRGB(hue, saturation, value);
            Color color = new Color(rgb);
            
            // Convert to hex format for DOT
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }
    
    // --- Constants ---
    private static final String NODE_SHAPE = "rectangle";

    // Unicode floor symbols
    private static final char LEFT_FLOOR = '\u230A';
    private static final char RIGHT_FLOOR = '\u230B';

    // --- Escaping helper ---
    private static final String escapeQuote(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    // --- Low-unit classification highlight ---
    private static String highlightClassification(Node temp, boolean isLow, int maxClassificationLength) {
        String cls = String.valueOf(temp.classification);
        if (isLow) {
            // wrap with floor symbols
            return LEFT_FLOOR + cls + RIGHT_FLOOR;
        } else {
            // pad with spaces to align visually
            int pad = maxClassificationLength - cls.length();
            int left = pad / 2;
            int right = pad - left;
            return " ".repeat(left) + cls + " ".repeat(right);
        }
    }

    // --- Shared node-writing helper ---
    private static void writeNode(FileWriter fw, Node temp, boolean isLow, int maxClassificationLength) throws IOException {
        StringBuilder attr = new StringBuilder();
        String label = nodeLabel(temp, isLow, maxClassificationLength);
        attr.append("label = \"").append(escapeQuote(label)).append("\"");        
        String nodeColor = getColorForClass(temp.classification);

        attr.append("label = \"").append(escapeQuote(label)).append("\"");
        attr.append(", shape = ").append(NODE_SHAPE);
        attr.append(", style = filled");
        attr.append(", fillcolor = \"").append(nodeColor).append("\"");

        fw.write(temp.hashCode() + " [" + attr.toString() + "];\n\t");
    }

    private static String nodeLabel(Node temp, boolean isLow, int maxClassificationLength) {

        String cls = String.valueOf(temp.classification);
        if (!isLow) {
            int pad = maxClassificationLength - cls.length();
            int left = pad / 2;
            int right = pad - left;
            cls = " ".repeat(left) + cls + " ".repeat(right);
        } else {
            cls = LEFT_FLOOR + cls + RIGHT_FLOOR; // or bold if you want
        }

        // 2. Values array line
        String valuesLine = Arrays.toString(temp.values);

        // 3. Combine into multi-line DOT label
        return valuesLine + "\\nClassification: " + cls;
    }


    // --- makeExpansionsDOT ---
    public static void makeExpansionsDOT(HashMap<Integer, Node> allNodes, ArrayList<ArrayList<Node>> lowUnitsByClass) throws IOException {
        HashSet<Node> lowSet = new HashSet<>();
        if (lowUnitsByClass != null)
            for (ArrayList<Node> listForClass : lowUnitsByClass)
                if (listForClass != null) lowSet.addAll(listForClass);

        int maxClassificationLength = allNodes.values().stream()
                .mapToInt(n -> String.valueOf(n.classification).length())
                .max().orElse(1);

        Integer[] kValsToMakeNode = Node.counterInitializer();
        HashMap<Node, Node> usedNodes = new HashMap<>();
        FileWriter fw = new FileWriter("out/Expansions.dot");
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        while (Node.incrementCounter(kValsToMakeNode)) {
            Node temp = allNodes.get(Node.hash(kValsToMakeNode));
            if (!usedNodes.containsKey(temp)) {
                usedNodes.put(temp, temp);
                writeNode(fw, temp, lowSet.contains(temp), maxClassificationLength);
            }

            for (Node ex : temp.upExpansions) {
                if (ex == null) continue;
                if (!usedNodes.containsKey(ex)) {
                    usedNodes.put(ex, ex);
                    writeNode(fw, ex, lowSet.contains(ex), maxClassificationLength);
                }
                fw.write(temp.hashCode() + " -> " + ex.hashCode() +
                        " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
            }
        }

        fw.write("}");
        fw.close();
    }

    // --- makeHanselChainDOT ---
    public static void makeHanselChainDOT(ArrayList<ArrayList<Node>> chains, ArrayList<ArrayList<Node>> lowUnitsByClass) throws IOException {
        chains = sortChainsForVisualization(chains);

        HashSet<Node> lowSet = new HashSet<>();
        if (lowUnitsByClass != null)
            for (ArrayList<Node> listForClass : lowUnitsByClass)
                if (listForClass != null) lowSet.addAll(listForClass);

        int maxClassificationLength = chains.stream()
                .flatMap(ArrayList::stream)
                .mapToInt(n -> String.valueOf(n.classification).length())
                .max().orElse(1);

        FileWriter fw = new FileWriter("out/HanselChains.dot");
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        ArrayList<Node> middleNodes = new ArrayList<>();

        for (ArrayList<Node> chain : chains) {
            middleNodes.add(chain.get(chain.size() / 2));

            for (Node temp : chain) {
                writeNode(fw, temp, lowSet.contains(temp), maxClassificationLength);
            }

            for (int c = 0; c < chain.size() - 1; c++) {
                Node temp = chain.get(c);
                Node ex = chain.get(c + 1);
                fw.write(temp.hashCode() + " -> " + ex.hashCode() +
                        " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
            }
        }

        fw.write("{ rank = same; ");
        for (Node mid : middleNodes) fw.write(mid.hashCode() + " ");
        fw.write("};\n");

        fw.write("}");
        fw.close();
    }

}

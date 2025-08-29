import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.Color;

;

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


    // takes all our nodes, and writes them to a DOT file, so we can visualize all the expansions.
    public static void makeExpansionsDOT(HashMap<Integer, Node> allNodes) throws IOException{
        
        Integer[] kValsToMakeNode = Node.counterInitializer();
        
        // now that we have our node, we can just mark all it's UP expansions. we already have the nodes made, we are just marking them.
        HashMap<Node, Node> usedNodes = new HashMap<>();    
        File DOTfile = new File("Expansions.dot");
        FileWriter fw = new FileWriter(DOTfile);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

    writingLoop:
        while(Node.incrementCounter(kValsToMakeNode)){
            // get the node which corresponds to this combination of digits
            Node temp = allNodes.get(Node.hash(kValsToMakeNode));
            
            // if we haven't used temp yet, it has to go into the file.
            if (usedNodes.get(temp) == null){
                String nodeName = temp.toString();
                String nodeColor = getColorForClass(temp.classification);
                fw.write(temp.hashCode() + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = \"" + nodeColor + "\"];\n\t");
            }


            // put all the neighbors into the pile if they are not used yet.
            for(Node ex : temp.upExpansions){
                
                if (ex == null)
                    continue;

                if (usedNodes.get(ex) == null){
                    usedNodes.put(ex, ex);
                    
                    // write our node into the DOT file now.
                    String nodeName = ex.toString();
                    String nodeColor = getColorForClass(ex.classification);
                    
                    // writing the expanded value into the file before we try to make the edge to it.
                    fw.write(ex.hashCode() + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = \"" + nodeColor + "\"];\n\t");
                }

                // now we make the edge between temp and ex.
                fw.write(temp.hashCode() + " -> " + ex.hashCode() + " [dir = both, color = black, arrowhead = vee, penwdith = 2]\n\t");
            }
        }
        fw.write("}");
        fw.close();
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

// takes our already created nodes, and calls our HC generation function. then makes a simple visualization just like we had for the expansions.
public static void makeHanselChainDOT(ArrayList<ArrayList<Node>> chains) throws IOException{
    
    chains = sortChainsForVisualization(chains);

    File DOTfile = new File("HanselChains.dot");
    FileWriter fw = new FileWriter(DOTfile);
    fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

    // NEW: collect the middle node of each chain for rank = same
    ArrayList<Node> middleNodes = new ArrayList<>();

    // iterate each hansel chain. we know that each point SHOULD appear exactly once. so no need for a used map.
    // we just write the node, then the target to the next one if there is one.
    for(ArrayList<Node> chain : chains){

        // grab the middle node
        int midIndex = chain.size() / 2;
        middleNodes.add(chain.get(midIndex));

        // iterate through the chain. write the node, then the one on top. 
        for(Node temp : chain){
            String nodeName = temp.toString();
            String nodeColor = getColorForClass(temp.classification);
            fw.write(temp.hashCode() + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = \"" + nodeColor + "\"];\n\t");
        }

        // iterate through and add the guy on top for each one.
        for(int c = 0; c < chain.size() - 1; c++){

            Node temp = chain.get(c);
            Node ex = chain.get(c + 1);

            // now we make the edge between temp and the next one in the chain.
            fw.write(temp.hashCode() + " -> " + ex.hashCode() + " [dir = both, color = black, arrowhead = vee, penwidth = 2];\n\t");
        }
    }

    // NEW: enforce middle elements to share the same rank
    fw.write("{ rank = same; ");
    for(Node mid : middleNodes){
        fw.write(mid.hashCode() + " ");
    }
    fw.write("};\n");

    fw.write("}");
    fw.close();

}

}

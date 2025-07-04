import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

public class Main {

    public static Integer[] kValues = {3, 3};

    public static void main(String[] args) {

        Node.makeNodes(kValues);

        try{
            makeExpansionsDOT();
            HanselChains.generateHanselChainSet(kValues, Node.Nodes);
            makeHanselChainDOT();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // takes all our nodes, and writes them to a DOT file, so we can visualize all the expansions.
    public static void makeExpansionsDOT() throws IOException{
        
        Integer[] kValsToMakeNode = new Integer[kValues.length];
        for(int i = 0; i < kValsToMakeNode.length; i++)
            kValsToMakeNode[i] = 0;
        kValsToMakeNode[0] = -1;
        
        // now that we have our node, we can just mark all it's UP expansions. we already have the nodes made, we are just marking them.
        HashMap<Integer, Node> usedNodes = new HashMap<>();    
        File DOTfile = new File("Expansions.dot");
        FileWriter fw = new FileWriter(DOTfile);
        fw.write("digraph G {\n\trankdir = TB;\n\tbgcolor = white;\n\t");

    writingLoop:
        while(true){

            // if we have wrapped around, reset to 0. and continue.
            int attribute = 0;

            // incrementing logic to go through all digits, all k vals.
            while (kValsToMakeNode[attribute] + 1 >= kValues[attribute]){
            
                kValsToMakeNode[attribute] = 0;
                attribute++;

                // break once we've incremented all the way around.
                if (attribute >= kValues.length)
                    break writingLoop;
            }

            // increment our attribute
            kValsToMakeNode[attribute]++;
        
            // get the node which corresponds to this combination of digits
            Node temp = Node.Nodes.get(Node.hash(kValsToMakeNode));
            
            // if we haven't used temp yet, it has to go into the file.
            if (usedNodes.get(Node.hash(temp.values)) == null){
                String nodeName = Arrays.toString(temp.values);
                fw.write(Node.hash(temp.values) + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = lightgrey];\n\t");
            }


            // put all the neighbors into the pile if they are not used yet.
            for(Node ex : temp.upExpansions){
                
                if (ex == null)
                    continue;

                if (usedNodes.get(Node.hash(ex.values)) == null){
                    usedNodes.put(Node.hash(ex.values), ex);
                    
                    // write our node into the DOT file now.
                    String nodeName = Arrays.toString(ex.values);
                    
                    // writing the expanded value into the file before we try to make the edge to it.
                    fw.write(Node.hash(ex.values) + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = lightgrey];\n\t");
                }

                // now we make the edge between temp and ex.
                fw.write(Node.hash(temp.values) + " -> " + Node.hash(ex.values) + " [dir = both, color = black, arrowhead = vee, penwdith = 2]\n\t");
            }
        }
        fw.write("}");
        fw.close();
    }

    // takes our already created nodes, and calls our HC generation function. then makes a simple visualization just like we had for the expansions.
    public static void makeHanselChainDOT() throws IOException{
        
        File DOTfile = new File("HanselChains.dot");
        FileWriter fw = new FileWriter(DOTfile);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        // iterate each hansel chain. we know that each point SHOULD appear exactly once. so no need for a used map.
        // we just write the node, then the target to the next one if there is one.
        for(ArrayList<Node> chain : HanselChains.hanselChainSet){

            // iterate through the chain. write the node, then the one on top. 
            for(Node temp : chain){
                String nodeName = Arrays.toString(temp.values);
                fw.write(Node.hash(temp.values) + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = lightgrey];\n\t");
            }

            // iterate through and add the guy on top for each one.
            for(int c = 0; c < chain.size() - 1; c++){

                Node temp = chain.get(c);
                Node ex = chain.get(c + 1);

                // now we make the edge between temp and the next one in the chain.
                fw.write(Node.hash(temp.values) + " -> " + Node.hash(ex.values) + " [dir = both, color = black, arrowhead = vee, penwdith = 2]\n\t");
            }

        }

        fw.write("}");
        fw.close();
    
 
    
    }

}
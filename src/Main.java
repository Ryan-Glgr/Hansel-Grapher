import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;

public class Main {

    public static Integer[] kValues = {3, 3, 3, 3};

    public static void main(String[] args) {


        try{
            
            // make all our nodes.
            Node.makeNodes(kValues);
            
            // make the chains
            HanselChains.generateHanselChainSet(kValues, Node.Nodes);

            // classify all our data
            Interview.conductInterview(Node.Nodes, 0);

            // visualize our results
            makeHanselChainDOT();

            // make the expansions picture
            makeExpansionsDOT();

            // our scripting to make the pictures - start and continue
            ProcessBuilder pb = new ProcessBuilder("./makePNG.sh");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();

            System.exit(0);
        
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // takes all our nodes, and writes them to a DOT file, so we can visualize all the expansions.
    public static void makeExpansionsDOT() throws IOException{
        
        Integer[] kValsToMakeNode = Node.counterInitializer();
        
        // now that we have our node, we can just mark all it's UP expansions. we already have the nodes made, we are just marking them.
        HashMap<Node, Node> usedNodes = new HashMap<>();    
        File DOTfile = new File("Expansions.dot");
        FileWriter fw = new FileWriter(DOTfile);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

    writingLoop:
        while(Node.incrementCounter(kValsToMakeNode)){
            // get the node which corresponds to this combination of digits
            Node temp = Node.Nodes.get(Node.hash(kValsToMakeNode));
            
            // if we haven't used temp yet, it has to go into the file.
            if (usedNodes.get(temp) == null){
                String nodeName = temp.toString();
                fw.write(temp.hashCode() + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = lightgrey];\n\t");
            }


            // put all the neighbors into the pile if they are not used yet.
            for(Node ex : temp.upExpansions){
                
                if (ex == null)
                    continue;

                if (usedNodes.get(ex) == null){
                    usedNodes.put(ex, ex);
                    
                    // write our node into the DOT file now.
                    String nodeName = ex.toString();
                    
                    // writing the expanded value into the file before we try to make the edge to it.
                    fw.write(ex.hashCode() + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = lightgrey];\n\t");
                }

                // now we make the edge between temp and ex.
                fw.write(temp.hashCode() + " -> " + ex.hashCode() + " [dir = both, color = black, arrowhead = vee, penwdith = 2]\n\t");
            }
        }
        fw.write("}");
        fw.close();
    }

    // takes our already created nodes, and calls our HC generation function. then makes a simple visualization just like we had for the expansions.
    public static void makeHanselChainDOT() throws IOException{
        
        HanselChains.sortChainsForVisualization();

        File DOTfile = new File("HanselChains.dot");
        FileWriter fw = new FileWriter(DOTfile);
        fw.write("digraph G {\n\trankdir = BT;\n\tbgcolor = white;\n\t");

        // iterate each hansel chain. we know that each point SHOULD appear exactly once. so no need for a used map.
        // we just write the node, then the target to the next one if there is one.
        for(ArrayList<Node> chain : HanselChains.hanselChainSet){

            // iterate through the chain. write the node, then the one on top. 
            for(Node temp : chain){
                String nodeName = temp.toString();
                fw.write(temp.hashCode() + " [label = \"" + nodeName + "\", shape = rectangle, style = filled, fillcolor = lightgrey];\n\t");
            }

            // iterate through and add the guy on top for each one.
            for(int c = 0; c < chain.size() - 1; c++){

                Node temp = chain.get(c);
                Node ex = chain.get(c + 1);

                // now we make the edge between temp and the next one in the chain.
                fw.write(temp.hashCode() + " -> " + ex.hashCode() + " [dir = both, color = black, arrowhead = vee, penwdith = 2]\n\t");
            }

        }

        fw.write("}");
        fw.close();
    
 
    
    }

}
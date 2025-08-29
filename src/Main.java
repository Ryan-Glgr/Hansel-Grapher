import java.util.ArrayList;

public class Main {

    public static Integer[] kValues = {4, 3, 5, 4, 2};
    
    // Calculate the highest possible classification at compile time
    // This uses the same logic as questionExpert: sum of max values / dimension
    public static Integer highestPossibleClassification;
    
    static {
        int maxSum = 0;
        for (int k : kValues) {
            maxSum += (k - 1); // max value for each dimension is k-1
        }
        highestPossibleClassification = maxSum / kValues.length; // same as sum / dimension
    }

    public static void main(String[] args) {

        // Check if debug mode is enabled
        boolean debugMode = args.length > 0 && args[0].equals("debug");
        
        // Set debug flags in all classes
        Node.DEBUG_PRINTING = debugMode;
        Interview.DEBUG = debugMode;

        try{
            
            // make all our nodes.
            Node.makeNodes(kValues, highestPossibleClassification + 1);
            
            // make the chains
            ArrayList<ArrayList<Node>> hanselChains = HanselChains.generateHanselChainSet(kValues, Node.Nodes);

            // classify all our data
            Interview.InterviewMode mode = Interview.InterviewMode.BEST_MINIMUM_CONFIRMED;
            Interview.conductInterview(Node.Nodes, hanselChains, mode, highestPossibleClassification + 1);

            // visualize our results
            Visualization.makeHanselChainDOT(hanselChains);

            // make the expansions picture
            Visualization.makeExpansionsDOT(Node.Nodes);

            // our scripting to make the pictures - start and continue
            ProcessBuilder pb = new ProcessBuilder("./makeGraphs.sh");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();

            System.exit(0);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}

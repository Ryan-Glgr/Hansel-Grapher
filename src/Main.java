import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static Integer[] kValues = {3, 3, 4, 3, 4};
    
    // Calculate the highest possible classification at compile time
    // This uses the same logic as questionExpert: sum of max values / dimension
    public static Integer highestPossibleClassification;
    
    static {
        int maxSum = 0;
        for (int k : kValues) {
            maxSum += (k - 1); // max value for each dimension is k-1
        }
        highestPossibleClassification = maxSum / kValues.length; // same as sum / dimension

        Node.dimension = kValues.length;
    }

    public static void main(String[] args) {

        // TODO: DETERMINE WHICH WAY MOEKA WORKS. THEN DEBATE WITH DR K WHICH WAY MAKES SENSE.
        boolean isomorphicAdjustmentCascadingStyle = true;
        makeClassifyAndSaveNodes(Interview.InterviewMode.BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS, isomorphicAdjustmentCascadingStyle);

        System.exit(0);
    }

    // TODO
    // make a meta version of the interview which combines heuristics?
    // make a version which can benefit from there being less chains, though some will be tiny.
    //      right now we are hung up by the fact that we have less chains, but a lot are one point chains. so we have to check all of them at the end?

    public static void makeClassifyAndSaveNodes(Interview.InterviewMode interviewMode, boolean isomorphicAdjustmentCascadingStyle){
        try{
            
            // make all our nodes.
            HashMap<Integer, Node> nodes = Node.makeNodes(kValues, highestPossibleClassification + 1);
            // make the chains
            ArrayList<ArrayList<Node>> hanselChains = HanselChains.generateHanselChainSet(kValues, nodes, isomorphicAdjustmentCascadingStyle);

            System.out.println("NUMBER OF CHAINS:\t" + hanselChains.size());
            System.out.println("NUMBER OF NODES:\t" + nodes.size());
            System.out.println("ISOMORPHIC ADJUSTMENT MODE CASCADING?:\t" + isomorphicAdjustmentCascadingStyle);

            // classify all our data
            Interview.conductInterview(nodes, hanselChains, interviewMode, highestPossibleClassification + 1);

            // visualize our results
            Visualization.makeHanselChainDOT(hanselChains);

            // make the expansions picture
            Visualization.makeExpansionsDOT(nodes);

            // our scripting to make the pictures - start and continue
            ProcessBuilder pb = new ProcessBuilder("./makeGraphs.sh");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}

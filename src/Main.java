import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static Integer[] kValues = {6, 5, 4, 5, 6};
    
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
        makeClassifyAndSaveNodes(Interview.InterviewMode.BEST_MINIMUM_CONFIRMED, isomorphicAdjustmentCascadingStyle);

        System.exit(0);
    }

    // TODO:
    // make a meta version of the interview which combines heuristics?
    //      - A useful one might be something like using the best balanced node earlier in the interview, and then using the best min confirmed as we get further.
    //      - Or just combining the two with some kind of weighing.


    public static void makeClassifyAndSaveNodes(Interview.InterviewMode interviewMode, boolean isomorphicAdjustmentCascadingStyle){
        try{
            
            // make all our nodes.
            HashMap<Integer, Node> nodes = Node.makeNodes(kValues, highestPossibleClassification + 1);
            // make the chains
            ArrayList<ArrayList<Node>> hanselChains = HanselChains.generateHanselChainSet(kValues, nodes);

            System.out.println("NUMBER OF CHAINS:\t" + hanselChains.size());
            System.out.println("NUMBER OF NODES:\t" + nodes.size());

            // classify all our data
            Interview.conductInterview(nodes, hanselChains, interviewMode, highestPossibleClassification + 1);

            // visualize our results
            Visualization.makeHanselChainDOT(hanselChains);

            // make the expansions picture
            Visualization.makeExpansionsDOT(nodes);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}

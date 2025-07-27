import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


// this class is where we are going to handle anything classification related.
public class Interview {

    // Enum for umbrella sorting strategies
    public enum UmbrellaSortStrategy {
        HIGHEST_TOTAL_UMBRELLA,
        MOST_ABOVE,
        MOST_BELOW,
        SMALLEST_DIFFERENCE
    }
    // Strategy for umbrella-based sorting
    public static UmbrellaSortStrategy UMBRELLA_STRATEGY = UmbrellaSortStrategy.SMALLEST_DIFFERENCE;

    public static boolean DEBUG = false;

    // if we set this false, we are going to call upon some ML interviewer instead.
    public static boolean EXPERT_MODE = true;

    /* 
    IDEAS:
    - we can binary search each chain of expansions, instead of each HC. since a hansel chain can have more possible expansions which aren't in the chain.
    - we can also sort the Nodes by how many possible expansions each one has. this way we can just take the one with most possible expansions at a time.
    - we can count the number of "nodes under each umbrella." not just the number of expansions. that will be more effective most likely in determining the most powerful nodes for classification
        - then we can re sort after each question.
    - We can use some kind of algorithm to try and guess which nodes are going to most useful. That being, most above, most below, most total, etc. at a given time.
    - We can use the hansel chains to accomplish some better results interview wise as well. 

    NOTES:
    - a node is a low unit if we are expanding down and the one below is a lower class.
    - a node is a high unit if we are going up and the next guy is a higher class obviously.

    */
    
    public static void mostExpansionsFirst(ArrayList<Node> allNodes){
        // Convert to array for parallel sorting
        Node[] nodeArray = allNodes.toArray(new Node[0]);
        Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
            return Integer.compare(y.possibleExpansions, x.possibleExpansions);
        });
        // Clear and add all back in sorted order
        allNodes.clear();
        allNodes.addAll(Arrays.asList(nodeArray));
    }

    // Sort nodes based on umbrella strategy
    public static void umbrellaSort(ArrayList<Node> allNodes) {
        // Convert to array for parallel sorting
        Node[] nodeArray = allNodes.toArray(new Node[0]);
        
        switch (UMBRELLA_STRATEGY) {
            case HIGHEST_TOTAL_UMBRELLA:
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases);
                });
                break;
                
            case MOST_ABOVE:
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Integer.compare(y.aboveUmbrellaCases, x.aboveUmbrellaCases);
                });
                break;
                
            case MOST_BELOW:
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Integer.compare(y.underneathUmbrellaCases, x.underneathUmbrellaCases);
                });
                break;
                
            case SMALLEST_DIFFERENCE:
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    // Calculate the absolute difference between above and below for each node
                    int diffX = Math.abs(x.aboveUmbrellaCases - x.underneathUmbrellaCases);
                    int diffY = Math.abs(y.aboveUmbrellaCases - y.underneathUmbrellaCases);
                    
                    // Sort by smallest difference first, but among nodes with similar differences,
                    // prefer those with larger total umbrella size (more impactful)
                    if (diffX == diffY) {
                        return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases);
                    }
                    return Integer.compare(diffX, diffY);
                });
                break;
                
            default:
                // Default to highest total umbrella
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases);
                });
                break;
        }
        
        // Clear and add all back in sorted order
        allNodes.clear();
        allNodes.addAll(Arrays.asList(nodeArray));
    }


    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    public static void conductInterview(HashMap<Integer, Node> data, int mode, boolean umbrellaBased){

        ArrayList<Node> allNodes = new ArrayList<>();
        // for each node, we are going to put in that node, and it's number of expansions as a pair.
        allNodes.addAll(data.values());

        // sort our nodes in whichever order we are using.
        if (mode == 0){
            // for now we just use the most expansions first method.
            mostExpansionsFirst(allNodes);
        }

        int questionsAsked = 0;
        while(allNodes.size() != 0){

            // if we are sorting by umbrella metrics, sort using our strategy.
            // this is useful if we want to find which node may impact the most other nodes at a given time.
            if (umbrellaBased){
                // go through all the nodes, and update their umbrella sizes.
                Node.updateUmbrellaSizes(allNodes);

                // sort based on our umbrella strategy
                umbrellaSort(allNodes);
            }

            // remove the front guy
            Node n = allNodes.remove(0);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed)
                continue;

            // get our value either from expert or ML
            int c = (EXPERT_MODE) ? questionExpert(n) : -1;

            // set this nodes classification, and permeate it to his neighbors.
            n.classification = c;

            questionsAsked++;

            // this sets all the upper bounds below, and all the lower bounds above.
            n.permeateClassification();

            if (DEBUG){
                System.out.println("ASKED ABOUT NODE:\n" + n);
            }

        }
        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + data.size());

    }

    // asks the "expert" what the classification of this datapoint is.
    public static int questionExpert(Node datapoint){

        // for now, we will just say if the sum of digits is greater than dimension, it's one.
        return datapoint.sum / Node.dimension;
    }


}

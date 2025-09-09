import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

// this class is where we are going to handle anything classification related.
public class Interview {
    
    public static boolean DEBUG = false;
    // if we set this false, we are going to call upon some ML interviewer instead.
    public static boolean EXPERT_MODE = true;

    public static UmbrellaSortStrategy UMBRELLA_SORTING_STRATEGY = UmbrellaSortStrategy.BEST_BALANCE_RATIO;

    public enum InterviewMode {
        UMBRELLA_SORT,          // method where we sort nodes by how many nodes are unconfirmed above.below. submethods for prioritizing above/below/tiebreakers.
        BINARY_SEARCH_CHAINS,   // method where we just query midpoint of the chain each time. thus chopping each chain in half. we work on the longest chain at a time.
        BEST_MINIMUM_CONFIRMED, // method where we check all nodes, and determine which has the best min bound. meaning of all k classes, confirming this one as a particular class, how many nodes get confirmed. The node with the best lower bound is used each iteration.
    }

    // Enum for umbrella sorting strategies
    public enum UmbrellaSortStrategy {
        HIGHEST_TOTAL_UMBRELLA,     // sort by just the total amount in the umbrella. This means we find the node who's classification affects the most other nodes.
        MOST_ABOVE,
        MOST_BELOW,                 
        SMALLEST_DIFFERENCE,        // sort our nodes by the smallest difference above/below. this way we find balanced nodes first
        BEST_BALANCE_RATIO,         // sort our nodes by their balance ratios.
    }

    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    public static void conductInterview(HashMap<Integer, Node> data, ArrayList<ArrayList<Node>> hanselChains, InterviewMode mode, int numClasses) {

        ArrayList<Node> allNodes = new ArrayList<>();
        // for each node, we are going to put in that node, and it's number of expansions as a pair.
        allNodes.addAll(data.values());

        switch (mode){
            case InterviewMode.UMBRELLA_SORT:
                umbrellaSortInterview(allNodes, UMBRELLA_SORTING_STRATEGY, numClasses);
                break;
            case InterviewMode.BINARY_SEARCH_CHAINS:
                cutMiddleOfChainInterview(hanselChains);
                break;
            case InterviewMode.BEST_MINIMUM_CONFIRMED:
                bestMinConfirmedInterview(allNodes, numClasses);
                break;
        }
    }

    // asks the "expert" what the classification of this datapoint is.
    private static int questionExpert(Node datapoint){
        // for now, we will just say classification is sum of digits / dimension to keep it simple.
        return datapoint.sum / Node.dimension;
    }
    
    private static int questionML(Node datapoint){
        return -1;
    }

    // Sort nodes based on umbrella strategy
    // umbrella strategy considers how many nodes COULD be confirmed underneath/above a given node. for example:
    // if there are 30 unclassified nodes under a given node, it's underneath umbrella is 30. this just gives us an idea
    // of how powerful this node can be in classification.
    private static void umbrellaSortInterview(ArrayList<Node> allNodes, UmbrellaSortStrategy strategy, int numClasses) {
        
        int totalNodes = allNodes.size();

        ArrayList<Node> nodesToAsk = new ArrayList<>();
        nodesToAsk.addAll(allNodes);

        int questionsAsked = 0;
        while(nodesToAsk.size() != 0){

            // if we are sorting by umbrella metrics, sort using our strategy.
            // this is useful if we want to find which node may impact the most other nodes at a given time.
            // go through all the nodes, and update their umbrella sizes.
            Node.updateNodeRankings(allNodes, numClasses);

            // sort based on our umbrella strategy
            umbrellaSortInterviewSortingHelper(nodesToAsk, strategy);
            
            // remove the front guy
            Node n = nodesToAsk.remove(0);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed)
                continue;

            // get our value either from expert or ML
            n.classification = (EXPERT_MODE) ? questionExpert(n) : questionML(n);

            // this sets all the upper bounds below, and all the lower bounds above.
            n.permeateClassification();
            questionsAsked++;

            nodesToAsk = nodesToAsk.parallelStream()
            .filter(node -> !node.classificationConfirmed) // keep only unconfirmed nodes
            .collect(Collectors.toCollection(ArrayList::new));
        }
        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + totalNodes);
    }

    // just sorts our nodes in order of how we should ask them based on which interview technique we are trying.
    // smallest difference seems to be the best. taking that with the smallest difference between above and below, with
    // tiebreaker going to the greatest total size. That way the middle nodes ones go first.
    private static void umbrellaSortInterviewSortingHelper(ArrayList<Node> allNodes, UmbrellaSortStrategy strategy){
                
        // Convert to array for parallel sorting
        Node[] nodeArray = allNodes.toArray(new Node[0]);

        switch (strategy) {
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
            case BEST_BALANCE_RATIO:
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Float.compare(y.balanceRatio, x.balanceRatio);
                });
                break;
                
            default:
                // Default to highest total umbrella
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases);
                });
                break;
        }
    }

    // function where we search through chains, which get recursively split into chunks.
    private static void cutMiddleOfChainInterview(ArrayList<ArrayList<Node>> hanselChainSet){

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a whole bunch with one question, and we have to investigate all the 
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>();
        chunks.addAll(hanselChainSet);

        // just get the total number of nodes.
        int totalNodes = chunks.parallelStream()
            .mapToInt(ArrayList::size) // or chunk -> chunk.size()
            .sum();
    
        int questionsAsked = 0;
        while (chunks.size() > 0){

            // sort our list of chains by size. this way we use the largest chunks first.
            chunks = chunks.parallelStream()
                .sorted((ArrayList<Node> a, ArrayList<Node> b) -> {
                    return (Integer.compare(b.size(), a.size()));
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            // get the first chunk
            ArrayList<Node> chunkToQuestion = chunks.get(0);
            // get the middle node
            Node middleNode = chunkToQuestion.get(chunkToQuestion.size() / 2);
            // ask the expert or ML
            middleNode.classification = (EXPERT_MODE) ? questionExpert(middleNode) : questionML(middleNode);
            middleNode.permeateClassification();
            questionsAsked++;

            // for each chunk we have remaining of each chain, we are going to chop them up, splitting on parts where they are confirmed.
            chunks = chunks.parallelStream()
                .flatMap(chunk -> splitChunkIntoPiecesHelper(chunk).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        }

        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + totalNodes);
    }


    // splits a list of nodes (part or whole hansel chain) on nodes which are confirmed
    private static ArrayList<ArrayList<Node>> splitChunkIntoPiecesHelper(ArrayList<Node> chunk) {
        ArrayList<ArrayList<Node>> newChunks = new ArrayList<>();
        ArrayList<Node> currentChunk = new ArrayList<>();
    
        for (Node node : chunk) {
            if (node.classificationConfirmed) {
                // End current chunk if we have nodes collected
                if (!currentChunk.isEmpty()) {
                    newChunks.add(new ArrayList<>(currentChunk));
                    currentChunk.clear();
                }
                else{
                    // Skip the confirmed node entirely
                }
            } else {
                currentChunk.add(node);
            }
        }
    
        // Add last chunk if there are remaining nodes
        if (!currentChunk.isEmpty()) {
            newChunks.add(currentChunk);
        }
    
        return newChunks;
    }

    /*
     * General outline is this:
     *      get all nodes.
     *      track how many nodes would be confirmed for each node, if it was assigned each class.
     *      in a three class problem, we would use the minimum number of confirmed nodes, if a given class of the three were assigned.
     *      thus we take that node which had the largest number of confirmations (using the worst case)
     */
    private static void bestMinConfirmedInterview(ArrayList<Node> allNodes, int numClasses){
        
        int totalNodes = allNodes.size();

        ArrayList<Node> nodesToAsk = new ArrayList<>();
        nodesToAsk.addAll(allNodes);

        int questionsAsked = 0;
        while(nodesToAsk.size() != 0){

            // now sort decreasing, using our strategy. we sort descending, by a nodes minimum guaranteed classifications.
            // that is, of it's classes, whichever is the worst, we choose the one with the best floor. we are guaranteed to confirm that many at least.
            Node.updateNodeRankings(allNodes, numClasses);            
            nodesToAsk.sort((Node x, Node y) -> {
                return x.compareMinClassifications(y);
            });

            // remove the front guy
            Node n = nodesToAsk.remove(0);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed)
                continue;

            // get our value either from expert or ML
            n.classification = (EXPERT_MODE) ? questionExpert(n) : questionML(n);

            // this sets all the upper bounds below, and all the lower bounds above.
            n.permeateClassification();
            questionsAsked++;

            nodesToAsk = nodesToAsk.parallelStream()
                .filter(node -> !node.classificationConfirmed) // keep only unconfirmed nodes
                .collect(Collectors.toCollection(ArrayList::new));
        }
        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + totalNodes);
    }

}
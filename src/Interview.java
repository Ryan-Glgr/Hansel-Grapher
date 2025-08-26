import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;


// this class is where we are going to handle anything classification related.
public class Interview {
    
    public static boolean DEBUG = false;
    // if we set this false, we are going to call upon some ML interviewer instead.
    public static boolean EXPERT_MODE = true;

    public enum InterviewMode {
        UMBRELLA_SORT,
        LONGEST_CHUNKS_FIRST,
        SHORTEST_CHUNKS_FIRST
    }

    // Enum for umbrella sorting strategies
    public enum UmbrellaSortStrategy {
        HIGHEST_TOTAL_UMBRELLA,
        MOST_ABOVE,
        MOST_BELOW,
        SMALLEST_DIFFERENCE
    }


    /* 
    IDEAS:
    - we can binary search each chain of expansions, instead of each HC. since a hansel chain can have more possible expansions which aren't in the chain.
    - we can also sort the Nodes by how many possible expansions each one has. this way we can just take the one with most possible expansions at a time.
    - we can count the number of "nodes under each umbrella." not just the number of expansions. that will be more effective most likely in determining the most powerful nodes for classification
        - then we can re sort after each question.
    - MAKE REGULAR INTERVIEW TECHNIQUES WHERE WE SEARCH ONE HC AT A TIME!

    NOTES:
    - a node is a low unit if we are expanding down and the one below is a lower class.
    - a node is a high unit if we are going up and the next guy is a higher class obviously.

    */


    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    public static void conductInterview(HashMap<Integer, Node> data, InterviewMode mode){

        ArrayList<Node> allNodes = new ArrayList<>();
        // for each node, we are going to put in that node, and it's number of expansions as a pair.
        allNodes.addAll(data.values());

        switch (mode){
            case InterviewMode.UMBRELLA_SORT:
                umbrellaSortInterview(allNodes, UmbrellaSortStrategy.SMALLEST_DIFFERENCE);
                break;
            case InterviewMode.LONGEST_CHUNKS_FIRST:
                cutMiddleOfChainInterview(HanselChains.hanselChainSet, InterviewMode.LONGEST_CHUNKS_FIRST);
                break;
            case InterviewMode.SHORTEST_CHUNKS_FIRST:
                cutMiddleOfChainInterview(HanselChains.hanselChainSet, InterviewMode.SHORTEST_CHUNKS_FIRST);
                break;
            default:
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
    private static void umbrellaSortInterview(ArrayList<Node> allNodes, UmbrellaSortStrategy strategy) {
        
        int totalNodes = allNodes.size();

        int questionsAsked = 0;
        while(allNodes.size() != 0){

            // if we are sorting by umbrella metrics, sort using our strategy.
            // this is useful if we want to find which node may impact the most other nodes at a given time.
            // go through all the nodes, and update their umbrella sizes.
            Node.updateUmbrellaSizes(allNodes);

            // sort based on our umbrella strategy
            umbrellaSortInterviewSortingHelper(allNodes, strategy);
            
            // remove the front guy
            Node n = allNodes.remove(0);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed)
                continue;

            // get our value either from expert or ML
            n.classification = (EXPERT_MODE) ? questionExpert(n) : questionML(n);

            // this sets all the upper bounds below, and all the lower bounds above.
            n.permeateClassification();
            questionsAsked++;

            if (DEBUG){
                System.out.println("ASKED ABOUT NODE:\n" + n);
            }

            allNodes = allNodes.parallelStream()
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
                
            default:
                // Default to highest total umbrella
                Arrays.parallelSort(nodeArray, (Node x, Node y) -> {
                    return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases);
                });
                break;
        }
    }

    // function where we search through chains, which get recursively split into chunks.
    private static void cutMiddleOfChainInterview(ArrayList<ArrayList<Node>> hanselChainSet, InterviewMode sortDirection){

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a whole bunch with one question, and we have to investigate all the 
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>();
        chunks.addAll(hanselChainSet);

        int totalNodes = chunks.parallelStream()
        .mapToInt(ArrayList::size) // or chunk -> chunk.size()
        .sum();
    
        int questionsAsked = 0;
        while (chunks.size() > 0){

            if (sortDirection == InterviewMode.LONGEST_CHUNKS_FIRST){
                // sort our list of chains by size. this way we use the largest chunks first.
                chunks = chunks.parallelStream()
                    .sorted((ArrayList<Node> a, ArrayList<Node> b) -> {
                    return b.size() - a.size();
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
            else {
                // sort our list of chains by size. this way we use the smallest chunks first.
                chunks = chunks.parallelStream()
                    .sorted((ArrayList<Node> a, ArrayList<Node> b) -> {
                    return a.size() - b.size();
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }

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

}

package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.ryan_glgr.hansel_grapher.Main;

// this class is where we are going to handle anything classification related.
public class Interview {
    
    public static boolean DEBUG = false;
    // if we set this false, we are going to call upon some ML interviewer instead.
    public static boolean EXPERT_MODE = true;

    public enum InterviewMode {
        BINARY_SEARCH_CHAINS,                       // method where we just query midpoint of the chain each time. thus chopping each chain in half. we work on the longest chain at a time.
        BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS, // basically finds the longest expansion chain, + 1 in some attribute, as far as we can go, and binary searches that chain at each step.
        BEST_MINIMUM_CONFIRMED,                     // method where we check all nodes, and determine which has the best min bound. meaning of all k classes, confirming this one as a particular class, how many nodes get confirmed. The node with the best lower bound is used each iteration.
        HIGHEST_TOTAL_UMBRELLA_SORT,                // sort by just the total amount in the umbrella. This means we find the node who's classification affects the most other nodes.
        MOST_ABOVE_UMBRELLA_SORT,
        MOST_BELOW_UMBRELLA_SORT,                 
        SMALLEST_DIFFERENCE_UMBRELLA_SORT,          // sort our nodes by the smallest difference above/below. this way we find balanced nodes first
        BEST_BALANCE_RATIO_UMBRELLA_SORT,           // sort our nodes by their balance ratios.
    }

    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    public static void conductInterview(HashMap<Integer, Node> data, 
                                        ArrayList<ArrayList<Node>> hanselChains, 
                                        InterviewMode mode, 
                                        int numClasses) {

        ArrayList<Node> allNodes = new ArrayList<>();
        // for each node, we are going to put in that node, and it's number of expansions as a pair.
        allNodes.addAll(data.values());

        switch (mode){
            // all these go to umbrella sort, since it's the same interview, just different sorting technique.
            case HIGHEST_TOTAL_UMBRELLA_SORT:
            case MOST_ABOVE_UMBRELLA_SORT:
            case MOST_BELOW_UMBRELLA_SORT:
            case SMALLEST_DIFFERENCE_UMBRELLA_SORT:
            case BEST_BALANCE_RATIO_UMBRELLA_SORT:
                umbrellaSortInterview(allNodes, mode, numClasses);
                break;
            
            case BINARY_SEARCH_CHAINS:
                cutMiddleOfChainInterview(hanselChains);
                break;
            
            case BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS:
                // our root node is easy to find. it's the node with [0,0,...,0] since our hash function
                // is that value, we can just know that we look up 0 to find it.
                binarySearchStringOfExpansionsInterview(allNodes, data.get(0));
                break;
            
            case BEST_MINIMUM_CONFIRMED:
                bestMinConfirmedInterview(allNodes, numClasses);
                break;
        }
    }


    // just use the weights from main for simple magic function testing.
    public static Float[] kValueWeights = Main.weights;

    // asks the "expert" what the classification of this datapoint is.
    private static int questionExpert(Node datapoint){
        // for now, we will just say classification is sum of digits / dimension to keep it simple.
        // return datapoint.sum / Node.dimension;
        int sum = 0;
        for (int i = 0; i < Node.dimension; i++){
            sum += (int)(datapoint.values[i] * kValueWeights[i]);
        }
        return sum / Node.dimension;
    }
    
    private static int questionML(Node datapoint){
        return -1;
    }

    // Sort nodes based on umbrella strategy
    // umbrella strategy considers how many nodes are reachable underneath/above a given node. for example:
    private static void umbrellaSortInterview(ArrayList<Node> allNodes, InterviewMode umbrellaSortingStrategy, int numClasses) {
            
        int totalNodes = allNodes.size();
        ArrayList<Node> nodesToAsk = new ArrayList<>(allNodes);

        int questionsAsked = 0;

        // --- Build comparator once (based on umbrella strategy) ---
        // Instead of sorting every time, we’ll reuse this comparator
        // and just call Collections.min or Collections.max in the loop.
        final Comparator<Node> comparator;
        final boolean chooseMax; // true => take max, false => take min

        switch (umbrellaSortingStrategy) {
            case HIGHEST_TOTAL_UMBRELLA_SORT:
                comparator = Comparator.comparingInt(n -> n.totalUmbrellaCases);
                chooseMax = true;
                break;

            case MOST_ABOVE_UMBRELLA_SORT:
                comparator = Comparator.comparingInt(n -> n.aboveUmbrellaCases);
                chooseMax = true;
                break;

            case MOST_BELOW_UMBRELLA_SORT:
                comparator = Comparator.comparingInt(n -> n.underneathUmbrellaCases);
                chooseMax = true;
                break;

            case BEST_BALANCE_RATIO_UMBRELLA_SORT:
                comparator = Comparator.comparingDouble(n -> n.balanceRatio);
                chooseMax = true;
                break;

            case SMALLEST_DIFFERENCE_UMBRELLA_SORT:
            default:
                comparator = (x, y) -> {
                    // Calculate the absolute difference between above and below for each node
                    int diffX = Math.abs(x.aboveUmbrellaCases - x.underneathUmbrellaCases);
                    int diffY = Math.abs(y.aboveUmbrellaCases - y.underneathUmbrellaCases);

                    // Sort by smallest difference first, but among nodes with similar differences,
                    // prefer those with larger total umbrella size (more impactful)
                    if (diffX == diffY) {
                        return Integer.compare(y.totalUmbrellaCases, x.totalUmbrellaCases);
                    }
                    return Integer.compare(diffX, diffY);
                };
                chooseMax = false; // for smallest difference, we want the min
                break;
        }

        // --- Main loop ---
        while (!nodesToAsk.isEmpty()) {

            // if we are sorting by umbrella metrics, sort using our strategy.
            // this is useful if we want to find which node may impact the most other nodes at a given time.
            // go through all the nodes, and update their umbrella sizes.
            Node.updateAllNodeRankings(nodesToAsk, numClasses);

            // Instead of sorting every time, just find the best node according to the chosen comparator
            Node n = chooseMax
                ? Collections.max(nodesToAsk, comparator)
                : Collections.min(nodesToAsk, comparator);

            // remove the front guy (the chosen one)
            nodesToAsk.remove(n);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed)
                continue;

            // get our value either from expert or ML
            n.classification = (EXPERT_MODE) ? questionExpert(n) : questionML(n);

            // this sets all the upper bounds below, and all the lower bounds above.
            n.permeateClassification();
            questionsAsked++;

            // filter out confirmed nodes and continue
            nodesToAsk = nodesToAsk.parallelStream()
                .filter(node -> !node.classificationConfirmed)
                .collect(Collectors.toCollection(ArrayList::new));
        }

        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + totalNodes);
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

    private static void binarySearchStringOfExpansionsInterview(ArrayList<Node> allNodes, Node rootNode) {
        int totalNodes = (int) allNodes.size();
        int questionsAsked = 0;

        while (true) {
            // Collect still alive nodes
            List<Node> aliveNodes = allNodes.parallelStream()
                .filter(n -> !n.classificationConfirmed)
                .collect(Collectors.toList());

            if (aliveNodes.isEmpty()) 
                break;

            // Find the longest chain among alive nodes.
            // NOT a hansel chain necessarily. Thought it could be technically a hansel chain?
            // but just a string of Nodes which are all + 1 in some attribute from another.
            // just the longest string of dominoes.
            ArrayList<Node> longestChain = findLongestStringOfExpansions(aliveNodes);
            // ArrayList<Node> longestChain = findLongestStringOfExpansionsHelper(rootNode);

            if (longestChain.isEmpty()) 
                break; // safety

            // Take the middle node of the longest chain
            Node middleNode = longestChain.get(longestChain.size() / 2);

            // Query expert or ML
            middleNode.classification = (EXPERT_MODE)
                ? questionExpert(middleNode)
                : questionML(middleNode);

            // Permeate classification
            middleNode.permeateClassification();
            questionsAsked++;
        }
        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + totalNodes);
    }

    // ALL NODES IS ONLY THE UNCONFIRMED NODES AT THIS STEP OF INTERVIEW!
    // This returns the longest possible chain of expansions at this stage of our interview. IMPORTANT: this is NOT a hansel chain, but rather just a chain of Nodes which are + 1 from one another. They could and will be all in different chains all over the place.
    private static ArrayList<Node> findLongestStringOfExpansions(List<Node> allNodes) {
        
        /*
         * Build our longest possible path like this:
         * 
         * - find all our LEAVES (nodes with no unconfirmed neighbors in upExpansions. Up and down are interchangeable in this algorithm, you just have to use whichever one consistently).
         *      - initialize them with a length of 1.
         * - iteratively, kind of like a Bellman-Ford, we check each node, and check if we can put a bigger length on it. We do this just by checking if any of it's upExpansions have a larger value than what we have.
         *     - if we find a bigger length, we update it.
         * - Then we go through and rebuild our path by finding the node at each step with the longest possible length.
         */ 

        // map: longest chain length starting at each node
        Map<Node, Integer> longestPossibleChainOfExpansionsForEachNodeMap = new HashMap<>();

        // Step 1: initialize leaves (no unconfirmed upstairs neighbors) with a length of 1.
        for (Node n : allNodes) {
            boolean isTerminal = Arrays.stream(n.upExpansions)
                .allMatch(nb -> nb == null || nb.classificationConfirmed);
            if (isTerminal) {
                longestPossibleChainOfExpansionsForEachNodeMap.put(n, 1);
            }
        }

        // Step 2: iteratively update each node's longest length, until we have no progress.
        boolean progress = true;
        while (progress) {
            progress = false;

            for (Node n : allNodes) {

                int bestNeighbor = Arrays.stream(n.upExpansions)
                    .filter(neighbor -> neighbor != null && !neighbor.classificationConfirmed)
                    .mapToInt(neighbor -> longestPossibleChainOfExpansionsForEachNodeMap.getOrDefault(neighbor, 0))
                    .max()
                    .orElse(0);

                // if our best neighbor is not 0, we check if it is better than what we had.
                int newVal = (bestNeighbor > 0) ? bestNeighbor + 1 : 0;
                int oldVal = longestPossibleChainOfExpansionsForEachNodeMap.getOrDefault(n, 0);

                // if our new value is better than what we had, put that in.
                if (newVal > oldVal) {
                    longestPossibleChainOfExpansionsForEachNodeMap.put(n, newVal);
                    progress = true;
                }
            }
        }

        // Step 3: pick starting node with the max value from our input list.
        Node bestNode = longestPossibleChainOfExpansionsForEachNodeMap.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);

        // if we didn't have one just get out of here.
        if (bestNode == null) 
            return new ArrayList<>();

        // Step 4: reconstruct path greedily
        ArrayList<Node> path = new ArrayList<>();
        Node current = bestNode;
        while (current != null && longestPossibleChainOfExpansionsForEachNodeMap.containsKey(current)) {
            path.add(current);

            // pick the next node from my down expansions. We want that with the highest count at each step This is the DP approach.
            Node next = Arrays.stream(current.upExpansions)
                .filter(nb -> nb != null && !nb.classificationConfirmed)
                .max(Comparator.comparingInt(nb -> longestPossibleChainOfExpansionsForEachNodeMap.getOrDefault(nb, 0)))
                .orElse(null);

            current = next;
        }

        return path;
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
        Comparator<Node> comp = (a, b) -> a.compareMinClassifications(b);

        ArrayList<Node> nodesToAsk = new ArrayList<>();
        nodesToAsk.addAll(allNodes);

        int questionsAsked = 0;
        while(nodesToAsk.size() != 0){

            // now sort decreasing, using our strategy. we sort descending, by a nodes minimum guaranteed classifications.
            // that is, of it's classes, whichever is the worst, we choose the one with the best floor. we are guaranteed to confirm that many at least.
            Node.updateAllNodeRankings(nodesToAsk, numClasses);            
            
            Node n = Collections.max(nodesToAsk, comp);
            nodesToAsk.remove(n);

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
        System.out.println("TOTAL NODES:\t" + totalNodes);
        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
    }

}
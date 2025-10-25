package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats;

// this class is where we are going to handle anything classification related.
public class Interview {
    
    public static boolean DEBUG = false;
    // if we set this false, we are going to call upon some ML interviewer instead.
    public static boolean MAGIC_FUNCTION_MODE = true;

    private final Integer[] kVals;
    private final Float[] kValueWeights;
    public final InterviewStats interviewStats;

    public enum InterviewMode {
        BINARY_SEARCH_CHAINS,                       // method where we just query midpoint of the chain each time. thus chopping each chain in half. we work on the longest chain at a time.
        BINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE,
        BINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED,
        BINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT,
        BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY,
        BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY,
        BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC,
        BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS, // basically finds the longest expansion chain, + 1 in some attribute, as far as we can go, and binary searches that chain at each step.

        NONBINARY_SEARCH_CHAINS,
        NONBINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE,
        NONBINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED,
        NONBINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT,
        NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY,
        NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY,
        NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC,

        BEST_MINIMUM_CONFIRMED,                     // method where we check all nodes, and determine which has the best min bound. meaning of all k classes, confirming this one as a particular class, how many nodes get confirmed. The node with the best lower bound is used each iteration.

        HIGHEST_TOTAL_UMBRELLA_SORT,                // sort by just the total amount in the umbrella. This means we find the node who's classification affects the most other nodes.
        SMALLEST_DIFFERENCE_UMBRELLA_SORT,          // sort our nodes by the smallest difference above/below. this way we find balanced nodes first

        BEST_BALANCE_RATIO_UNITY,
        BEST_BALANCE_RATIO_SHANNON_ENTROPY,
        BEST_BALANCE_RATIO_QUADRATIC,
    }

    public Interview(Integer[] kVals,
            Float[] weights,
            HashMap<Integer, Node> data,
            ArrayList<ArrayList<Node>> hanselChains,
            InterviewMode mode,
            int numClasses){

        inputScanner = new Scanner(System.in);

        this.kVals = kVals;
        this.kValueWeights = weights;
        this.interviewStats = conductInterview(data, hanselChains, mode, numClasses);
    }

    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    private InterviewStats conductInterview(HashMap<Integer, Node> data,
           ArrayList<ArrayList<Node>> hanselChains,
           InterviewMode mode,
           int numClasses) {

        ArrayList<Node> allNodes = new ArrayList<>(data.values());
        InterviewStats stats = switch(mode) {

            case HIGHEST_TOTAL_UMBRELLA_SORT ->
                umbrellaSortInterview(allNodes, NodeComparisons.HIGHEST_TOTAL_UMBRELLA, numClasses);

            case SMALLEST_DIFFERENCE_UMBRELLA_SORT ->
                umbrellaSortInterview(allNodes, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA, numClasses);

            case BEST_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }
            case BEST_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }
            case BEST_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }
            case BINARY_SEARCH_CHAINS ->
                // extra args after hansel chains don't matter for regular binary search.
                binarySearchHCsInterview(hanselChains, false, null, numClasses);

            case BINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE ->
                // we use the MIN for smallest difference
                binarySearchHCsInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA, numClasses);

            case BINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT ->
                binarySearchHCsInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA, numClasses);

            case BINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED ->
                binarySearchHCsInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS, numClasses);

            case BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield binarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }

            case BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield binarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }

            case BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield binarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }

            case NONBINARY_SEARCH_CHAINS ->
                // extra args after hansel chains don't matter for regular binary search.
                    nonBinarySearchHCsInterview(hanselChains, false, null, numClasses);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE ->
                    // we use the MIN for smallest difference
                    nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA, numClasses);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT ->
                    nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA, numClasses);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED ->
                    nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS, numClasses);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO, numClasses);
            }

            case BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS ->
                // our root node is easy to find. it's the node with [0,0,...,0] since our hash function
                // is that value, we can just know that we look up 0 to find it.
                binarySearchStringOfExpansionsInterview(allNodes);

            case BEST_MINIMUM_CONFIRMED ->
                bestMinConfirmedInterview(allNodes, numClasses);

            default -> {
                System.out.println("PROGRAMMER IS AN IDIOT!");
                System.exit(0);
                yield null; // required inside a block
            }
        };

        return new InterviewStats(kVals,
            hanselChains.size(),
            data.size(),
            mode,
                MAGIC_FUNCTION_MODE,
            stats.nodesAsked,
            stats.permeationStatsForEachNodeAsked);
    }

    // asks the "expert" what the classification of this datapoint is.
    private int magicFunction(Node datapoint){
        int sum = 0;
        for (int i = 0; i < Node.dimension; i++){
            sum += (int)(datapoint.values[i] * kValueWeights[i]);
        }
        return sum / Node.dimension;
        // Hardcoded Boolean function f1(x)
//        Integer[] x = datapoint.values;

        // HARDCODED f(1) from https://www.researchgate.net/publication/12402645_Consistent_knowledge_discovery_in_medical_diagnosis
        // this is the BIOPSY CASE
//        int result =
//                (x[1] * x[2]) |  // x2x3
//                (x[1] * x[3]) |  // x2x4
//                (x[0] * x[1]) |  // x1x2
//                (x[0] * x[3]) |  // x1x4
//                (x[0] * x[2]) |  // x1x3
//                (x[2] * x[3]) |  // x3x4
//                (x[2])        |  // x3
//                (x[1] * x[4]) |  // x2x5
//                (x[0] * x[4]) |  // x1x5
//                (x[4]);          // x5
//        return result;  // 1 if true, 0 if false


        // HARDCODED f2(x) = x2x3 ∨ x1x2x4 ∨ x1x2 ∨ x1x3x4 ∨ x1x3 ∨ x3x4 ∨ x3 ∨ x2x5 ∨ x1x5 ∨ x4x5
        // THIS IS THE CANCER CASE FROM THAT SAME STUDY ^^^
//        int result =
//            (x[1] * x[2]) |            // x2x3
//            (x[0] * x[1] * x[3]) |     // x1x2x4
//            (x[0] * x[1]) |            // x1x2
//            (x[0] * x[2] * x[3]) |     // x1x3x4
//            (x[0] * x[2]) |            // x1x3
//            (x[2] * x[3]) |            // x3x4
//            (x[2]) |                   // x3
//            (x[1] * x[4]) |            // x2x5
//            (x[0] * x[4]) |            // x1x5
//            (x[3] * x[4]);             // x4x5


        // ψ(x)=x2∨x1∨x3x4x5. the simplified expression from 'Consistent and complete data and "expert” mining in medicine'
//        int result =
//                (x[1]) |            // x2
//                (x[0]) |            // x1
//                (x[2] * x[3] * x[4]); // x3x4x5

//        return result;  // 1 if true, 0 if false
    }
    private Scanner inputScanner;
    private int questionExpert(Node datapoint){

        System.out.println("WHAT IS THE CLASSIFICATION FOR THIS DATAPOINT?");
        System.out.println(Arrays.toString(datapoint.values));
        System.out.println("\tCURRENT MINIMUM:\t" + datapoint.classification);
        System.out.println("\tCURRENT MAXIMUM:\t" + datapoint.maxPossibleValue);
        System.out.println("INPUT:\t");
        int expertInput = inputScanner.nextInt();
        if (expertInput < datapoint.classification || expertInput > datapoint.maxPossibleValue){
            System.out.println("MONOTONICITY VIOLATION!");
            throw new RuntimeException("MONOTONICITY RUINED!"); // use Runtime exception so that it blows up the program. we want it unchecked.
        }
        return expertInput;
    }

    // Sort nodes based on umbrella strategy
    // umbrella strategy considers how many nodes are reachable underneath/above a given node. for example:
    private InterviewStats umbrellaSortInterview(ArrayList<Node> allNodes,
            Comparator<Node> umbrellaSortingStrategy, 
            int numClasses) {

        List<Node> nodesAsked = new ArrayList<>();
        List<PermeationStats> permeationStatsForEachNodeAsked = new ArrayList<>();


        // Instead of sorting every time, just find the best node according to the chosen comparator
        boolean useMin = (umbrellaSortingStrategy == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        ArrayList<Node> nodesToAsk = new ArrayList<>(allNodes);
        // --- Main loop ---
        while (!nodesToAsk.isEmpty()) {

            // if we are sorting by umbrella metrics, sort using our strategy.
            // this is useful if we want to find which node may impact the most other nodes at a given time.
            // go through all the nodes, and update their umbrella sizes.
            Node.updateAllNodeRankings(nodesToAsk);

            Node n = useMin 
            ? Collections.min(nodesToAsk, umbrellaSortingStrategy) 
            : Collections.max(nodesToAsk, umbrellaSortingStrategy);

            // remove the front guy (the chosen one)
            nodesToAsk.remove(n);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed)
                continue;

            // get our value either from expert or ML
            int classification = (MAGIC_FUNCTION_MODE) ? magicFunction(n) : questionExpert(n);

            // this sets all the upper bounds below, and all the lower bounds above.
            PermeationStats stats = n.permeateClassification(classification);
            nodesAsked.add(n);
            permeationStatsForEachNodeAsked.add(stats);

            // filter out confirmed nodes and continue
            nodesToAsk = nodesToAsk.parallelStream()
                .filter(node -> !node.classificationConfirmed)
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return new InterviewStats(nodesAsked, permeationStatsForEachNodeAsked);
    }

    // function where we search through chains, which get recursively split into chunks.
    private InterviewStats binarySearchHCsInterview(ArrayList<ArrayList<Node>> hanselChainSet,
            boolean completingTheSquareTechnique,
            Comparator<Node> choosingAlternateMiddleNodeTechnique,
            int numClasses) {

        List<Node> questionsAsked = new ArrayList<>();
        List<PermeationStats> permeationStats = new ArrayList<>();

        boolean useMaxComparison = (choosingAlternateMiddleNodeTechnique == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a whole bunch with one question, and we have to investigate all the
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>(hanselChainSet);
        while (chunks.size() > 0){

            // get our biggest chunk
            ArrayList<Node> chunkToQuestion = Collections.max(chunks, Comparator.comparingInt(List::size));

            // get the middle node
            int middleIndex = chunkToQuestion.size() / 2;

            // if we are completing the square to find a potential better node than just the middle node.
            Node nodeToQuestion;
            if (completingTheSquareTechnique){
                // now that our nodes have been updated with their umbrella sizes and min classifications and such, we can determine if there is a better node than our middle node.
                nodeToQuestion = getBestSquareCompletion(chunkToQuestion,
                    middleIndex,
                    choosingAlternateMiddleNodeTechnique,
                    useMaxComparison,
                    numClasses);
            }
            else
                nodeToQuestion = chunkToQuestion.get(middleIndex);

            // ask the expert or ML
            int classification = (MAGIC_FUNCTION_MODE)
                ? magicFunction(nodeToQuestion)
                : questionExpert(nodeToQuestion);

            PermeationStats permStats = nodeToQuestion.permeateClassification(classification);
            questionsAsked.add(nodeToQuestion);
            permeationStats.add(permStats);

            // for each chunk we have remaining of each chain, we are going to chop them up, splitting on parts where they are confirmed.
            chunks = chunks.parallelStream()
                .flatMap(chunk -> splitChunkIntoPiecesHelper(chunk).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        }

        return new InterviewStats(questionsAsked, permeationStats);
    }

    // splits a list of nodes (part or whole hansel chain) on nodes which are confirmed
    private ArrayList<ArrayList<Node>> splitChunkIntoPiecesHelper(ArrayList<Node> chunk) {
        ArrayList<ArrayList<Node>> newChunks = new ArrayList<>();
        ArrayList<Node> currentChunk = new ArrayList<>();
    
        for (Node node : chunk) {
            if (node.classificationConfirmed) {
                // End current chunk if we have nodes collected
                if (!currentChunk.isEmpty()) {
                    newChunks.add(new ArrayList<>(currentChunk));
                    currentChunk.clear();
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

    private Node getBestSquareCompletion(
            ArrayList<Node> chunkToQuestion,
            int middleIndex,
            Comparator<Node> choosingAlternateMiddleNodeTechnique,
            boolean useMaxComparison,
            int numClasses) {

        Node middleNode = chunkToQuestion.get(middleIndex);

        Node aboveNode = (middleIndex + 1 >= chunkToQuestion.size())
            ? null
            : chunkToQuestion.get(middleIndex + 1);

        Node belowNode = (middleIndex - 1 < 0)
            ? null
            : chunkToQuestion.get(middleIndex - 1);

        // determine our intersection. if we have above and below, intersect them. if we had one or the other, use just that set.
        ArrayList<Node> intersection = new ArrayList<>();
        if (aboveNode != null && belowNode != null) {
            // filter nulls from expansions
            Set<Node> upSet = Arrays.stream(belowNode.upExpansions)
                                    .filter(Objects::nonNull)
                                    .filter(n -> !n.classificationConfirmed)
                                    .collect(Collectors.toCollection(HashSet::new));

            intersection = Arrays.stream(aboveNode.downExpansions)
                                .filter(Objects::nonNull)
                                .filter(n -> !n.classificationConfirmed)
                                .filter(upSet::contains)
                                .collect(Collectors.toCollection(ArrayList::new));
        } 
        else if (aboveNode != null) {
            intersection = Arrays.stream(aboveNode.downExpansions)
                                .filter(Objects::nonNull)
                                .filter(n -> !n.classificationConfirmed)
                                .collect(Collectors.toCollection(ArrayList::new));
        } 
        else if (belowNode != null) {
            intersection = Arrays.stream(belowNode.upExpansions)
                                .filter(Objects::nonNull)
                                .filter(n -> !n.classificationConfirmed)
                                .collect(Collectors.toCollection(ArrayList::new));
        } 
        else {
            return middleNode;
        }

        // THIS IS VERY IMPORTANT! WE NEED TO UPDATE THE NODE RANKINGS FOR THESE NODES!!!!
        Node.updateAllNodeRankings(intersection);

        // safe min/max selection
        Node selectedNode = useMaxComparison
            ? Collections.max(intersection, choosingAlternateMiddleNodeTechnique)
            : Collections.min(intersection, choosingAlternateMiddleNodeTechnique);

        // check to see if chosen node is the same as the middleNode
        // if it is check to see if there is a up or down exclusive expandable node
        // which is better to ask about
        if(selectedNode.equals(middleNode) && aboveNode != null && belowNode != null) {
            ArrayList<Node> union = Stream.concat(
                    Arrays.stream(aboveNode.downExpansions), Arrays.stream(belowNode.upExpansions))
                    .filter(Objects::nonNull)
                    .filter(n -> !n.classificationConfirmed)
                    .distinct()
                    .filter(Predicate.not(intersection::contains))
                    .collect(Collectors.toCollection(ArrayList::new));
            union.add(selectedNode);
            
            // AGAIN IMPORTANT TO UPDATE THE RANKINGS BEFORE WE COMPARE!!!
            Node.updateAllNodeRankings(union);
            selectedNode = useMaxComparison
                ? Collections.max(union, choosingAlternateMiddleNodeTechnique)
                : Collections.min(union, choosingAlternateMiddleNodeTechnique);
        }

        return selectedNode;
    }

    private InterviewStats binarySearchStringOfExpansionsInterview(ArrayList<Node> allNodes) {
        List<Node> nodesAsked = new ArrayList<>();
        List<PermeationStats> permeationStats = new ArrayList<>();

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

            if (longestChain.isEmpty()) 
                break; // safety

            // Take the middle node of the longest chain
            Node middleNode = longestChain.get(longestChain.size() / 2);

            // Query expert or ML
            int classification = (MAGIC_FUNCTION_MODE)
                ? magicFunction(middleNode)
                : questionExpert(middleNode);

            // Permeate classification
            PermeationStats permeationStatsForNode = middleNode.permeateClassification(classification);
            nodesAsked.add(middleNode);
            permeationStats.add(permeationStatsForNode);
        }

        return new InterviewStats(nodesAsked, permeationStats);
    }

    private InterviewStats nonBinarySearchHCsInterview(ArrayList<ArrayList<Node>> hanselChainSet,
            boolean completingTheSquareTechnique,
            Comparator<Node> choosingAlternateMiddleNodeTechnique,
            int numClasses) {

        List<Node> questionsAsked = new ArrayList<>();
        List<PermeationStats> permeationStats = new ArrayList<>();

        boolean useMaxComparison = (choosingAlternateMiddleNodeTechnique == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a whole bunch with one question, and we have to investigate all the
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>(hanselChainSet);
        while (chunks.size() > 0){

            // get our biggest chunk
            ArrayList<Node> chunkToQuestion = Collections.max(chunks, Comparator.comparingInt(List::size));

            // this part is important. we are going to do a (nunmClassesInChunk - 1)ary search through the chain.
            Node topNode = chunkToQuestion.get(chunkToQuestion.size() - 1);
            Node bottomNode = chunkToQuestion.get(0);
            int highestClassPossibleInChain = topNode.maxPossibleValue;
            int lowestClassPossibleInChiain = bottomNode.classification;
            int totalNumberOfClasses = highestClassPossibleInChain - lowestClassPossibleInChiain + 1;

            // if totalNumberOfClasses = 2, we do a typical binary search. if it is 3, we query the nodes at the 1/3rd mark and 2/3rds marks.
            for(int numerator = 1; numerator < totalNumberOfClasses; numerator++){

                int indexToQuestion = (int) (((double) numerator / (double) totalNumberOfClasses) * (double) chunkToQuestion.size());

                // this is possible if say our first question of the chunk confirms a bunch of nodes up above it.
                Node nodeToQuestion = chunkToQuestion.get(indexToQuestion);
                if (nodeToQuestion.classificationConfirmed)
                    continue;

                if (completingTheSquareTechnique){
                    nodeToQuestion = getBestSquareCompletion(chunkToQuestion,
                            indexToQuestion,
                            choosingAlternateMiddleNodeTechnique,
                            useMaxComparison,
                            numClasses);
                }

                // ask the expert or ML
                int classification = (MAGIC_FUNCTION_MODE)
                        ? magicFunction(nodeToQuestion)
                        : questionExpert(nodeToQuestion);

                PermeationStats permStats = nodeToQuestion.permeateClassification(classification);
                questionsAsked.add(nodeToQuestion);
                permeationStats.add(permStats);
            }

            // for each chunk we have remaining of each chain, we are going to chop them up, splitting on parts where they are confirmed.
            chunks = chunks.parallelStream()
                    .flatMap(chunk -> splitChunkIntoPiecesHelper(chunk).stream())
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return new InterviewStats(questionsAsked, permeationStats);
    }

    // ALL NODES IS ONLY THE UNCONFIRMED NODES AT THIS STEP OF INTERVIEW!
    // This returns the longest possible chain of expansions at this stage of our interview. IMPORTANT: this is NOT a hansel chain, but rather just a chain of Nodes which are + 1 from one another. They could and will be all in different chains all over the place.
    private ArrayList<Node> findLongestStringOfExpansions(List<Node> allNodes) {
        
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
    private InterviewStats bestMinConfirmedInterview(ArrayList<Node> allNodes, int numClasses){
        
        // interview stats we need later.
        List<Node> nodesAsked = new ArrayList<>();
        List<PermeationStats> permeationStatsForEachNodeAsked = new ArrayList<>();

        ArrayList<Node> nodesToAsk = new ArrayList<>(allNodes);
        while(nodesToAsk.size() != 0){

            // now sort decreasing, using our strategy. we sort descending, by a nodes minimum guaranteed classifications.
            // that is, of it's classes, whichever is the worst, we choose the one with the best floor. we are guaranteed to confirm that many at least.
            Node.updateAllNodeRankings(nodesToAsk);
            
            Node nodeToAsk = Collections.max(nodesToAsk, NodeComparisons.BY_MIN_CLASSIFICATIONS);
            nodesToAsk.remove(nodeToAsk);
            nodesAsked.add(nodeToAsk);

            // get our value either from expert or ML
            int classification = (MAGIC_FUNCTION_MODE) ? magicFunction(nodeToAsk) : questionExpert(nodeToAsk);

            // this sets all the upper bounds below, and all the lower bounds above.
            PermeationStats thisNodeStats = nodeToAsk.permeateClassification(classification);
            permeationStatsForEachNodeAsked.add(thisNodeStats);

            nodesToAsk = nodesToAsk.parallelStream()
                .filter(node -> !node.classificationConfirmed) // keep only unconfirmed nodes
                .collect(Collectors.toCollection(ArrayList::new));
        }

        return new InterviewStats(nodesAsked, permeationStatsForEachNodeAsked);
    }

}
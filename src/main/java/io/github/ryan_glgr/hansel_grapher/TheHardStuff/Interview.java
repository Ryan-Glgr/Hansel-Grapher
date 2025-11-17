package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.ryan_glgr.hansel_grapher.FunctionRules.Attribute;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleCreation;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats;

import static java.lang.Math.min;

// this class is where we are going to handle anything classification related.
public class Interview {
    
    // if we set this false, we are going to call upon some ML interviewer instead.
    public static Set<Node> magicLowUnits = null; // set these to make the magicFunction use low units to define function
    private boolean magicFunctionMode = true; // TODO: make this an instace field and then we can specify weights in the GUI.
    private final int highestPossibleClassification; // needed when we are doing these magic function interviews in GUI, since the user may put in less classes than the function wants to make.

    private final Integer[] kVals;
    private final Float[] kValueWeights;
    public final String[] attributeNames;
    public final InterviewStats interviewStats;
    public final HashMap<Integer, Node> data;
    public final ArrayList<ArrayList<Node>> hanselChains;
    public final ArrayList<ArrayList<Node>> lowUnitsByClass;
    public final ArrayList<ArrayList<Node>> adjustedLowUnitsByClass;
    public final RuleNode[] ruleTrees;
    public final RuleNode[][] subFunctionRuleTrees;

    public final String[] classificationNames;
    public final Attribute[] attributes;


    public Interview(Integer[] kVals,
            Float[] weights,
            InterviewMode mode,
            int numClasses,
            String[] attributeNames,
            String[] classificationNames,
            boolean[] subFunctionsEnabledForEachAttribute,
            Interview[] subFunctionsForEachAttribute,
            boolean magicFunctionMode) {

        this.highestPossibleClassification = numClasses - 1;
        this.kVals = kVals;
        this.kValueWeights = weights;
        this.attributeNames = attributeNames;
        this.classificationNames = classificationNames;
        this.inputScanner = new Scanner(System.in);
        this.magicFunctionMode = magicFunctionMode;

        // TODO: enable sub functions. this wouldn't be too hard. basically we would have to at some point get the information from a user
        //       we would need to know which attribute had the sub function, and then what this sub functions values are. once we have that,
        //       we could just run another interview on that sub function. In the rule printing, first we will print sub functions, then
        //       we will show the main functions.
        this.attributes = new Attribute[this.kVals.length];
        for (int index = 0; index < this.kVals.length; index++) {
            this.attributes[index] = new Attribute(
                    this.kVals[index],
                    index,
                    subFunctionsEnabledForEachAttribute[index],
                    subFunctionsForEachAttribute[index],
                    this.attributeNames[index]);
        }

        this.data = Node.makeNodes(this.kVals, numClasses);
        this.hanselChains = HanselChains.generateHanselChainSet(this.kVals, data);

        // this is where the magic happens
        this.interviewStats = conductInterview(mode);

        // once the interview is conducted, we are in the Monotone ordinal function recreation stage:
        this.lowUnitsByClass = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
        this.adjustedLowUnitsByClass = HanselChains.removeUselessLowUnits(lowUnitsByClass);
        this.ruleTrees = RuleCreation.createRuleTrees(adjustedLowUnitsByClass, this.kVals.length);

        // now we need to make our rule trees from each sub function.
        subFunctionRuleTrees = Arrays.stream(subFunctionsForEachAttribute)
                .parallel()
                .map(RuleCreation::createRuleNodesForSubFunctions)
                .toArray(RuleNode[][]::new);
    }

    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    private InterviewStats conductInterview(InterviewMode mode) {

        ArrayList<Node> allNodes = new ArrayList<>(data.values());
        InterviewStats stats = switch(mode) {

            case HIGHEST_TOTAL_UMBRELLA_SORT ->
                umbrellaSortInterview(allNodes, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);

            case SMALLEST_DIFFERENCE_UMBRELLA_SORT ->
                umbrellaSortInterview(allNodes, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

            case BEST_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO);
            }
            case BEST_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO);
            }
            case BEST_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case TRADITIONAL_BINARY_SEARCH ->
                // extra args after hansel chains don't matter for regular binary search.
                    traditionalBinarySearchInterview(hanselChains, false, null);

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE ->
                // we use the MIN for smallest difference
                    traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT ->
                    traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED ->
                    traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS);

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_CHUNKS ->
                // extra args after hansel chains don't matter for regular binary search.
                binarySearchChunksInterview(hanselChains, false, null);

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_SMALLEST_DIFFERENCE ->
                // we use the MIN for smallest difference
                binarySearchChunksInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT ->
                binarySearchChunksInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BEST_MIN_CONFIRMED ->
                binarySearchChunksInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS);

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case NONBINARY_SEARCH_CHAINS ->
                // extra args after hansel chains don't matter for regular binary search.
                    nonBinarySearchHCsInterview(hanselChains, false, null);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE ->
                    // we use the MIN for smallest difference
                    nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT ->
                    nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED ->
                    nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS);

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                Node.BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                Node.BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                Node.BALANCE_RATIO = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS ->
                // our root node is easy to find. it's the node with [0,0,...,0] since our hash function
                // is that value, we can just know that we look up 0 to find it.
                binarySearchStringOfExpansionsInterview(allNodes);

            case BEST_MINIMUM_CONFIRMED ->
                bestMinConfirmedInterview(allNodes);
        };

        return new InterviewStats(kVals,
            hanselChains.size(),
            data.size(),
            mode,
                magicFunctionMode,
            stats.nodesAsked,
            stats.permeationStatsForEachNodeAsked);
    }

    // asks the "expert" what the classification of this datapoint is.
    private int magicFunction(Node datapoint){
        // if low units are used to define the function
        if(magicLowUnits != null) {
            boolean result = true; // false = 1, true = 0 do not be fooled
            for(Node n : magicLowUnits) {
                result &= datapoint.isDominatedBy(n, true);
            }
            return result ? 0 : 1;
        }

        int sum = 0;
        for (int i = 0; i < datapoint.values.length; i++){
            sum += (int)(datapoint.values[i] * kValueWeights[i]);
        }
        // need this because our magic function may try and spit out more classes than the user specifies.
        return min((sum / datapoint.values.length), highestPossibleClassification);

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
    private final Scanner inputScanner;
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
            Comparator<Node> umbrellaSortingStrategy) {

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
            int classification = (magicFunctionMode) ? magicFunction(n) : questionExpert(n);

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
    private InterviewStats binarySearchChunksInterview(ArrayList<ArrayList<Node>> hanselChainSet,
                                                       boolean completingTheSquareTechnique,
                                                       Comparator<Node> choosingAlternateMiddleNodeTechnique) {

        final List<Node> questionsAsked = new ArrayList<>();
        final List<PermeationStats> permeationStats = new ArrayList<>();

        final boolean useMaxComparison = (choosingAlternateMiddleNodeTechnique == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a bunch with one question, and we have to investigate all the
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>(hanselChainSet);
        while (!chunks.isEmpty()){

            // get our biggest chunk
            final ArrayList<Node> chunkToQuestion = Collections.max(chunks, Comparator.comparingInt(List::size));

            // get the middle node
            final int middleIndex = chunkToQuestion.size() / 2;

            // if we are completing the square to find a potential better node than just the middle node.
            final Node nodeToQuestion;
            if (completingTheSquareTechnique){
                // now that our nodes have been updated with their umbrella sizes and min classifications and such, we can determine if there is a better node than our middle node.
                nodeToQuestion = getBestSquareCompletion(chunkToQuestion,
                    middleIndex,
                    choosingAlternateMiddleNodeTechnique,
                    useMaxComparison);
            }
            else
                nodeToQuestion = chunkToQuestion.get(middleIndex);

            // ask the expert or ML
            final int classification = (magicFunctionMode)
                ? magicFunction(nodeToQuestion)
                : questionExpert(nodeToQuestion);

            final PermeationStats permStats = nodeToQuestion.permeateClassification(classification);
            questionsAsked.add(nodeToQuestion);
            permeationStats.add(permStats);

            // for each chunk we have remaining of each chain, we are going to chop them up, splitting on parts where they are confirmed.
            chunks = chunks.parallelStream()
                .flatMap(chunk -> splitChunkIntoPiecesHelper(chunk).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        }

        return new InterviewStats(questionsAsked, permeationStats);
    }

    private InterviewStats traditionalBinarySearchInterview(ArrayList<ArrayList<Node>> hanselChainSet,
                                                            boolean completingTheSquareTechnique,
                                                            Comparator<Node> choosingAlternateMiddleNodeTechnique) {

        final List<Node> questionsAsked = new ArrayList<>();
        final List<PermeationStats> permeationStats = new ArrayList<>();

        final boolean useMaxComparison = (choosingAlternateMiddleNodeTechnique == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a bunch with one question, and we have to investigate all the
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>(hanselChainSet);
        while (!chunks.isEmpty()){

            // get our biggest chunk (piece of Hansel chain)
            ArrayList<Node> chunkToBinarySearch = Collections.max(chunks, Comparator.comparingInt(List::size));

            // we have to keep track of our list of chains individually. so they don't get chopped up until it is time for us to query a particular chain.
            // one particular chain may get chopped into many chunks, each of which you would want to binary search.
            // this starts as a list of list of nodes, but just one list. then it may blow up into several pieces.
            ArrayList<ArrayList<Node>> chainToQuestion = new ArrayList<>();
            chainToQuestion.add(new ArrayList<>(chunkToBinarySearch));

            // while we still have remaining pieces of this chain:
            while(!chainToQuestion.isEmpty()) {

                // for each chunk we have remaining of each chain, we are going to chop them up, splitting on parts where they are confirmed.
                chainToQuestion = chainToQuestion.parallelStream()
                        .flatMap(chunk -> splitChunkIntoPiecesHelper(chunk)
                                .stream())
                        .collect(Collectors.toCollection(ArrayList::new));

                // chain is done.
                if (chainToQuestion.isEmpty())
                    break;

                // get the longest chunk of this HC
                ArrayList<Node> longestPartOfThisChainNotYetConfirmed = Collections.max(chainToQuestion, Comparator.comparingInt(List::size));

                // get the middle node
                final int middleIndex = longestPartOfThisChainNotYetConfirmed.size() / 2;

                // if we are completing the square to find a potential better node than just the middle node.
                final Node nodeToQuestion;
                if (completingTheSquareTechnique){
                    // now that our nodes have been updated with their umbrella sizes and min classifications and such, we can determine if there is a better node than our middle node.
                    nodeToQuestion = getBestSquareCompletion(longestPartOfThisChainNotYetConfirmed,
                            middleIndex,
                            choosingAlternateMiddleNodeTechnique,
                            useMaxComparison);
                }
                else
                    nodeToQuestion = longestPartOfThisChainNotYetConfirmed.get(middleIndex);

                // ask the expert or ML
                int classification = (magicFunctionMode)
                        ? magicFunction(nodeToQuestion)
                        : questionExpert(nodeToQuestion);

                PermeationStats permStats = nodeToQuestion.permeateClassification(classification);
                questionsAsked.add(nodeToQuestion);
                permeationStats.add(permStats);
            }

            // update all our HCs with the results now that we are done with that chain.
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
            boolean useMaxComparison) {

        Node middleNode = chunkToQuestion.get(middleIndex);

        Node aboveNode = (middleIndex + 1 >= chunkToQuestion.size())
            ? null
            : chunkToQuestion.get(middleIndex + 1);

        Node belowNode = (middleIndex - 1 < 0)
            ? null
            : chunkToQuestion.get(middleIndex - 1);

        // determine our intersection. if we have above and below, intersect them. if we had one or the other, use just that set.
        ArrayList<Node> intersection;
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
            int classification = (magicFunctionMode)
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
            Comparator<Node> choosingAlternateMiddleNodeTechnique) {

        List<Node> questionsAsked = new ArrayList<>();
        List<PermeationStats> permeationStats = new ArrayList<>();

        boolean useMaxComparison = (choosingAlternateMiddleNodeTechnique == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a whole bunch with one question, and we have to investigate all the
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>(hanselChainSet);
        while (!chunks.isEmpty()){

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
                            useMaxComparison);
                }

                // ask the expert or ML
                int classification = (magicFunctionMode)
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
            current = Arrays.stream(current.upExpansions)
                .filter(nb -> nb != null && !nb.classificationConfirmed)
                .max(Comparator.comparingInt(nb -> longestPossibleChainOfExpansionsForEachNodeMap.getOrDefault(nb, 0)))
                .orElse(null);
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
    private InterviewStats bestMinConfirmedInterview(ArrayList<Node> allNodes){
        
        // interview stats we need later.
        List<Node> nodesAsked = new ArrayList<>();
        List<PermeationStats> permeationStatsForEachNodeAsked = new ArrayList<>();

        ArrayList<Node> nodesToAsk = new ArrayList<>(allNodes);
        while(!nodesToAsk.isEmpty()){

            // now sort decreasing, using our strategy. we sort descending, by a nodes minimum guaranteed classifications.
            // that is, of it's classes, whichever is the worst, we choose the one with the best floor. we are guaranteed to confirm that many at least.
            Node.updateAllNodeRankings(nodesToAsk);
            
            Node nodeToAsk = Collections.max(nodesToAsk, NodeComparisons.BY_MIN_CLASSIFICATIONS);
            nodesToAsk.remove(nodeToAsk);
            nodesAsked.add(nodeToAsk);

            // get our value either from expert or ML
            int classification = (magicFunctionMode) ? magicFunction(nodeToAsk) : questionExpert(nodeToAsk);

            // this sets all the upper bounds below, and all the lower bounds above.
            PermeationStats thisNodeStats = nodeToAsk.permeateClassification(classification);
            permeationStatsForEachNodeAsked.add(thisNodeStats);

            nodesToAsk = nodesToAsk.parallelStream()
                .filter(node -> !node.classificationConfirmed) // keep only unconfirmed nodes
                .collect(Collectors.toCollection(ArrayList::new));
        }

        return new InterviewStats(nodesAsked, permeationStatsForEachNodeAsked);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(interviewStats.interviewMode)
            .append(" INTERVIEW COMPLETE!\n");
        sb.append("TOTAL NUMBER OF NODES: " + interviewStats.numberOfNodes + "\n");
        sb.append("NUMBER OF QUESTIONS ASKED: ")
            .append(interviewStats.nodesAsked.size())
            .append('\n');

        int totalLowUnits = lowUnitsByClass.stream()
            .mapToInt(List::size)
            .sum();
        sb.append("TOTAL NUMBER OF LOW UNITS:\t")
            .append(totalLowUnits)
            .append('\n');

        for (int classification = 0; classification < lowUnitsByClass.size(); classification++) {
            List<Node> lowUnits = lowUnitsByClass.get(classification);
            sb.append("NUMBER OF LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\t")
                .append(lowUnits.size())
                .append('\n');
            sb.append("LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\n")
                .append(Node.printListOfNodes(lowUnits))
                .append('\n');
        }

        final int totalAdjustedLowUnits = adjustedLowUnitsByClass.stream()
            .mapToInt(List::size)
            .sum();
        sb.append("\nTOTAL NUMBER OF ADJUSTED LOW UNITS:\t")
            .append(totalAdjustedLowUnits)
            .append('\n');

        for (int classification = 0; classification < adjustedLowUnitsByClass.size(); classification++) {
            List<Node> adjustedLowUnits = adjustedLowUnitsByClass.get(classification);
            sb.append("NUMBER OF ADJUSTED LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\t")
                .append(adjustedLowUnits.size())
                .append('\n');
            sb.append("ADJUSTED LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\n")
                .append(Node.printListOfNodes(adjustedLowUnits))
                .append('\n');
        }

        if (ruleTrees != null && ruleTrees.length > 0) {
            sb.append('\n');
            for (int classification = 0; classification < ruleTrees.length; classification++) {
                RuleNode tree = ruleTrees[classification];
                if (tree != null) {
                    sb.append(tree.toString(false, classification));
                }
            }
        }

        final int totalClauses = Arrays.stream(ruleTrees)
            .filter(Objects::nonNull)
            .mapToInt(tree -> tree.getNumberOfClauses(tree))
            .sum();

        sb.append("\nTOTAL NUMBER OF CLAUSES NEEDED:\t")
            .append(totalClauses)
            .append('\n');

        // child functions logic
        for (int attribute = 0; attribute < subFunctionRuleTrees.length; attribute++) {
            RuleNode[] subFunctionRules = subFunctionRuleTrees[attribute];
            if (subFunctionRules == null)
                continue;

            sb.append("------------------------------------------------------\n\n\n");
            sb.append("SUB-FUNCTION FOR " + attributeNames[attribute] + ":\n");
            sb.append(subFunctionRules.toString());
        }
        return sb.toString();
    }

}
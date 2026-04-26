package io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.ryan_glgr.hansel_grapher.functionrules.Attribute;
import io.github.ryan_glgr.hansel_grapher.functionrules.RuleCreation;
import io.github.ryan_glgr.hansel_grapher.functionrules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.stats.PermeationStats;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.BalanceRatio;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.HanselChains;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.NodeComparisons;
import io.github.ryan_glgr.hansel_grapher.helper.Util;
import org.roaringbitmap.RoaringBitmap;

import static java.lang.Math.min;

// this class is where we are going to handle anything classification related.
public class Interview {

    private static final BalanceRatio DEFAULT_BALANCE_RATIO = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;

    // if we set this false, we are going to call upon some ML interviewer instead.
    public final MagicFunctionMode magicFunctionMode;
    public BalanceRatio balanceRatio;

    public final InterviewStats interviewStats;
    public final HashMap<Integer, Node> data;
    public final HashMap<Integer, Node> allNodesToTheirIDsMap; // all the same nodes, but this way we can look up a node by it's ID as well.
    public final ArrayList<ArrayList<Node>> hanselChains;
    public final ArrayList<ArrayList<Node>> lowUnitsByClass;
    public final ArrayList<ArrayList<Node>> adjustedLowUnitsByClass;
    public final RuleNode[] ruleTrees;

    public final int numClasses;
    public final String[] classificationNames;
    public final Attribute[] attributes;
    public final String[] attributeNames;
    public final Integer[] kVals;
    public final Map<Integer, Set<Node>> lowUnitsForEachClassification; // used for the magic function mode when we know what the low units are already, and we are trying to run the interview.

    private final Scanner inputScanner;


    public Interview(final Integer[] kVals,
                     final Float[] weights,                                 // pass in the weights of each attribute. needed IFF you are doing MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE
                     final InterviewMode mode,                              // question asking mode
                     final int numClasses,
                     final String[] attributeNames,
                     final String[] classificationNames,
                     final Map<Integer, Set<Integer[]>> setOfLowUnitsByClassification, // pass in the nodes which are going to satisfy our magic function. NEEDED IFF YOU ARE DOING MagicFunctionMode.KNOWN_LOW_UNITS_MODE!
                     final Interview[] subFunctionsForEachAttribute,       // needs to at least be an Interview[numAttributes], but they can all be null if you want no subfunctions.
                     final Set<Map<Integer, Integer>> impossibleAttributeCombinations, // the combinations of attributes which we are marking as impossible. any node which matches all entries in any one of these maps as >= each value is marked as impossible. in the future we could expand to also use <= and not just >=.
                     final MagicFunctionMode magicFunctionMode) {          // the mode which actually determines how we know a nodes classification
        this.classificationNames = Objects.isNull(classificationNames)
                ? Util.createDefaultClassificationNames(numClasses) : classificationNames;
	    this.attributeNames = Objects.isNull(attributeNames)
                ? Util.createDefaultAttributeNames(kVals.length) : attributeNames;
        this.inputScanner = new Scanner(System.in);
        this.magicFunctionMode = magicFunctionMode;
        this.numClasses = numClasses;

        this.kVals = kVals;
        final int numAttributes = kVals.length;

        this.attributes = IntStream.range(0, numAttributes)
                .mapToObj(index -> new Attribute(
                        kVals[index],
                        index,
                        weights[index],
                        Objects.isNull(subFunctionsForEachAttribute) ? null : subFunctionsForEachAttribute[index],
                        attributeNames[index]))
                .toArray(Attribute[]::new);

        this.data = Node.makeNodes(kVals, numClasses);
        this.allNodesToTheirIDsMap = new HashMap<>();
        for (final Node node : data.values()) {
            allNodesToTheirIDsMap.put(node.nodeID, node);
        }

        this.lowUnitsForEachClassification = getKnownLowUnitsOfEachClassification(setOfLowUnitsByClassification);

        this.hanselChains = HanselChains.generateHanselChainSet(kVals, data);
        Node.markImpossibleNodes(impossibleAttributeCombinations, new ArrayList<>(data.values()));

        // this is where the magic happens
        this.interviewStats = conductInterview(mode);

        // once the interview is conducted, we are in the Monotone ordinal function recreation stage:
        this.lowUnitsByClass = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
        this.adjustedLowUnitsByClass = HanselChains.removeUselessLowUnits(lowUnitsByClass);
        this.ruleTrees = RuleCreation.createRuleTrees(adjustedLowUnitsByClass, numAttributes);

        inputScanner.close();
    }

    private Map<Integer, Set<Node>> getKnownLowUnitsOfEachClassification(final Map<Integer, Set<Integer[]>> setOfLowUnitsByClassification) {

        if (Objects.isNull(setOfLowUnitsByClassification))
            return null;
        return setOfLowUnitsByClassification.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(lowUnit -> data.get(Node.hash(lowUnit)))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ));
    }

    // mega function which determines how we are going to ask questions.
    // mode determines the question asking heuristics. umbrellaBased determines if we sort by umbrella metrics.
    private InterviewStats conductInterview(final InterviewMode mode) {

        final ArrayList<Node> allNodes = new ArrayList<>(data.values());
        final InterviewStats stats = switch(mode) {

            case HIGHEST_TOTAL_UMBRELLA_SORT -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);
            }

            case SMALLEST_DIFFERENCE_UMBRELLA_SORT -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);
            }

            case BEST_BALANCE_RATIO_UNITY -> {
                balanceRatio = BalanceRatio.UNITY_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO);
            }
            case BEST_BALANCE_RATIO_SHANNON_ENTROPY -> {
                balanceRatio = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO);
            }
            case BEST_BALANCE_RATIO_QUADRATIC -> {
                balanceRatio = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield umbrellaSortInterview(allNodes, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case TRADITIONAL_BINARY_SEARCH -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, false, null);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                balanceRatio = BalanceRatio.UNITY_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                balanceRatio = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                balanceRatio = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield traditionalBinarySearchInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_CHUNKS -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, false, null);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_SMALLEST_DIFFERENCE -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BEST_MIN_CONFIRMED -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                balanceRatio = BalanceRatio.UNITY_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                balanceRatio = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                balanceRatio = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield binarySearchChunksInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case NONBINARY_SEARCH_CHAINS -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, false, null);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.HIGHEST_TOTAL_UMBRELLA);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BY_MIN_CLASSIFICATIONS);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY -> {
                balanceRatio = BalanceRatio.UNITY_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY -> {
                balanceRatio = BalanceRatio.SHANNON_ENTROPY_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC -> {
                balanceRatio = BalanceRatio.QUADRATIC_BALANCE_RATIO;
                yield nonBinarySearchHCsInterview(hanselChains, true, NodeComparisons.BEST_BALANCE_RATIO);
            }

            case BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield binarySearchStringOfExpansionsInterview(allNodes);
            }

            case BEST_MINIMUM_CONFIRMED -> {
                balanceRatio = DEFAULT_BALANCE_RATIO;
                yield bestMinConfirmedInterview(allNodes);
            }
        };

        return new InterviewStats(kVals,
            hanselChains.size(),
            data.size(),
            mode,
            magicFunctionMode,
            stats.nodesAsked,
            stats.permeationStatsForEachNodeAsked);
    }

    private int askQuestion(final Node n) {
        return switch (magicFunctionMode) {
            case KVAL_TIMES_WEIGHTS_MODE -> linearFunctionQuestion(n);
            case KNOWN_LOW_UNITS_MODE -> knownLowUnitsQuestion(n);
            case EXPERT_MODE -> questionExpert(n);
        };
    }

    // just returns k values[i] * weights[i] for this node / divided by the dimension to keep the class count in check.
    private int linearFunctionQuestion(final Node datapoint){
        int sum = 0;
        for (int i = 0; i < datapoint.values.length; i++){
            sum += (int)(datapoint.values[i] * attributes[i].weight);
        }
        // need this because our magic function may try and spit out more classes than the user specifies.
        return min((sum / datapoint.values.length), (numClasses - 1));
    }

    // used when we already know the function, and we want to re run the interview.
// useful for recreating results where we know their low units or boolean function.
    private int knownLowUnitsQuestion(final Node datapoint){
        // we can just assume that every node is always going to be at least class 0, by the properties of monotonicity.
        return lowUnitsForEachClassification.entrySet().stream()
                .filter(entry -> entry.getKey() > 0) // skip classification 0
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(lowUnit -> lowUnit.isDominatedBy(datapoint, true)))
                .map(Map.Entry::getKey)
                .max(Integer::compareTo)
                .orElse(0);
    }

    // asks the expert by printing the node and having them enter a datapoint
    private int questionExpert(final Node datapoint){

        System.out.println("WHAT IS THE CLASSIFICATION FOR THIS DATAPOINT?");
        System.out.println(Arrays.toString(datapoint.values));
        System.out.println("\tCURRENT MINIMUM:\t" + datapoint.classification);
        System.out.println("\tCURRENT MAXIMUM:\t" + datapoint.maxPossibleValue);
        System.out.println("INPUT:\t");
        final int expertInput =inputScanner.nextInt();
        if (expertInput < datapoint.classification || expertInput > datapoint.maxPossibleValue){
            System.out.println("MONOTONICITY VIOLATION!");
            throw new RuntimeException("MONOTONICITY RUINED!"); // use Runtime exception so that it blows up the program. we want it unchecked.
        }
        return expertInput;
    }

    // Sort nodes based on umbrella strategy
    // umbrella strategy considers how many nodes are reachable underneath/above a given node. for example:
    private InterviewStats umbrellaSortInterview(final ArrayList<Node> allNodes,
                                                 final Comparator<Node> umbrellaSortingStrategy) {

        final List<Node> nodesAsked = new ArrayList<>();
        final List<PermeationStats> permeationStatsForEachNodeAsked = new ArrayList<>();

        // Instead of sorting every time, just find the best node according to the chosen comparator
        final boolean useMin = (umbrellaSortingStrategy == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        ArrayList<Node> nodesToAsk = new ArrayList<>(allNodes);
        // --- Main loop ---
        while (!nodesToAsk.isEmpty()) {

            final PermeationStats lastUpdate = permeationStatsForEachNodeAsked.isEmpty()
                    ? new PermeationStats(0, 0, 0, new RoaringBitmap(), new RoaringBitmap())
                    : permeationStatsForEachNodeAsked.getLast();

            Node.updateAllNodeRankings(nodesToAsk, this.balanceRatio, this.numClasses, lastUpdate, allNodesToTheirIDsMap);

            final Node n = useMin
                ? Collections.min(nodesToAsk, umbrellaSortingStrategy)
                : Collections.max(nodesToAsk, umbrellaSortingStrategy);

            // remove the front guy (the chosen one)
            nodesToAsk.remove(n);

            // no need to ask about a confirmed node.
            if (n.classificationConfirmed) {
                continue;
            }

            // get our value either from expert or ML
            final int classification = askQuestion(n);

            // this sets all the upper bounds below, and all the lower bounds above.
            final PermeationStats stats = n.permeateClassification(classification);
            nodesAsked.add(n);
            permeationStatsForEachNodeAsked.add(stats);

            final RoaringBitmap nodesConfirmed = stats.nodesConfirmed;
            // filter out confirmed nodes and continue
            nodesToAsk = nodesToAsk.stream()
                .filter(node -> !nodesConfirmed.contains(node.nodeID))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return new InterviewStats(nodesAsked, permeationStatsForEachNodeAsked);
    }

    // function where we search through chains, which get recursively split into chunks.
    private InterviewStats binarySearchChunksInterview(final ArrayList<ArrayList<Node>> hanselChainSet,
                                                       final boolean completingTheSquareTechnique,
                                                       final Comparator<Node> choosingAlternateMiddleNodeTechnique) {

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

                final PermeationStats lastUpdate = permeationStats.isEmpty()
                        ? new PermeationStats(0, 0, 0, new RoaringBitmap(), new RoaringBitmap())
                        : permeationStats.getLast();
                // now that our nodes have been updated with their umbrella sizes and min classifications and such, we can determine if there is a better node than our middle node.
                nodeToQuestion = getBestSquareCompletion(chunkToQuestion,
                    middleIndex,
                    choosingAlternateMiddleNodeTechnique,
                    useMaxComparison,
                    lastUpdate);
            }
            else
                nodeToQuestion = chunkToQuestion.get(middleIndex);

            // ask the expert or ML
            final int classification = askQuestion(nodeToQuestion);

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

    private InterviewStats traditionalBinarySearchInterview(final ArrayList<ArrayList<Node>> hanselChainSet,
                                                            final boolean completingTheSquareTechnique,
                                                            final Comparator<Node> choosingAlternateMiddleNodeTechnique) {

        final List<Node> questionsAsked = new ArrayList<>();
        final List<PermeationStats> permeationStats = new ArrayList<>();

        final boolean useMaxComparison = (choosingAlternateMiddleNodeTechnique == NodeComparisons.SMALLEST_DIFFERENCE_UMBRELLA);

        // we have to keep a list of chunks of the chain which are not confirmed. basically we chop the chain
        // each time that we confirm a node. we could confirm a bunch with one question, and we have to investigate all the
        // chains/chunks to determine that. a chain
        ArrayList<ArrayList<Node>> chunks = new ArrayList<>(hanselChainSet);
        while (!chunks.isEmpty()){

            // get our biggest chunk (piece of Hansel chain)
            final ArrayList<Node> chunkToBinarySearch = Collections.max(chunks, Comparator.comparingInt(List::size));

            // we have to keep track of our list of chains individually. so they don't get chopped up until it is time for us to query a particular chain.
            // one particular chain may get chopped into many chunks, each of which you would want to binary search.
            // this starts as a list of list of nodes, but just one list. then it may blow up into several pieces.
            ArrayList<ArrayList<Node>> chainToQuestion = new ArrayList<>();
            chainToQuestion.add(new ArrayList<>(chunkToBinarySearch));

            // while we still have remaining pieces of this chain:
            while(!chainToQuestion.isEmpty()) {

                // for each chunk we have remaining of each chain, we are going to chop them up, splitting on parts where they are confirmed.
                chainToQuestion = chainToQuestion.stream()
                        .flatMap(chunk -> splitChunkIntoPiecesHelper(chunk)
                                .stream())
                        .collect(Collectors.toCollection(ArrayList::new));

                // chain is done.
                if (chainToQuestion.isEmpty())
                    break;

                // get the longest chunk of this HC
                final ArrayList<Node> longestPartOfThisChainNotYetConfirmed = Collections.max(chainToQuestion, Comparator.comparingInt(List::size));

                // get the middle node
                final int middleIndex = longestPartOfThisChainNotYetConfirmed.size() / 2;

                // if we are completing the square to find a potential better node than just the middle node.
                final Node nodeToQuestion;
                if (completingTheSquareTechnique){

                    final PermeationStats lastUpdate = permeationStats.isEmpty()
                            ? new PermeationStats(0, 0, 0, new RoaringBitmap(), new RoaringBitmap())
                            : permeationStats.getLast();

                    // now that our nodes have been updated with their umbrella sizes and min classifications and such, we can determine if there is a better node than our middle node.
                    nodeToQuestion = getBestSquareCompletion(longestPartOfThisChainNotYetConfirmed,
                            middleIndex,
                            choosingAlternateMiddleNodeTechnique,
                            useMaxComparison,
                            lastUpdate);
                }
                else
                    nodeToQuestion = longestPartOfThisChainNotYetConfirmed.get(middleIndex);

                // ask the expert or ML
                final int classification = askQuestion(nodeToQuestion);
                final PermeationStats permStats = nodeToQuestion.permeateClassification(classification);
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
    private ArrayList<ArrayList<Node>> splitChunkIntoPiecesHelper(final ArrayList<Node> chunk) {
        final ArrayList<ArrayList<Node>> newChunks = new ArrayList<>();
        final ArrayList<Node> currentChunk = new ArrayList<>();
    
        for (final Node node : chunk) {
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
            final ArrayList<Node> chunkToQuestion,
            final int middleIndex,
            final Comparator<Node> choosingAlternateMiddleNodeTechnique,
            final boolean useMaxComparison,
            final PermeationStats lastUpdate) {

        final Node middleNode = chunkToQuestion.get(middleIndex);

        final Node aboveNode = (middleIndex + 1 >= chunkToQuestion.size())
            ? null
            : chunkToQuestion.get(middleIndex + 1);

        final Node belowNode = (middleIndex - 1 < 0)
            ? null
            : chunkToQuestion.get(middleIndex - 1);

        // determine our intersection. if we have above and below, intersect them. if we had one or the other, use just that set.
        final ArrayList<Node> intersection;
        if (aboveNode != null && belowNode != null) {
            // filter nulls from expansions
            final Set<Node> upSet = Arrays.stream(belowNode.upExpansions)
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
        Node.updateAllNodeRankings(intersection, this.balanceRatio, this.numClasses, lastUpdate, allNodesToTheirIDsMap);

        // safe min/max selection
        Node selectedNode = useMaxComparison
            ? Collections.max(intersection, choosingAlternateMiddleNodeTechnique)
            : Collections.min(intersection, choosingAlternateMiddleNodeTechnique);

        // check to see if chosen node is the same as the middleNode
        // if it is check to see if there is a up or down exclusive expandable node
        // which is better to ask about
        if(selectedNode.equals(middleNode) && aboveNode != null && belowNode != null) {
            final ArrayList<Node> union = Stream.concat(
                    Arrays.stream(aboveNode.downExpansions), Arrays.stream(belowNode.upExpansions))
                    .filter(Objects::nonNull)
                    .filter(n -> !n.classificationConfirmed)
                    .distinct()
                    .filter(Predicate.not(intersection::contains))
                    .collect(Collectors.toCollection(ArrayList::new));
            union.add(selectedNode);

            // AGAIN IMPORTANT TO UPDATE THE RANKINGS BEFORE WE COMPARE!!!
            Node.updateAllNodeRankings(union, this.balanceRatio, this.numClasses, lastUpdate, allNodesToTheirIDsMap);
            selectedNode = useMaxComparison
                ? Collections.max(union, choosingAlternateMiddleNodeTechnique)
                : Collections.min(union, choosingAlternateMiddleNodeTechnique);
        }

        return selectedNode;
    }

    private InterviewStats binarySearchStringOfExpansionsInterview(final ArrayList<Node> allNodes) {
        final List<Node> nodesAsked = new ArrayList<>();
        final List<PermeationStats> permeationStats = new ArrayList<>();

        while (true) {
            // Collect still alive nodes
            final List<Node> aliveNodes = allNodes.stream()
                .filter(n -> !n.classificationConfirmed)
                .collect(Collectors.toList());

            if (aliveNodes.isEmpty()) 
                break;

            // Find the longest chain among alive nodes.
            // NOT a hansel chain necessarily. Thought it could be technically a hansel chain?
            // but just a string of Nodes which are all + 1 in some attribute from another.
            // just the longest string of dominoes.
            final ArrayList<Node> longestChain = findLongestStringOfExpansions(aliveNodes);

            if (longestChain.isEmpty()) 
                break; // safety

            // Take the middle node of the longest chain
            final Node middleNode = longestChain.get(longestChain.size() / 2);

            // Query expert or ML
            final int classification = askQuestion(middleNode);
            // Permeate classification
            final PermeationStats permeationStatsForNode = middleNode.permeateClassification(classification);
            nodesAsked.add(middleNode);
            permeationStats.add(permeationStatsForNode);
        }

        return new InterviewStats(nodesAsked, permeationStats);
    }

    private InterviewStats nonBinarySearchHCsInterview(final ArrayList<ArrayList<Node>> hanselChainSet,
                                                       final boolean completingTheSquareTechnique,
                                                       final Comparator<Node> choosingAlternateMiddleNodeTechnique) {

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

            // this part is important. we are going to do a (nunmClassesInChunk - 1)ary search through the chain.
            final Node topNode = chunkToQuestion.getLast();
            final Node bottomNode = chunkToQuestion.getFirst();
            final int highestClassPossibleInChain = topNode.maxPossibleValue;
            final int lowestClassPossibleInChiain = bottomNode.classification;
            final int totalNumberOfClasses = highestClassPossibleInChain - lowestClassPossibleInChiain + 1;

            // if totalNumberOfClasses = 2, we do a typical binary search. if it is 3, we query the nodes at the 1/3rd mark and 2/3rds marks.
            for(int numerator = 1; numerator < totalNumberOfClasses; numerator++){

                final int indexToQuestion = (int) (((double) numerator / (double) totalNumberOfClasses) * (double) chunkToQuestion.size());

                // this is possible if say our first question of the chunk confirms a bunch of nodes up above it.
                Node nodeToQuestion = chunkToQuestion.get(indexToQuestion);
                if (nodeToQuestion.classificationConfirmed)
                    continue;

                if (completingTheSquareTechnique){

                    final PermeationStats lastUpdate = permeationStats.isEmpty()
                            ? new PermeationStats(0, 0, 0, new RoaringBitmap(), new RoaringBitmap())
                            : permeationStats.getLast();

                    nodeToQuestion = getBestSquareCompletion(chunkToQuestion,
                            indexToQuestion,
                            choosingAlternateMiddleNodeTechnique,
                            useMaxComparison,
                            lastUpdate);
                }

                // ask the expert or ML
                final int classification = askQuestion(nodeToQuestion);

                final PermeationStats permStats = nodeToQuestion.permeateClassification(classification);
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
    private ArrayList<Node> findLongestStringOfExpansions(final List<Node> allNodes) {
        
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
        final Map<Node, Integer> longestPossibleChainOfExpansionsForEachNodeMap = new HashMap<>();

        // Step 1: initialize leaves (no unconfirmed upstairs neighbors) with a length of 1.
        for (final Node n : allNodes) {
            final boolean isTerminal = Arrays.stream(n.upExpansions)
                .allMatch(nb -> nb == null || nb.classificationConfirmed);
            if (isTerminal) {
                longestPossibleChainOfExpansionsForEachNodeMap.put(n, 1);
            }
        }

        // Step 2: iteratively update each node's longest length, until we have no progress.
        boolean progress = true;
        while (progress) {
            progress = false;

            for (final Node n : allNodes) {

                final int bestNeighbor = Arrays.stream(n.upExpansions)
                    .filter(neighbor -> neighbor != null && !neighbor.classificationConfirmed)
                    .mapToInt(neighbor -> longestPossibleChainOfExpansionsForEachNodeMap.getOrDefault(neighbor, 0))
                    .max()
                    .orElse(0);

                // if our best neighbor is not 0, we check if it is better than what we had.
                final int newVal = (bestNeighbor > 0) ? bestNeighbor + 1 : 0;
                final int oldVal = longestPossibleChainOfExpansionsForEachNodeMap.getOrDefault(n, 0);

                // if our new value is better than what we had, put that in.
                if (newVal > oldVal) {
                    longestPossibleChainOfExpansionsForEachNodeMap.put(n, newVal);
                    progress = true;
                }
            }
        }

        // Step 3: pick starting node with the max value from our input list.
        final Node bestNode = longestPossibleChainOfExpansionsForEachNodeMap.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);

        // if we didn't have one just get out of here.
        if (bestNode == null) 
            return new ArrayList<>();

        // Step 4: reconstruct path greedily
        final ArrayList<Node> path = new ArrayList<>();
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
    private InterviewStats bestMinConfirmedInterview(final ArrayList<Node> allNodes){
        
        // interview stats we need later.
        final List<Node> nodesAsked = new ArrayList<>();
        final List<PermeationStats> permeationStatsForEachNodeAsked = new ArrayList<>();

        ArrayList<Node> nodesToAsk = new ArrayList<>(allNodes);
        while(!nodesToAsk.isEmpty()){

            // now sort decreasing, using our strategy. we sort descending, by a nodes minimum guaranteed classifications.
            // that is, of it's classes, whichever is the worst, we choose the one with the best floor. we are guaranteed to confirm that many at least.
            final PermeationStats lastUpdate = permeationStatsForEachNodeAsked.isEmpty()
                ? new PermeationStats(0, 0, 0, new RoaringBitmap(), new RoaringBitmap())
                : permeationStatsForEachNodeAsked.getLast();
            Node.updateAllNodeRankings(nodesToAsk, this.balanceRatio, this.numClasses, lastUpdate, allNodesToTheirIDsMap);

            final Node nodeToAsk = Collections.max(nodesToAsk, NodeComparisons.BY_MIN_CLASSIFICATIONS);

            // get our value either from expert or ML
            final int classification = askQuestion(nodeToAsk);

            // this sets all the upper bounds below, and all the lower bounds above.
            final PermeationStats thisNodeStats = nodeToAsk.permeateClassification(classification);
            nodesToAsk.remove(nodeToAsk);
            nodesAsked.add(nodeToAsk);
            permeationStatsForEachNodeAsked.add(thisNodeStats);

            nodesToAsk = nodesToAsk.stream()
                .filter(node -> !node.classificationConfirmed) // keep only unconfirmed nodes
                .collect(Collectors.toCollection(ArrayList::new));
        }

        return new InterviewStats(nodesAsked, permeationStatsForEachNodeAsked);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(interviewStats.interviewMode).append(" MODE\n");
        sb.append("TOTAL NUMBER OF NODES: ").append(interviewStats.numberOfNodes).append("\n");
        sb.append("NUMBER OF QUESTIONS ASKED: ")
            .append(interviewStats.nodesAsked.size())
            .append('\n');

        final int totalLowUnits = lowUnitsByClass.stream()
            .mapToInt(List::size)
            .sum();
        sb.append("TOTAL NUMBER OF LOW UNITS:\t")
            .append(totalLowUnits)
            .append('\n');

        for (int classification = 0; classification < lowUnitsByClass.size(); classification++) {
            final List<Node> lowUnits = lowUnitsByClass.get(classification);
            sb.append("NUMBER OF LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\t")
                .append(lowUnits.size())
                .append('\n');
            sb.append("LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\n")
                .append(Util.printListOfNodes(lowUnits))
                .append('\n');
        }

        final int totalAdjustedLowUnits = adjustedLowUnitsByClass.stream()
            .mapToInt(List::size)
            .sum();
        sb.append("\nTOTAL NUMBER OF ADJUSTED LOW UNITS:\t")
            .append(totalAdjustedLowUnits)
            .append('\n');

        for (int classification = 0; classification < adjustedLowUnitsByClass.size(); classification++) {
            final List<Node> adjustedLowUnits = adjustedLowUnitsByClass.get(classification);
            sb.append("NUMBER OF ADJUSTED LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\t")
                .append(adjustedLowUnits.size())
                .append('\n');
            sb.append("ADJUSTED LOW UNITS FOR CLASS ")
                .append(classification)
                .append(":\n")
                .append(Util.printListOfNodes(adjustedLowUnits))
                .append('\n');
        }

        if (ruleTrees != null && ruleTrees.length > 0) {
            sb.append('\n');
            for (int classification = 0; classification < ruleTrees.length; classification++) {
                final RuleNode tree = ruleTrees[classification];
                if (tree != null) {
                    sb.append(tree.toString(false, classification));
                }
            }
        }

        final int totalClauses = Arrays.stream(ruleTrees)
            .filter(Objects::nonNull)
            .mapToInt(RuleNode::getNumberOfClauses)
            .sum();

        sb.append("\nTOTAL NUMBER OF CLAUSES NEEDED:\t")
            .append(totalClauses)
            .append('\n');

        Arrays.stream(attributes)
                .filter(attribute -> attribute.subFunction != null)
                .forEach(attribute -> {
                    sb.append("------------------------------------------------------\n\n\n");
                    sb.append("SUB-FUNCTION FOR ").append(attribute.name).append(":\n");
                    sb.append(attribute.subFunction);
                });

        return sb.toString();
    }

}

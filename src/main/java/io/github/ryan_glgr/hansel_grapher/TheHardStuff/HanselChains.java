package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HanselChains{
    
    // Thread pool for parallel operations
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    // function to create our chains
    public static ArrayList<ArrayList<Node>> generateHanselChainSet(
            Integer[] kValues, HashMap<Integer, Node> nodes) {

        ArrayList<ArrayList<Node>> hanselChainSet = new ArrayList<>();

        // initialize valsForChains with zeros
        Integer[] valsForChains = new Integer[kValues.length];
        Arrays.fill(valsForChains, 0);

        // create the first chain (varying only the first digit)
        ArrayList<Node> baseChain = new ArrayList<>();
        for (int firstDigitVal = 0; firstDigitVal < kValues[0]; firstDigitVal++) {
            valsForChains[0] = firstDigitVal;
            baseChain.add(nodes.get(Node.hash(valsForChains)));
        }
        hanselChainSet.add(baseChain);

        // expand dimension by dimension
        for (int digit = 1; digit < kValues.length; digit++) {
            final int digitFinalBecauseJavaSucks = digit;
            hanselChainSet = hanselChainSet
                .parallelStream()
                .flatMap(chain -> copyChainAndAdjustCopies(nodes, 
                    chain, 
                    digitFinalBecauseJavaSucks, 
                    kValues[digitFinalBecauseJavaSucks])
                .stream())
                .collect(Collectors.toCollection(ArrayList::new));
        }

        // final validation
        for (ArrayList<Node> chain : hanselChainSet) {
            if (!checkValidChain(chain)) {
                System.out.println("ERROR: Chain is broken: " + chain.toString());
            }
        }

        return hanselChainSet;
    }

    private static ArrayList<ArrayList<Node>> copyChainAndAdjustCopies(HashMap<Integer, Node> nodes, ArrayList<Node> original, int digit, int kValue) {

        ArrayList<ArrayList<Node>> group = new ArrayList<>();

        // add the original chain (already has digit=0 case)
        group.add(original);

        // make copies for values 1..kValue-1
        for (int val = 1; val < kValue; val++) {
            ArrayList<Node> copy = copyChainWithDigitValue(nodes, original, digit, val);
            group.add(copy);
        }

        // adjust group locally
        adjustEndsOfIsomorphicChains(group);

        return group;
    }

    // the recursive step. you copy all the values of a chain, but append your given digit to the front of each node.
    private static ArrayList<Node> copyChainWithDigitValue(HashMap<Integer, Node> nodes,ArrayList<Node> chainToCopy, int currentDigit, int currentDigitVal) {

        ArrayList<Node> newChain = new ArrayList<>(chainToCopy.size());
        for (Node t : chainToCopy) {
            Integer[] newVals = Arrays.copyOf(t.values, t.values.length);
            newVals[currentDigit] = currentDigitVal;
            Node temp = nodes.get(Node.hash(newVals));
            newChain.add(temp);
        }
        return newChain;
    }

    // takes the end of one isomorphic chain, and moves it to the end of the next isomorphic chain. for example [0,0] - [0,1] gets [1,1] from the chain [1,0] - [1,1]..
    private static void adjustEndsOfIsomorphicChains(ArrayList<ArrayList<Node>> group) {
        int n = group.size();
        if (n < 2) return;

        // For each chain, take the top nodes from all chains after it and add them to this chain
        for (int i = 0; i < n - 1; i++) {
            ArrayList<Node> receivingChain = group.get(i);
            for (int j = i + 1; j < n; j++) {
                ArrayList<Node> donatingChain = group.get(j);
                if (!donatingChain.isEmpty()) {
                    Node topNode = donatingChain.remove(donatingChain.size() - 1);
                    receivingChain.add(topNode);
                }
            }
        }

        // Remove any chains that became empty
        group.removeIf(List::isEmpty);
    }

    // simple helper which checks that all the nodes of a chain have a hamming distance of + 1 from the next.
    private static boolean checkValidChain(ArrayList<Node> chain) {

        for (int lowerNode = 0; lowerNode < chain.size() - 1; lowerNode++) {
            int upperNode = lowerNode + 1;

            // Use the Node instance method to compute the Hamming distance
            int hammingDistance = chain.get(lowerNode).computeHammingDistance(chain.get(upperNode));
            
            if (hammingDistance != 1) 
                return false;
        }
        return true;
    }
    
    // takes in our fully classified data, and just finds the low units in each chain.
    public static ArrayList<ArrayList<Node>> findLowUnitsForEachClass(ArrayList<ArrayList<Node>> hanselChainSet, int numClasses) {
        
        ArrayList<ArrayList<Node>> lowUnits = new ArrayList<>();
        for (int i = 0; i < numClasses; i++) {
            lowUnits.add(new ArrayList<>());
        }

        for(ArrayList<Node> chain : hanselChainSet){
            Node[] lowestNodeInEachClassInsideThisChain = new Node[numClasses];
            
            // take the FIRST occurence of each class in the chain as the low unit for the
            for (Node node : chain) {
                if (lowestNodeInEachClassInsideThisChain[node.classification] == null) {
                    lowestNodeInEachClassInsideThisChain[node.classification] = node;
                }
            }
            
            // add the lowest node of each class to our set of low units.
            for(Node lowUnit : lowestNodeInEachClassInsideThisChain){
                if(lowUnit != null){
                    // get this particular low unit's class, and add it to that classes list of low units.
                    lowUnits.get(lowUnit.classification).add(lowUnit);
                }
            }
        }

        lowUnits.forEach(list -> list.sort(LEXICOGRAPHIC_NODE_COMPARATOR));
        return lowUnits;
    }

    public static ArrayList<ArrayList<Node>> removeUselessLowUnits(ArrayList<ArrayList<Node>> lowUnits) {

        ArrayList<ArrayList<Node>> newLowUnits = new ArrayList<>();
        for (int i = 0; i < lowUnits.size(); i++) {
            newLowUnits.add(new ArrayList<>());
        }

        boolean dominatingInAnUpwardFashion = true; // meaning that "otherNode" is dominating from the top...                                                                                   pause

        for (int classification = 0; classification < lowUnits.size(); classification++) {
            // add all the low units for this class
            newLowUnits.get(classification).addAll(lowUnits.get(classification));

            HashSet<Node> nodesWhichAreNotNeeded = new HashSet<>();

            // for each node, if it is totally dominated by any of the other nodes, we know that we can remove that other node which is dominating us.
            for (Node node : lowUnits.get(classification)) {
                for (Node otherNode : lowUnits.get(classification)) {
                    if (node != otherNode) {
                        if (node.isDominatedBy(otherNode, dominatingInAnUpwardFashion)) {
                            nodesWhichAreNotNeeded.add(otherNode);
                        }
                    }
                }
            }

            // go backwards through the list so we can safely remove by index
            for (int nodePosition = newLowUnits.get(classification).size() - 1; nodePosition >= 0; nodePosition--) {
                // if this node has been marked as "dominating" another node, we can remove it. it's not really a low unit. just a low unit in it's own chain.
                Node node = newLowUnits.get(classification).get(nodePosition);

                if (nodesWhichAreNotNeeded.contains(node)) {
                    newLowUnits.get(classification).remove(nodePosition);
                }
            }
        }
        lowUnits.forEach(list -> list.sort(LEXICOGRAPHIC_NODE_COMPARATOR));
        return newLowUnits;
    }

    // Compare nodes lexicographically by their attribute arrays
    private static final Comparator<Node> LEXICOGRAPHIC_NODE_COMPARATOR = (a, b) -> {
        Integer[] A = a.values;
        Integer[] B = b.values;
        for (int i = 0; i < A.length; i++) {
            int cmp = Integer.compare(A[i], B[i]);
            
            if (cmp != 0) 
                return cmp;
        }
        return 0;
    };


    // Clean up thread pool when done
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

}
package io.github.ryan_glgr.hansel_grapher.functionallogic;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HanselChains{
    
    // function to create our chains
    public static ArrayList<ArrayList<Node>> generateHanselChainSet(
            final Integer[] kValues, final HashMap<Integer, Node> nodes) {

        ArrayList<ArrayList<Node>> hanselChainSet = new ArrayList<>();

        // initialize valsForChains with zeros
        final Integer[] valsForChains = new Integer[kValues.length];
        Arrays.fill(valsForChains, 0);

        // create the first chain (varying only the first digit)
        final ArrayList<Node> baseChain = new ArrayList<>();
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
                .flatMap(chain -> copyChainAndAdjustCopies(
                    nodes,
                    chain, 
                    digitFinalBecauseJavaSucks, 
                    kValues[digitFinalBecauseJavaSucks])
                .stream())
                .collect(Collectors.toCollection(ArrayList::new));
        }

        // final validation
        hanselChainSet.forEach(chain -> assertTrue(checkValidChain(chain)));

        return hanselChainSet;
    }

    private static ArrayList<ArrayList<Node>> copyChainAndAdjustCopies(final HashMap<Integer, Node> nodes, final ArrayList<Node> original, final int digit, final int kValue) {

        final ArrayList<ArrayList<Node>> group = new ArrayList<>();

        // add the original chain (already has digit=0 case)
        group.add(original);

        // make copies for values 1..kValue-1
        for (int val = 1; val < kValue; val++) {
            final ArrayList<Node> copy = copyChainWithDigitValue(nodes, original, digit, val);
            group.add(copy);
        }

        // adjust group locally
        adjustEndsOfIsomorphicChains(group);

        return group;
    }

    // the recursive step. you copy all the values of a chain, but append your given digit to the front of each node.
    private static ArrayList<Node> copyChainWithDigitValue(final HashMap<Integer, Node> nodes, final ArrayList<Node> chainToCopy, final int currentDigit, final int currentDigitVal) {

        final ArrayList<Node> newChain = new ArrayList<>(chainToCopy.size());
        for (final Node t : chainToCopy) {
            final Integer[] newVals = Arrays.copyOf(t.values, t.values.length);
            newVals[currentDigit] = currentDigitVal;
            final Node temp = nodes.get(Node.hash(newVals));
            newChain.add(temp);
        }
        return newChain;
    }

    // takes the end of one isomorphic chain, and moves it to the end of the next isomorphic chain. for example [0,0] - [0,1] gets [1,1] from the chain [1,0] - [1,1]..
    private static void adjustEndsOfIsomorphicChains(final ArrayList<ArrayList<Node>> group) {
        final int n = group.size();
        if (n < 2) return;

        // For each chain, take the top nodes from all chains after it and add them to this chain
        for (int i = 0; i < n - 1; i++) {
            final ArrayList<Node> receivingChain = group.get(i);
            for (int j = i + 1; j < n; j++) {
                final ArrayList<Node> donatingChain = group.get(j);
                if (!donatingChain.isEmpty()) {
                    final Node topNode = donatingChain.removeLast();
                    receivingChain.add(topNode);
                }
            }
        }

        // Remove any chains that became empty
        group.removeIf(List::isEmpty);
    }

    // simple helper which checks that all the nodes of a chain have a hamming distance of + 1 from the next.
    private static boolean checkValidChain(final ArrayList<Node> chain) {

        for (int lowerNode = 0; lowerNode < chain.size() - 1; lowerNode++) {
            final int upperNode = lowerNode + 1;

            // Use the Node instance method to compute the Hamming distance
            final int hammingDistance = chain.get(lowerNode).computeHammingDistance(chain.get(upperNode));
            
            if (hammingDistance != 1) 
                return false;
        }
        return true;
    }

}
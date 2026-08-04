package io.github.ryan_glgr.hansel_grapher.experimental;

import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.functionallogic.HanselChains;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.MagicFunctionMode;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnitsFactory;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;

import java.util.*;
import java.util.stream.Collectors;

public class ExperimentalFunctionalities {
    public static ArrayList<ArrayList<Node>> duplicateChain(final ArrayList<ArrayList<Node>> chains) {
        final ArrayList<ArrayList<Node>> dupChains = new ArrayList<>(chains.size());
        for (final ArrayList<Node> chain : chains) {
            final ArrayList<Node> dupChain = new ArrayList<Node>(chain.size());
            for (final Node n : chain) {
                dupChain.add(new Node(n));
            }
            dupChains.add(dupChain);
        }
        return dupChains;
    }

    public static void generateChains(final Integer[] kVals, final int numClasses) {
        final HashMap<Integer, Node> data = Node.makeNodes(kVals, numClasses);
        ArrayList<ArrayList<Node>> hanselChains;
        ArrayList<ArrayList<Node>> defaultChains = HanselChains.generateHanselChainSet(kVals, data);
        final HashSet<Set<LowUnit>> lowUnits = new HashSet<>();
        final int[] sizes = defaultChains.stream().mapToInt(ArrayList::size).toArray();
        final int[] lowValueIndices = new int[sizes.length];
        final MagicFunctionMode magicFunctionMode = MagicFunctionMode.KNOWN_LOW_UNITS_MODE;

        long count = 0;

        while (true) {
            int index = sizes.length - 1;
            while (index >= 0) {
                lowValueIndices[index]++;

                if (lowValueIndices[index] <= sizes[index]) {
                    hanselChains = defaultChains;
                    defaultChains = duplicateChain(hanselChains);
                    hanselChains.getFirst().getLast().permeateClassification(0);
                    count++;
                    System.out.println(Arrays.toString(lowValueIndices));
                    for(int i = 0; i < lowValueIndices.length; i++) {
                        if(lowValueIndices[i] > 0) {
                            hanselChains.get(i).get(lowValueIndices[i] - 1).permeateClassification(1);
                        }
                    }
                    final var lowUnitsByClass = LowUnitsFactory.findPrunedLowUnits(hanselChains);
                    lowUnits.add(lowUnitsByClass.get(1));
                    // for(Node n : adjustedLowUnitsByClass.get(1)) {
                    //     System.out.print(Arrays.toString(n.values));
                    //     System.out.print(" ");
                    // }
                    // System.out.println();
                    break;
                } else {
                    lowValueIndices[index] = 0;
                    index--;
                }
            }
            if (index < 0) {
                break;
            }
        }
        System.out.println(lowUnits.size());

        final Float[] fakeWeights = new Float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        final String[] fakeNames = new String[]{"","","","",""};
        final Interview[] subFunctionsForEachAttribute = new Interview[kVals.length];
        final InterviewMode[] modes = InterviewMode.values();
        final float[] questions = new float[modes.length];
        count = 0;

        for(final Set<LowUnit> nodes: lowUnits) {
            final Map<Integer, Set<Integer[]>> lowUnitsToMakeTheFunctionTrue = Map.of(1,
                    nodes.stream()
                    .map(lowUnit -> lowUnit.getDatapoint().values)
                    .collect(Collectors.toSet()));

            final Map<Integer, Set<Integer[]>> lowUnitsToMakeTheFunctionTrueByClass = new HashMap<>();
            for (final LowUnit lowUnit : nodes) {
                Set<Integer[]> lowUnitsForThisNodesClass = lowUnitsToMakeTheFunctionTrueByClass.get(lowUnit.getDatapoint().classification);
                if (Objects.isNull(lowUnitsForThisNodesClass)) {
                    lowUnitsToMakeTheFunctionTrueByClass.put(lowUnit.getDatapoint().classification, new HashSet<>());
                    lowUnitsForThisNodesClass = lowUnitsToMakeTheFunctionTrueByClass.get(lowUnit.getDatapoint().classification);
                }
                lowUnitsForThisNodesClass.add(lowUnit.getDatapoint().values);
            }

            for(int i = 0; i < modes.length; i++) {
                final Interview interview = new Interview(kVals,
                    fakeWeights,
                    numClasses,
                    fakeNames,
                    fakeNames,
                    lowUnitsToMakeTheFunctionTrue,
                    subFunctionsForEachAttribute,
                    null,
                    magicFunctionMode,
                        null,
                        null);
                interview.beginInterview(modes[i]);

                final InterviewStats interviewStats = interview.interviewStats;
                questions[i] += interviewStats.nodesAsked.size();
            }
            count++;
            System.out.printf("%d/%d\n", count, lowUnits.size());
        }

        for(int i = 0; i < modes.length; i++) {
            questions[i] /= (float)lowUnits.size();
            System.out.println(modes[i].toString() + ": " + questions[i]);
        }
    }

    /*
     * Structure of Impossible attribute combinations as follows. Pass a set of Maps. Each map represents a combination of k values which is impossible.
     * We assume anything >= each attribute in a map is impossible. for example i may make a map with entries 0: 2, and 1: 0,
     * this means that attribute (x0 >= 2 AND x1 >= 0) is an IMPOSSIBLE combination. And any node which satisfies x0 >=2
     * AND x1 >= 0 is an IMPOSSIBLE combination.
     */
    public static void markImpossibleNodes(final Set<Map<Integer, Integer>> impossibleAttributeCombinations, final ArrayList<Node> nodes) {
        if (Objects.isNull(impossibleAttributeCombinations))
            return;

        nodes.parallelStream()
                .filter(node -> nodeSatisfiesImpossibleAttributeCombination(node, impossibleAttributeCombinations))
                .forEach(node -> {
                    node.classification = Node.IMPOSSIBLE_CLASSIFICATION;
                    node.classificationConfirmed = true;
                });
    }

    private static boolean nodeSatisfiesImpossibleAttributeCombination(final Node targetNode, final Set<Map<Integer, Integer>> impossibleAttributeCombinations) {
        // if all the entries in a map are satisfied, that means we have satisfied some impossible combination of attributes.
        return impossibleAttributeCombinations.stream().anyMatch(impossibleAttributeCombination -> impossibleAttributeCombination
                .entrySet()
                .stream()
                // key of entry is the attribute index, value is the impossible combo. So if all match the predicate,
                // this node is >= the combination we said is impossible.
                .allMatch(entry ->
                        targetNode.values[entry.getKey()] >= entry.getValue()));
    }
}

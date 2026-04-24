package io.github.ryan_glgr.hansel_grapher.experimental;

import io.github.ryan_glgr.hansel_grapher.stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.HanselChains;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.MagicFunctionMode;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;

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
        final HashSet<HashSet<Node>> lowUnits = new HashSet<>();
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
                    final var lowUnitsByClass = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
                    final var adjustedLowUnitsByClass = HanselChains.removeUselessLowUnits(lowUnitsByClass);
                    lowUnits.add(new HashSet<>(adjustedLowUnitsByClass.get(1)));
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

        for(final Set<Node> nodes: lowUnits) {
            final Map<Integer, Set<Integer[]>> lowUnitsToMakeTheFunctionTrue = Map.of(1,
                    nodes.stream()
                    .map(node -> node.values)
                    .collect(Collectors.toSet()));

            final Map<Integer, Set<Integer[]>> lowUnitsToMakeTheFunctionTrueByClass = new HashMap<>();
            for (final Node node : nodes) {
                Set<Integer[]> lowUnitsForThisNodesClass = lowUnitsToMakeTheFunctionTrueByClass.get(node.classification);
                if (Objects.isNull(lowUnitsForThisNodesClass)) {
                    lowUnitsToMakeTheFunctionTrueByClass.put(node.classification, new HashSet<>());
                    lowUnitsForThisNodesClass = lowUnitsToMakeTheFunctionTrueByClass.get(node.classification);
                }
                lowUnitsForThisNodesClass.add(node.values);
            }

            for(int i = 0; i < modes.length; i++) {
                final Interview interview = new Interview(kVals,
                    fakeWeights,
                    modes[i],
                    numClasses,
                    fakeNames,
                    fakeNames,
                    lowUnitsToMakeTheFunctionTrue,
                    subFunctionsForEachAttribute,
                    null,
                    magicFunctionMode);

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
}

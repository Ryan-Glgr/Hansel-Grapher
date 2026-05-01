package io.github.ryan_glgr.hansel_grapher.functionrules;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;


public class RuleCreation {

    // takes in the set of low units, broken up by classification.
    public static RuleNode[] createRuleTrees(final Map<Integer, Set<Node>> lowUnitSet,
                                             final int numAttributes){

        return IntStream.range(0, lowUnitSet.size())
                .parallel()
                .mapToObj(classification -> 
                    RuleNode.createRuleNodes(new ArrayList<>(lowUnitSet.getOrDefault(classification, Set.of())), numAttributes))
                .toArray(RuleNode[]::new);
    }
}

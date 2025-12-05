package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import java.util.ArrayList;
import java.util.stream.IntStream;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;


public class RuleCreation {

    // takes in the set of low units, broken up by classification.
    public static RuleNode[] createRuleTrees(final ArrayList<ArrayList<Node>> lowUnitSet,
                                             final int numAttributes,
                                             final boolean findOptimalChildren){

        final RuleNode[] roots = new RuleNode[lowUnitSet.size()];
        roots[0] = RuleNode.createRuleNodes(new ArrayList<>(), numAttributes, findOptimalChildren);
        
        IntStream.range(1, lowUnitSet.size())
                .parallel()
                .forEach(classification -> 
                    roots[classification] = RuleNode.createRuleNodes(lowUnitSet.get(classification),
                            numAttributes,
                            findOptimalChildren));
        return roots;
    }
}

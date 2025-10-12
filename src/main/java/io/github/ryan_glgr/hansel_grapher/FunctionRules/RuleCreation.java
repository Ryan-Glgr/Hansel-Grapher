package io.github.ryan_glgr.hansel_grapher.FunctionRules;

import java.util.ArrayList;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;


public class RuleCreation {

    // takes in the set of low units, broken up by classification.
    public static RuleNode[] createRuleTrees(ArrayList<ArrayList<Node>> lowUnitSet){

        RuleNode[] roots = new RuleNode[lowUnitSet.size()];
        for (int classification = 0; classification < lowUnitSet.size(); classification++) {
            roots[classification] = RuleNode.createRuleNodes(lowUnitSet.get(classification));
        }
        return roots;
    }

}

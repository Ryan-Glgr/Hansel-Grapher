package io.github.ryan_glgr.hansel_grapher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleCreation;
import io.github.ryan_glgr.hansel_grapher.FunctionRules.RuleNode;
import io.github.ryan_glgr.hansel_grapher.Stats.InterviewStats;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.HanselChains;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;
import io.github.ryan_glgr.hansel_grapher.Visualizations.InterviewStatsVisualizer;
import io.github.ryan_glgr.hansel_grapher.Visualizations.VisualizationDOT;

public class Main {

    public static Integer[] kValues = {3, 4, 3, 3, 2, 4, 5};
    public static Float[] weights = {2.25f, 1.0f, 0.75f, 2.65f, .80f, 4.5f, 2.25f};
    static {
        int maxSum = 0;
        for (int i = 0; i < kValues.length; i++) {
            maxSum += (int)((kValues[i] - 1) * weights[i]);
        }
        highestPossibleClassification = maxSum / kValues.length;

        Node.dimension = kValues.length;
    }
    // Calculate the highest possible classification at compile time
    public static Integer highestPossibleClassification;
    
    public static void main(String[] args) {

//        for (Interview.InterviewMode mode : Interview.InterviewMode.values()){
//            makeClassifyAndSaveNodes(mode);
//        }
        makeClassifyAndSaveNodes(Interview.InterviewMode.NONBINARY_SEARCH_CHAINS);
        makeClassifyAndSaveNodes(Interview.InterviewMode.BEST_MINIMUM_CONFIRMED);
        System.exit(0);
    }

    public static void makeClassifyAndSaveNodes(Interview.InterviewMode interviewMode){

            int numClasses = highestPossibleClassification + 1;

            // make all our nodes.
            HashMap<Integer, Node> nodes = Node.makeNodes(kValues, numClasses);
            // make the chains
            ArrayList<ArrayList<Node>> hanselChains = HanselChains.generateHanselChainSet(kValues, nodes);

            // classify all our data
            InterviewStats interviewStats = new Interview(kValues, weights, nodes, hanselChains, interviewMode, numClasses).interviewStats;

            System.out.println(interviewMode + " INTERVIEW COMPLETE!");
            System.out.println("NUMBER OF QUESTIONS ASKED: " + interviewStats.nodesAsked.size());

            // find our low units
            ArrayList<ArrayList<Node>> lowUnits = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
            int numberOfLowUnits = lowUnits.stream().mapToInt(ArrayList::size).sum();
            System.out.println("TOTAL NUMBER OF LOW UNITS:\t" + numberOfLowUnits);

            for (int classification = 0; classification < numClasses; classification++) {
                 System.out.println("NUMBER OF LOW UNITS FOR CLASS " + classification + ":\t" + lowUnits.get(classification).size());
                 System.out.println("LOW UNITS FOR CLASS " + classification + ":\n");
                 printListOfNodes(lowUnits.get(classification));
            }

            ArrayList<ArrayList<Node>> adjustedLowUnits = HanselChains.removeUselessLowUnits(lowUnits);
            int numberOfAdjustedLowUnits = adjustedLowUnits.stream().mapToInt(ArrayList::size).sum();
            System.out.println("\nTOTAL NUMBER OF ADJUSTED LOW UNITS:\t" + numberOfAdjustedLowUnits);

            for (int classification = 0; classification < numClasses; classification++) {
                 System.out.println("NUMBER OF ADJUSTED LOW UNITS FOR CLASS " + classification + ":\t" + adjustedLowUnits.get(classification).size());
                 System.out.println("ADJUSTED LOW UNITS FOR CLASS " + classification + ":\n");
                 printListOfNodes(adjustedLowUnits.get(classification));
            }
            
            RuleNode[] ruleTrees = RuleCreation.createRuleTrees(adjustedLowUnits);
            for (int classification = 0; classification < numClasses; classification++) {
                 ruleTrees[classification].printTree(false, classification);
            }

            int totalClauses = Arrays.stream(ruleTrees)
                .mapToInt(ruleTree -> ruleTree.subtreeSize(ruleTree))
                .sum();
             System.out.println("\nTOTAL NUMBER OF CLAUSES NEEDED:\t" + totalClauses);

        try{
            // ensure output directory exists
            File outputDir = new File("out/phony.txt").getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }

            // visualize our results
//             VisualizationDOT.makeHanselChainDOT(hanselChains, adjustedLowUnits);

            // make the expansions picture
//             VisualizationDOT.makeExpansionsDOT(nodes, adjustedLowUnits);

            String interviewStatsOutputString = interviewMode + " Interview Stats";
            InterviewStatsVisualizer.savePDF(interviewStats, 
                "out/" + interviewStatsOutputString + ".pdf",
                interviewMode);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void printListOfNodes(ArrayList<Node> nodes){
        List<String> valuesStrings = nodes.stream()
            .map(node -> "\n" + Arrays.toString(node.values))
            .toList();
        System.out.println(valuesStrings);
    }

}

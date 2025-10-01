import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static Integer[] kValues = {3, 4, 6, 3};
    public static Float[] weights = {2.25f, 1.0f, 0.5f, 0.75f};
    static {
        int maxSum = 0;
        for (int i = 0; i < kValues.length; i++) {
            maxSum += (int)((kValues[i] - 1) * weights[i]);
        }
        highestPossibleClassification = maxSum / kValues.length; // same as sum / dimension

        Node.dimension = kValues.length;
    }
    // Calculate the highest possible classification at compile time
    // This uses the same logic as questionExpert: sum of max values / dimension
    public static Integer highestPossibleClassification;
    
    public static void main(String[] args) {

        makeClassifyAndSaveNodes(Interview.InterviewMode.BEST_MINIMUM_CONFIRMED);

        System.exit(0);
    }

    // TODO:
    // make a meta version of the interview which combines heuristics?
    //      - A useful one might be something like using the best balanced node earlier in the interview, and then using the best min confirmed as we get further.
    //      - Or just combining the two with some kind of weighing.


    public static void makeClassifyAndSaveNodes(Interview.InterviewMode interviewMode){
        try{
            int numClasses = highestPossibleClassification + 1;

            // make all our nodes.
            HashMap<Integer, Node> nodes = Node.makeNodes(kValues, numClasses);
            // make the chains
            ArrayList<ArrayList<Node>> hanselChains = HanselChains.generateHanselChainSet(kValues, nodes);

            System.out.println("NUMBER OF CHAINS:\t" + hanselChains.size());
            System.out.println("NUMBER OF NODES:\t" + nodes.size());

            // classify all our data
            Interview.conductInterview(nodes, hanselChains, interviewMode, numClasses);

            // find our low units
            ArrayList<ArrayList<Node>> lowUnits = HanselChains.findLowUnitsForEachClass(hanselChains, numClasses);
            int numberOfLowUnits = lowUnits.stream().mapToInt(ArrayList::size).sum();
            System.out.println("TOTAL NUMBER OF LOW UNITS:\t" + numberOfLowUnits);

            for (int classification = 0; classification < numClasses; classification++) {
                System.out.println("NUMBER OF LOW UNITS FOR CLASS " + classification + ":\t" 
                                + lowUnits.get(classification).size());

                // map each node to Arrays.toString(node.values) and collect to a list
                List<String> valuesStrings = lowUnits.get(classification).stream()
                    .map(node -> Arrays.toString(node.values) + "\n")
                    .toList();

                System.out.println("LOW UNITS FOR CLASS " + classification + ":\t" + valuesStrings);
            }

            ArrayList<ArrayList<Node>> adjustedLowUnits = HanselChains.removeUselessLowUnits(lowUnits);
            int numberOfAdjustedLowUnits = adjustedLowUnits.stream().mapToInt(ArrayList::size).sum();
            System.out.println("\nTOTAL NUMBER OF ADJUSTED LOW UNITS:\t" + numberOfAdjustedLowUnits);

            for (int classification = 0; classification < numClasses; classification++) {
                System.out.println("NUMBER OF LOW UNITS FOR CLASS " + classification + ":\t" 
                                + adjustedLowUnits.get(classification).size());

                List<String> valuesStrings = adjustedLowUnits.get(classification).stream()
                    .map(node -> Arrays.toString(node.values) + "\n")
                    .toList();

                System.out.println("LOW UNITS FOR CLASS " + classification + ":\t" + valuesStrings);
            }


            // visualize our results
            Visualization.makeHanselChainDOT(hanselChains, adjustedLowUnits);

            // make the expansions picture
            Visualization.makeExpansionsDOT(nodes, adjustedLowUnits);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}

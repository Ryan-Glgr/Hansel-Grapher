import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static Integer[] kValues = {3, 4, 3, 5, 3, 3};
    public static Float[] weights = {2.25f, 1.0f, 0.75f, 1.5f, 2.65f, 1.5f};
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

        makeClassifyAndSaveNodes(Interview.InterviewMode.BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS);
        System.exit(0);
    }

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
            int numberOfLowUnits = lowUnits
                .stream()
                .mapToInt(ArrayList::size)
                .sum();
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


            // visualize our results
            Visualization.makeHanselChainDOT(hanselChains, adjustedLowUnits);

            // make the expansions picture
            Visualization.makeExpansionsDOT(nodes, adjustedLowUnits);
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

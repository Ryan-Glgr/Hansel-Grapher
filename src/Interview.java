import java.util.ArrayList;
import java.util.HashMap;


// this class is where we are going to handle anything classification related.
public class Interview {


    // if we set this false, we are going to call upon some ML interviewer instead.
    public static boolean EXPERT_MODE = true;



    /* IDEAS

    - we can binary search each chain of expansions, instead of each HC. since a hansel chain can have more possible expansions which aren't in the chain.
    - we can also sort the Nodes by how many possible expansions each one has. this way we can just take the one with most possible expansions at a time.

    - a node is a low unit if we are expanding down and the one below is a lower class.
    - a node is a high unit if we are going up and the next guy is a higher class obviously.

    */
    
    public static void mostExpansionsFirst(ArrayList<Node> allNodes){
        // now we sort all nodes by their possible expansions.
        allNodes.sort((Node x, Node y) -> {
            return y.possibleExpansions - x.possibleExpansions;
        });
    }


    // mega function which determines how we are going to ask questions.
    public static void conductInterview(HashMap<Integer, Node> data, int mode){

        ArrayList<Node> allNodes = new ArrayList<>();
        // for each node, we are going to put in that node, and it's number of expansions as a pair.
        allNodes.addAll(data.values());

        // sort our nodes in whichever order we are using.
        if (mode == 0){
            // for now we just use the most expansions first method.
            mostExpansionsFirst(allNodes);
        }


        int questionsAsked = 0;
        for(Node n : allNodes){
                        
            // if a node has an equal classification to it's upperbound, it is toast. no need to bother asking a question then.
            if (n.classification == n.maxPossibleValue)
                continue;

            // get our value either from expert or ML
            int c = (EXPERT_MODE) ? questionExpert(n) : -1;

            questionsAsked++;
            // set this nodes classification, and permeate it to his neighbors.
            n.classification = c;
            n.permeateClassification();
        }
        System.out.println("QUESTIONS ASKED:\t" + questionsAsked);
        System.out.println("TOTAL NODES:\t" + allNodes.size());

    }

    // asks the "expert" what the classification of this datapoint is.
    public static int questionExpert(Node datapoint){

        // for now, we will just say if the sum of digits is greater than dimension, it's one.
        int sum = 0;
        for(int digit : datapoint.values){
            sum += digit;
        }
        return sum / Node.dimension;
    }


}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;




public class HanselChains{

    // our set of HC's. an individual chain is an arraylist, and we have a bunch of them.
    public static ArrayList<ArrayList<Node>> hanselChainSet;

    // function to create our chains
    public static void generateHanselChainSet(Integer[] kValues, HashMap<Integer, Node> nodes){


        // for each attribute, we do this. consider an arraylist with just the first attribute to start as our "list of chains".
        // append all the different values of each attribute to each chain we have so far
        hanselChainSet = new ArrayList<ArrayList<Node>>();

        // now we go through each attribute, and for each of it's "legal" k values, we append all of those, one to each of the existing chains.
        // then we go back through, and just move the last thing in each chain into the one previous.
        
        // make a new array of integers we can use to look up our points fast.
        Integer[] valsForChains = new Integer[kValues.length];
        for(int i = 0; i < valsForChains.length; i++)
            valsForChains[i] = 0;

        ArrayList<Node> thisChain = new ArrayList<Node>();
        for(int firstDigitVal = 0; firstDigitVal < kValues[0]; firstDigitVal++){
            // make a new "hansel chain" which is really just our one point which would be like [0,0,0,0], then [0,0,0,1], then [0,0,0,2] for k = 3
            valsForChains[0] = firstDigitVal;
            thisChain.add(nodes.get(Node.hash(valsForChains)));
        }
        hanselChainSet.add(thisChain);

        // now we have our first digit set up. in regular boolean chains, we would've just made (0), (1). padded with 0's of course.
        // now we iterate through each digit, doing the hansel chain making process.
        // for each digit, we basically make a copy of all the existing hansel chains, and append all our possible values to it.
        for(int digit = 1; digit < kValues.length; digit++){

            // our new set of chains, we don't want to work over the top of the existing ones.
            ArrayList<ArrayList<Node>> newChains = new ArrayList<ArrayList<Node>>();

            // now we need to copy the existing chains to append all our different values. for example, if we had a k value of three, we make three sets of the existing chains, and append 0, 1, or 2 to all those points.
            for(int digitVal = 1; digitVal < kValues[digit]; digitVal++){

                // if we are appending digit 0, we actually already have that. since our method of appending is just changing the padded 0's to whatever value.
                // now, iterate through all the chains we currently have, and we are going to make a copy of each, and change whichever digit index we are at, to digitVal in each of the copies.
                for(ArrayList<Node> chain : hanselChainSet){

                    // copying "chain" but changing the value of "digit" to digitVal. mimicking the recursive step where you copy each chain but change the front value.
                    ArrayList<Node> copyChain = new ArrayList<Node>();
                    for(Node t : chain){

                        // make a copy of the values
                        Integer[] newVals = Arrays.copyOf(t.values, t.values.length);
                        newVals[digit] = digitVal;

                        // get the node with this value from the hashmap of nodes.
                        Node temp = Node.Nodes.get(Node.hash(newVals));
                        copyChain.add(temp);
                    }
                    newChains.add(copyChain);
                }
            }

            // original number of chains is useful, because now we need to do some moving around.
            int originalNumChains = hanselChainSet.size();

            // now that we are done making new chains, we go through and add all the copied and changed ones.
            hanselChainSet.addAll(newChains);
        
            // now our final step, starting from the second chain, we go through, and give our last element to the previous chain which has the same length as the loser chain.
            for(int c = originalNumChains; c < hanselChainSet.size(); c++){
                ArrayList<Node> loserChain = hanselChainSet.get(c);
                ArrayList<Node> chainToAddTo = hanselChainSet.get(c - originalNumChains);
                chainToAddTo.add(loserChain.remove(loserChain.size() - 1));
            }

            // now delete empty chains.
            for(int c = hanselChainSet.size() - 1; c >= 0; c--){
                if (hanselChainSet.get(c).size() == 0)
                    hanselChainSet.remove(c);
            }

        
        }
    }

    public static void sortChainsForVisualization(){
        // sort our chains
        hanselChainSet.sort((ArrayList<Node> a, ArrayList<Node> b) -> {
            return b.size() - a.size();
        });

        // now give them the diamond shape.
        ArrayList<ArrayList<Node>> newOrdering = new ArrayList<ArrayList<Node>>();
        for(int i = 0; i < hanselChainSet.size(); i++){
            // if even, put it in the front, if odd, the back. that is how we will alternate and get that shape.
            if (i % 2 == 0){
                newOrdering.add(hanselChainSet.get(i));
            }
            else{
                newOrdering.addFirst(hanselChainSet.get(i));
            }
        }
        // change the pointer to hanselChainSet to the reordered ones.
        hanselChainSet = newOrdering;

    }

}
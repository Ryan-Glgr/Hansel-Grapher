import java.util.ArrayList;
import java.util.HashMap;




public class HanselChains{

    // our set of HC's. an individual chain is an arraylist, and we have a bunch of them.
    public static ArrayList<ArrayList<Node>> HanselChainSet;

    // function to create our chains
    public static void generateHanselChainSet(Integer[] kValues, HashMap<Integer, Node> nodes){


        // for each attribute, we do this. consider an arraylist with just the first attribute to start as our "list of chains".
        // append all the different values of each attribute to each chain we have so far
        HanselChainSet = new ArrayList<ArrayList<Node>>();

        // now we go through each attribute, and for each of it's "legal" k values, we append all of those, one to each of the existing chains.
        // then we go back through, and just move the last thing in each chain into the one previous.
        
        // make a new array of integers we can use to look up our points fast.
        Integer[] valsForChains = new Integer[kValues.length];
        for(int i = 0; i < valsForChains.length; i++)
            valsForChains[i] = 0;

        for(int firstDigitVal = 0; firstDigitVal < kValues[0]; firstDigitVal++){

            // make a new "hansel chain" which is really just our one point which would be like [0,0,0,0], then [0,0,0,1], then [0,0,0,2] for k = 3
            ArrayList<Node> thisChain = new ArrayList<Node>();
            valsForChains[0] = firstDigitVal;
            thisChain.add(nodes.get(Node.hash(valsForChains)));
        }

        // now we have our first digit set up. in regular boolean chains, we would've just made (0), (1). padded with 0's of course.

        // now we iterate through each digit, doing the hansel chain making process.
        // for each digit, we basically make a copy of all the existing hansel chains, and append all our possible values to it.
        for(int digit = 1; digit < kValues.length; digit++){

            // our new set of chains, we don't want to work over the top of the existing ones.
            ArrayList<ArrayList<Node>> newChains = new ArrayList<ArrayList<Node>>();

            // now we need to copy the existing chains to append all our different values. for example, if we had a k value of three, we make three sets of the existing chains, and append 0, 1, or 2 to all those points.
            for(int digitVal = 1; digitVal < kValues[digit]; digitVal++){

                // if we are appending digit 0, we actually already have that. since our method of appending is just changing the padded 0's to whatever value.
                

            }

        }

    }





}
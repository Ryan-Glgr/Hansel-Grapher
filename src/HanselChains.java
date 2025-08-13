import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class HanselChains{

    // our set of HC's. an individual chain is an arraylist, and we have a bunch of them.
    public static ArrayList<ArrayList<Node>> hanselChainSet;
    
    // Thread pool for parallel operations
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

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
            // make a new hansel chain which is really just our one point which would be like [0,0,0,0], then [0,0,0,1], then [0,0,0,2] for k = 3
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
                // our chains which we are copying from the existing chains
                ArrayList<Future<ArrayList<Node>>> futures = new ArrayList<>();
                final int currentDigit = digit;
                final int currentDigitVal = digitVal;

                // copy each existing chain, and we are going to do this for each digit value > 0 until k
                for (ArrayList<Node> chain : hanselChainSet) {
                    futures.add(executor.submit(() -> copyChainWithDigitValue(chain, currentDigit, currentDigitVal)));
                }
                
                // Collect results from all threads
                try {
                    for(Future<ArrayList<Node>> future : futures) {
                        newChains.add(future.get());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("WE'RE BONED");
                    System.exit(0);
                }
            }

            // original number of chains is useful, because now we need to do some moving around.
            int originalNumChains = hanselChainSet.size();

            // now that we are done making new chains, we go through and add all the copied and changed ones.
            hanselChainSet.addAll(newChains);
        
            // now the important adjustment step, where we take the end of one chain, and move it to the one before, so that we get the diamond shape.
            adjustEndsOfIsomorphicChainsStep(hanselChainSet, originalNumChains, kValues[digit]);

            hanselChainSet.removeIf(List::isEmpty);
        }
    
        for (ArrayList<Node> chain : hanselChainSet) {
            if (!checkChain(chain)) System.out.println("ERROR: Chain is broken: " + chain.toString());
        }
    
    }

    private static ArrayList<Node> copyChainWithDigitValue(ArrayList<Node> chainToCopy, int currentDigit, int currentDigitVal) {

        ArrayList<Node> newChain = new ArrayList<>(chainToCopy.size());
        for (Node t : chainToCopy) {
            Integer[] newVals = Arrays.copyOf(t.values, t.values.length);
            newVals[currentDigit] = currentDigitVal;
            Node temp = Node.Nodes.get(Node.hash(newVals));
            newChain.add(temp);
        }
        return newChain;
    }

    public static void adjustEndsOfIsomorphicChainsStep(ArrayList<ArrayList<Node>> hanselChains, int originalNumChains, int kValue){

        for (int digitVal = 1; digitVal < kValue; digitVal++) {
            int fromStart = digitVal * originalNumChains;

            for (int i = 0; i < originalNumChains; i++) {
                List<Node> fromChain = hanselChains.get(fromStart + i);
                List<Node> toChain = hanselChains.get(i);

                if (!fromChain.isEmpty()) {
                    toChain.add(fromChain.remove(fromChain.size() - 1));
                }
            }
        }
    }

    public static void sortChainsForVisualization(){
        // sort our chains using parallel stream
        hanselChainSet.parallelStream()
                .sorted((ArrayList<Node> a, ArrayList<Node> b) -> {
                    return b.size() - a.size();
                })
                .forEachOrdered(chain -> {}); // Force evaluation of parallel sort
        
        // Apply the sort result back to hanselChainSet
        hanselChainSet = hanselChainSet.parallelStream()
                .sorted((ArrayList<Node> a, ArrayList<Node> b) -> {
                    return b.size() - a.size();
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

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

    public static Boolean checkChain(ArrayList<Node> chain){
        for(int i = 0; i < chain.size() - 1; i++){
            if (chain.get(i).sumUpDataPoint() + 1 != chain.get(i + 1).sumUpDataPoint()){
                return false;
            }
        }
        return true;
    }

    // Clean up thread pool when done
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

}
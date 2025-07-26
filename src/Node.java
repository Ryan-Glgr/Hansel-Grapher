import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class Node {

    public static boolean DEBUG_PRINTING = false;

    // big list of nodes
    public static HashMap<Integer, Node> Nodes = new HashMap<Integer, Node>();
    
    // Thread pool for parallel operations
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    
    // list of max possible value of each attribute
    public static Integer[] kValues;
    public static Integer dimension;

    // the datapoint this point represents
    public Integer[] values;

    // the classification of this point
    public Integer classification;

    // highest possible class this node can have
    public Integer maxPossibleValue; // comes from above
    
    // the direct neighbors that are up by one in each attribute. null if it is not valid to increase the attribute at that index by one
    public Node[] upExpansions;

    // direct neighbors downwards
    public Node[] downExpansions;

    // count of possible expansions, so that we can choose whichever node has the most possible expansions.
    public int possibleExpansions;

    // takes in a datapoint, and makes a copy of that and stores that as our "point"
    public Node(Integer[] datapoint){
        // copy the passed in datapoint to this node's point.
        values = Arrays.copyOf(datapoint, datapoint.length);
        
        // set classification to -1.
        classification = 0;

        upExpansions = new Node[dimension];
        downExpansions = new Node[dimension];

        maxPossibleValue = Integer.MAX_VALUE;
        possibleExpansions = 0;

    }

    // finds our up and down expansions. Since we know that the values of the expansions are just our point +- 1 in some attribute, we can just look it up.
    public void findExpansions(){

        // loop through our attributes, and find all the points which are our up and down neighbors.
        Integer[] key = Arrays.copyOf(values, values.length);
        for(int attribute = 0; attribute < dimension; attribute++){
            // increment the value of key at this attribute, so we can find the one with + 1 in this digit.
            key[attribute]++;
            upExpansions[attribute] = Nodes.get(hash(key));

            // decrement by 2 to find the one with -1 in this attribute.
            key[attribute] -= 2;
            downExpansions[attribute] = Nodes.get(hash(key));

            // reset back to normal so we can find the other nodes with the right key.
            key[attribute]++;
        }

        for (Node n : upExpansions)
            if (n != null)
                possibleExpansions++;

        for (Node n : downExpansions)
            if (n != null)
                possibleExpansions++;

    }

    // hash function is easy. k value [i] * point val [i] as a running sum and then vals [0] gets added.
    public static Integer hash(Integer[] keyVal){

        int sum = 0;
        for(int i = 0; i < keyVal.length; i++){
            sum += (int)Math.pow(31, i) * keyVal[i];
        }
        return sum;
    }

    // Helper method to increment a counter array based on kValues bounds
    // Returns true if increment was successful, false if we've wrapped around completely
    public static boolean incrementCounter(Integer[] counter) {
        int attribute = 0;
        
        // incrementing logic to go through all digits, all k vals.
        while (counter[attribute] + 1 >= kValues[attribute]) {
            counter[attribute] = 0;
            attribute++;
            
            // return false if we've incremented all the way around
            if (attribute >= kValues.length) {
                return false;
            }
        }
        
        // increment our attribute
        counter[attribute]++;
        return true;
    }

    // Helper method to create a properly initialized counter array for use with incrementCounter
    // Returns array filled with 0s except first element is -1, so first increment gives [0,0,0,...]
    public static Integer[] counterInitializer() {
        Integer[] counter = new Integer[kValues.length];
        for(int i = 0; i < counter.length; i++) {
            counter[i] = 0;
        }
        counter[0] = -1;
        return counter;
    }

    // makes all our nodes and populates the map
    public static void makeNodes(Integer[] kVals){

        Node.kValues = kVals;

        // set the dimension we are going to use all over
        dimension = kValues.length;

        Integer[] kValsToMakeNode = counterInitializer();

        // iterate through all the digits, and make all the nodes. 
        while(incrementCounter(kValsToMakeNode)){
            // just make a new node and put it in the map.
            Nodes.put(Node.hash(kValsToMakeNode), new Node(kValsToMakeNode));
        }
    
        // re initialize so we can copy paste
        Integer[] finalKValsToMakeNode = counterInitializer();
        
        // Collect all nodes that need expansion processing
        java.util.List<Node> nodesToProcess = new java.util.ArrayList<>();
        
        while(incrementCounter(finalKValsToMakeNode)){
            Node temp = Nodes.get(hash(finalKValsToMakeNode));
            nodesToProcess.add(temp);
        }
        
        // Process all nodes in parallel
        nodesToProcess.parallelStream().forEach(Node::findExpansions);
    

    }

    // our expansion victim just got his value assigned. now, what he does is update all his up neighbors to be at least his new value.
    // recursive with parallel processing.
    // if the node above already had that value or greater, we can skip the recursive step.
    public void expandUp(){

        // Use parallel processing for upstairs neighbors
        Arrays.stream(upExpansions)
                .parallel()
                .filter(upstairsNeighbor -> upstairsNeighbor != null && upstairsNeighbor.classification < this.classification)
                .forEach(upstairsNeighbor -> {
                    synchronized(upstairsNeighbor) {
                        // Double-check condition after acquiring lock
                        if (upstairsNeighbor.classification < this.classification) {
                            upstairsNeighbor.classification = this.classification;
                            upstairsNeighbor.expandUp();
                        }
                    }
                });

    }

    // we also now understand that everyone under me must be same or lower. so we change the upper bound of our downstairs neighbors.
    // uses this upperbound parameter so that we don't have to set the classification every single time.
    public void expandDown(int upperBound){
        // Use parallel processing for downstairs neighbors
        Arrays.stream(downExpansions)
                .parallel()
                .filter(downstairsNeighbor -> downstairsNeighbor != null && downstairsNeighbor.maxPossibleValue > upperBound)
                .forEach(downstairsNeighbor -> {
                    synchronized(downstairsNeighbor) {
                        // Double-check condition after acquiring lock
                        if (downstairsNeighbor.maxPossibleValue > upperBound) {
                            downstairsNeighbor.maxPossibleValue = upperBound;
                            downstairsNeighbor.expandDown(upperBound);
                        }
                    }
                });
    }

    // each node gets this new classification
    public void permeateClassification(){
        // we know two things. the ones under now have an upper bound established, and the ones above have a lower bound established.
        // so, we can iterate all the ones above, and set their classification to min(newClassification, theirClassification).
        // then we can iterate all the ones below, and set their maxPossibleValue to max(theirClassification, newClassification). 
        // set the value of everyone above to AT LEAST this value.
        expandUp();
        
        // set the max possible value of the ones below to our classification.
        expandDown(this.classification);

    }   

    @Override
    public int hashCode() {
        return hash(this.values);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return Arrays.equals(values, node.values);
    }

    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append("DATAPOINT:\n\t");
        s.append(Arrays.toString(values)).append("\n");

        s.append("CLASSIFICATION:\t" + classification + "\n");

        if (DEBUG_PRINTING){
            s.append("UP EXPANSIONS:\n");
            for(Node t : upExpansions){
                if (t == null)
                    s.append("\tNULL\n");
                
                else
                    s.append("\t").append(Arrays.toString(t.values)).append("\n");
            }

            s.append("DOWN EXPANSIONS:\n");
            for(Node t : downExpansions){
                if (t == null)
                    s.append("\tNULL\n");
                
                else
                    s.append("\t").append(Arrays.toString(t.values)).append("\n");
            }
        }

        return s.toString();
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

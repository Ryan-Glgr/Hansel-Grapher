package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Node {

    public static boolean DEBUG_PRINTING = false;
    
    // list of max possible value of each attribute
    public static Integer[] kValues;
    public static Integer dimension;

    // the datapoint this point represents
    public Integer[] values;

    // cached sum of all values in this datapoint for efficient sorting
    public int sum;

    // the classification of this point
    public Integer classification;

    // used when our lower bound and upperbound are the same. (or we've just asked the expert.)
    // once it's confirmed, we don't need to ask a question of course
    public boolean classificationConfirmed;

    // highest possible class this node can have
    public Integer maxPossibleValue; // comes from above during interview
    
    // the direct neighbors that are up by one in each attribute. null if it is not valid to increase the attribute at that index by one
    public Node[] upExpansions;

    // direct neighbors downwards
    public Node[] downExpansions;

    // used to calculate how far we think a classification can permeate. 
    // For example, if i have 3 down expansions, each with 3 unique down expansions, we have 12 in our umbrella. we could update 12 nodes with one question.
    // the size of the umbrella is helpful to know which nodes are powerful in terms of their question giving us more info.
    public int totalUmbrellaCases;

    // underneath is all the recursive "not confirmed" nodes under. vice versa for above.
    public int underneathUmbrellaCases;
    public int aboveUmbrellaCases;

    // stores the number of possible confirmations by class in this way.
    // hypothetically compute how many confirmations we would get by assigning this node to each class. and store them respectively.
    public int[] possibleConfirmationsByClass;

    // used as a different measure of how "good" an umbrella is. basically, we want a node which has a lot of nodes in umbrella, and they're balanced.
    // so we take the ratio with the total number / the difference in above and below cases. 
    public float balanceRatio;

    // takes in a datapoint, and makes a copy of that and stores that as our "point"
    private Node(Integer[] datapoint, int numClasses){
        // copy the passed in datapoint to this node's point.
        values = Arrays.copyOf(datapoint, datapoint.length);
        
        // set classification to 0.
        classification = 0;
        classificationConfirmed = false;

        upExpansions = new Node[dimension];
        downExpansions = new Node[dimension];

        // Set the maximum possible classification value from Main
        maxPossibleValue = numClasses - 1;

        totalUmbrellaCases = 0;
        underneathUmbrellaCases = 0;
        aboveUmbrellaCases = 0;

        possibleConfirmationsByClass = new int[numClasses];
        balanceRatio = 0.0f;

        sum = sumUpDataPoint();
    }

    private void findExpansions(HashMap<Integer, Node> nodes){

        // Parallel computation of expansions for each attribute
        IntStream.range(0, dimension).parallel().forEach(attribute -> {
            Integer[] key = Arrays.copyOf(values, values.length);
            
            // increment the value of key at this attribute, so we can find the one with + 1 in this digit.
            key[attribute]++;
            upExpansions[attribute] = nodes.get(hash(key));

            // decrement by 2 to find the one with -1 in this attribute.
            key[attribute] -= 2;
            downExpansions[attribute] = nodes.get(hash(key));
        });
    }

    // hash function is easy. k value [i] * point val [i] as a running sum and then vals [0] gets added.
    public static Integer hash(Integer[] keyVal){
        long sum = 0;
        for(int i = 0; i < keyVal.length; i++){
            sum += (long)Math.pow(31, i) * keyVal[i];
        }
        return (int)(sum % Integer.MAX_VALUE);
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
    public static HashMap<Integer, Node> makeNodes(Integer[] kVals, int numClasses) {
    
        HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

        Node.kValues = kVals;

        // set the dimension we are going to use all over
        dimension = kValues.length;

        Integer[] kValsToMakeNode = counterInitializer();

        // iterate through all the digits, and make all the nodes. 
        while(incrementCounter(kValsToMakeNode)){
            // just make a new node and put it in the map.
            nodes.put(Node.hash(kValsToMakeNode), new Node(kValsToMakeNode, numClasses));
        }
    
        // re initialize so we can copy paste
        Integer[] finalKValsToMakeNode = counterInitializer();
        while(incrementCounter(finalKValsToMakeNode)){
            Node temp = nodes.get(hash(finalKValsToMakeNode));
            temp.findExpansions(nodes);
        }
        return nodes;
    }

    // BFS-based expansion to set floor (classification) for nodes above
    private void expandUp(int lowerBound){
        
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        java.util.Set<Node> visited = new java.util.HashSet<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            // Skip if current node is already confirmed - we've already processed its implications
            if (current.classificationConfirmed) {    
                continue;
            }
            
            for (Node upstairsNeighbor : current.upExpansions) {
                // if we have a neighbor, who needs their floor raised, where we haven't been, and importantly, WHO IS NOT CONFIRMED.
                if (upstairsNeighbor != null && upstairsNeighbor.classification < lowerBound && !visited.contains(upstairsNeighbor) && upstairsNeighbor.classificationConfirmed == false) {
                    
                    // Update the floor (classification) of upstairs neighbor
                    upstairsNeighbor.classification = lowerBound;
                    
                    queue.add(upstairsNeighbor);
                    visited.add(upstairsNeighbor);
                }
            }
        }
        
        // Confirm nodes after propagation - check if floor equals ceiling
        for (Node node : visited) {
            if (node.classification == node.maxPossibleValue && node != this) {
                node.classificationConfirmed = true;
            }
        }
    }

    // BFS-based expansion to set ceiling (maxPossibleValue) for nodes below
    private void expandDown(int upperBound){
        
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        java.util.Set<Node> visited = new java.util.HashSet<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            // Skip if current node is already confirmed - we've already processed its implications
            if (current.classificationConfirmed) {
                continue;
            }
            
            for (Node downstairsNeighbor : current.downExpansions) {
                
                // if we have a neighbor, who needs their ceiling lowered, where we haven't been, and importantly, WHO IS NOT CONFIRMED.
                if (downstairsNeighbor != null && downstairsNeighbor.maxPossibleValue > upperBound && !visited.contains(downstairsNeighbor) && downstairsNeighbor.classificationConfirmed == false) {
                    
                    // lower the ceiling (maxPossibleValue) of downstairs neighbor
                    downstairsNeighbor.maxPossibleValue = upperBound;
                    
                    queue.add(downstairsNeighbor);
                    visited.add(downstairsNeighbor);
                }
            }
        }

        // Confirm nodes after propagation - check if floor equals ceiling
        for (Node node : visited) {
            if (node.classification == node.maxPossibleValue && node != this) {
                node.classificationConfirmed = true;
            }
        }
    }

    // each node gets this new classification. it sends the effects of the classification up and down. (not just within one HC, but to all expansions up and down.)
    public void permeateClassification(){
    
        // update our max here, so that know it is confirmed.
        this.maxPossibleValue = this.classification;

        // Set the floor of everyone above to AT LEAST this value
        expandUp(this.classification);
        
        // Set the ceiling of everyone below to AT MOST this value
        expandDown(this.classification);

        // Confirm this node since it was directly asked to expert
        this.classificationConfirmed = true;
    }   

    // used when we are computing the number of umbrella cases. we need to sort by the hamming value of the case.
    public Integer sumUpDataPoint(){
        Integer sum = 0;
        for (Integer x : this.values){
            sum += x;
        }
        return sum;
    }

    // does a BFS from each node, updating rankings as we go. Ranking are umbrella size and the minimum classifications.
    public static void updateAllNodeRankings(ArrayList<Node> allNodes, int numClasses) {

        for(Node n : allNodes) {
            n.aboveUmbrellaCases = 0;
            n.underneathUmbrellaCases = 0;
            n.totalUmbrellaCases = 0;
            Arrays.fill(n.possibleConfirmationsByClass, 0);
        }
        // update the node stats for above and below cases.
        allNodes.parallelStream()
            .forEach(n -> n.updateNodeStatistics(false, numClasses));
        allNodes.parallelStream()
            .forEach(n -> n.updateNodeStatistics(true, numClasses));
        
        allNodes.parallelStream()
            .forEach(n -> n.totalUmbrellaCases = n.aboveUmbrellaCases + n.underneathUmbrellaCases);
        
        // compute the new balance ratio for each node. this is defined as the total umbrella size, divided by the difference in up/down size.
        allNodes.parallelStream()
            .forEach(n -> n.setBalanceFactor());
    }

    // Calculate umbrella size using proper graph traversal to avoid double counting
    private void updateNodeStatistics(boolean countUpwards, int numClasses) {
        java.util.Set<Node> visited = new java.util.HashSet<>();
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Node[] neighbors = countUpwards ? current.upExpansions : current.downExpansions;
            
            for (Node neighbor : neighbors) {
                if (neighbor != null && !neighbor.classificationConfirmed && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);

                    // if "this" node were assigned a hypothetical class how does that affect neighbor.
                    for(int hypotheticalClass = 0; hypotheticalClass < numClasses; hypotheticalClass++){
                        if (neighbor.wouldBeConfirmedForClass(hypotheticalClass, countUpwards)) {
                            this.possibleConfirmationsByClass[hypotheticalClass]++;
                        }
                    }
                }
            }
        }
        
        if (countUpwards) {
            this.aboveUmbrellaCases = visited.size() - 1;
        } else {
            this.underneathUmbrellaCases = visited.size() - 1;
        }
    }

    // determines if a neighbor would get confirmed if it's upstairs or downstairs neighbor were given "hypotheticalClass"
    private boolean wouldBeConfirmedForClass(int hypotheticalClass, boolean countingUpwards) {
        // return false if we're already confirmed.
        if (this.classificationConfirmed){
            return false;
        }

        // if we are counting upwards. this is ABOVE the node which called this on us.
        if (countingUpwards){
            // if the neighbor got confirmed as hypothetical class, we would be confirmed, if that hypothetical class was already our max possible value.
            return this.maxPossibleValue == hypotheticalClass;
        }
        // if we are counting downwards. this is a downstairs neighbor. our classification is already set as the lowerbound. so if our lowerbound is the same as the node above, then we are confirmed.
        else {
            return this.classification == hypotheticalClass;
        }
    }

    // compares two nodes by their minimum number of confirmations. Then by the second lowest, third and so on. If a total tie, we go by the balanceRatio.
    // returns negative when THIS node should be after.
    public int compareMinClassifications(Node other) {
        // make a copy of the mins and maxes arrays.
        int[] thisNodeMins = Arrays.copyOf(this.possibleConfirmationsByClass, this.possibleConfirmationsByClass.length);
        int[] otherNodeMins = Arrays.copyOf(other.possibleConfirmationsByClass, other.possibleConfirmationsByClass.length);

        Arrays.sort(thisNodeMins);   // ascending
        Arrays.sort(otherNodeMins);  // ascending

        // Compare lexicographically, preferring LARGER minima first
        for (int i = 0; i < thisNodeMins.length; i++) {
            if (thisNodeMins[i] != otherNodeMins[i]) {
                return Integer.compare(thisNodeMins[i], otherNodeMins[i]); // positive means "this" > "other"
            }
        }

        // Tie-breaker: prefer smaller balanceRatio (so lower is "better" → should return positive)
        return Float.compare(other.balanceRatio, this.balanceRatio);
    }

    public int computeHammingDistance(Node other) {
        int distance = 0;
        for (int i = 0; i < this.values.length; i++) {
            distance += Math.abs(this.values[i] - other.values[i]);
        }
        return distance;
    }

    // compute the "balance factor" for a node.
    private void setBalanceFactor() {
        if (totalUmbrellaCases == 0) {
            this.balanceRatio = 0.0f;
            return;
        }
        float imbalance = (float)Math.abs(this.aboveUmbrellaCases - this.underneathUmbrellaCases) / totalUmbrellaCases;
        this.balanceRatio = totalUmbrellaCases * (1.0f - imbalance);
    }

    // checks whether 'this' is getting dominated - smothered or even in ALL attributes. Either upwards or downwards.
    // in terms of finding low units, we only care about the other node on top case, but going the other way, we could find HIGH units.
    public boolean isDominatedBy(Node otherNode, boolean otherNodeOnTop){

        // look for ONE digit, where our boy isn't getting covered. if otherNode is greater or equal 
        //(less or equal if otherNodeOnTop is false, meaning 'this' node is on top) in all attributes, we are covered.
        for(int digitPosition = 0; digitPosition < this.values.length; digitPosition++){
            int thisValue = this.values[digitPosition];
            int otherValue = otherNode.values[digitPosition];

            if (otherNodeOnTop && thisValue > otherValue){
                return false;
            }
            else if (!otherNodeOnTop && thisValue < otherValue){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hash(this.values);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) 
            return true;
        if (obj == null || getClass() != obj.getClass()) 
            return false;

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
        
            s.append("CLASSIFICATION CONFIRMED? :" + (classificationConfirmed ? "\tYES\n" : "\tNO\n"));
            s.append("TOTAL UMBRELLA SIZE:\t" + totalUmbrellaCases + "\n");
            s.append("UNDER UMBRELLA SIZE:\t" + underneathUmbrellaCases + "\n");
            s.append("ABOVE UMBRELLA SIZE:\t" + aboveUmbrellaCases + "\n");

            s.append("POSSIBLE CONFIRMATIONS:\n");
            for(int i = 0; i < possibleConfirmationsByClass.length; i++){
                s.append("\tClass: " + i + ": " + possibleConfirmationsByClass[i] + "\n");
            }

        }
        return s.toString();
    }
}

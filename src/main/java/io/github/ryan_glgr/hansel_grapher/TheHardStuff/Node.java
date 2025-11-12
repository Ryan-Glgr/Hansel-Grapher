package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.*;
import java.util.stream.IntStream;

import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats;

public class Node {

    public static boolean DEBUG_PRINTING = false;

    public static BalanceRatio BALANCE_RATIO = BalanceRatio.UNITY_BALANCE_RATIO;

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
    public Integer[] possibleConfirmationsByClass;

    // used as a different measure of how "good" an umbrella is. basically, we want a node which has a lot of nodes in umbrella, and they're balanced.
    // so we take the ratio with the total number / the difference in above and below cases. 
    public double balanceRatio;

    public double umbrellaMagnitude;

    // takes in a datapoint, and makes a copy of that and stores that as our "point"
    private Node(Integer[] datapoint, int numClasses, int dimension){
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

        possibleConfirmationsByClass = new Integer[numClasses];
        balanceRatio = 0.0f;

        sum = sumUpDataPoint();
    }

    public Node(Node n) {
        this(n.values, n.maxPossibleValue + 1, n.upExpansions.length);
    }

    public static String printListOfNodes(List<Node> nodes){
        List<String> valuesStrings = nodes.stream()
            .map(node -> "\n" + Arrays.toString(node.values))
            .toList();
        return valuesStrings.toString();
    }

    private void findExpansions(HashMap<Integer, Node> nodes, int dimension){

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
    public static boolean incrementCounter(Integer[] counter, Integer[] kValues) {
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
    public static Integer[] counterInitializer(Integer[] kValues) {
        Integer[] counter = new Integer[kValues.length];
        Arrays.fill(counter, 0);
        counter[0] = -1;
        return counter;
    }

    // makes all our nodes and populates the map
    public static HashMap<Integer, Node> makeNodes(Integer[] kVals, int numClasses) {

        int dimension = kVals.length;
        HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

        Integer[] kValsToMakeNode = counterInitializer(kVals);

        // iterate through all the digits, and make all the nodes. 
        while(incrementCounter(kValsToMakeNode, kVals)){
            // just make a new node and put it in the map.
            nodes.put(Node.hash(kValsToMakeNode), new Node(kValsToMakeNode, numClasses, dimension));
        }
    
        // re initialize so we can copy paste
        Integer[] finalKValsToMakeNode = counterInitializer(kVals);
        while(incrementCounter(finalKValsToMakeNode, kVals)){
            Node temp = nodes.get(hash(finalKValsToMakeNode));
            temp.findExpansions(nodes, dimension);
        }
        return nodes;
    }

    private PermeationStats expand(final int bound, final boolean countUpwards) {
        // BFS-based expansion to set ceiling of below nodes, and floor of above nodes.
        final Queue<Node> queue = new LinkedList<>();
        final Set<Node> visited = new HashSet<>();
        Integer numberOfConfirmations = 0;
        Integer numberOfNodesTouched = 0;
        queue.add(this);
        visited.add(this);

         while (!queue.isEmpty()) { final Node current = queue.poll();
             // Skip if current node is already confirmed - we've already processed its implications
              if (current.classificationConfirmed) {
                  continue;
              }
              final Node[] neighbors = countUpwards
                      ? current.upExpansions
                      : current.downExpansions;

              for (final Node neighbor : neighbors) {
                  if (countUpwards) {
                      // if we have a neighbor, who needs their floor raised, where we haven't been, and importantly, WHO IS NOT CONFIRMED.
                      final boolean neighborNeedsUpdate = neighbor != null && !neighbor.classificationConfirmed && neighbor.classification < bound && !visited.contains(neighbor);
                      if (neighborNeedsUpdate){
                          neighbor.classification = bound;
                          numberOfNodesTouched++;
                          queue.add(neighbor);
                          visited.add(neighbor);
                      }
                  }
                  else {
                      final boolean neighborNeedsUpdate = neighbor != null && !neighbor.classificationConfirmed && neighbor.maxPossibleValue > bound && !visited.contains(neighbor);
                      if (neighborNeedsUpdate) {
                          neighbor.maxPossibleValue = bound;
                          numberOfNodesTouched++;
                          queue.add(neighbor);
                          visited.add(neighbor);
                      }
                  }
              }
         }

         // Confirm nodes after propagation - check if floor equals ceiling
         for (final Node node : visited) {
             final boolean nodeWasConfirmed = node.classification == node.maxPossibleValue && node != this;
             if (nodeWasConfirmed) {
                 node.classificationConfirmed = true;
                 numberOfConfirmations++;
             }
         }

         Integer numberOfNodesTouchedAbove = 0;
         Integer numberOfNodesTouchedBelow = 0;
         if (countUpwards){
             numberOfNodesTouchedAbove = numberOfNodesTouched;
         }
         else {
             numberOfNodesTouchedBelow = numberOfNodesTouched;
         }
         return new PermeationStats(numberOfConfirmations, numberOfNodesTouchedAbove, numberOfNodesTouchedBelow);
    }

    // each node gets this new classification. it sends the effects of the classification up and down. (not just within one HC, but to all expansions up and down.)
    public PermeationStats permeateClassification(int newClassification) {
    
        // lock in our nodes value
        this.classification = newClassification;
        this.maxPossibleValue = this.classification;

        // Set the floor of everyone above to AT LEAST this value
        PermeationStats aboveStats = expand(this.classification, true);
        
        // Set the ceiling of everyone below to AT MOST this value
        PermeationStats belowStats = expand(this.classification, false);

        // Confirm this node since it was directly asked to expert
        this.classificationConfirmed = true;

        // return our stats from this permeation.
        return new PermeationStats(aboveStats, belowStats);
    }

    // used when we are computing the number of umbrella cases. we need to sort by the hamming value of the case.
    public Integer sumUpDataPoint(){
        Integer sum = 0;
        for (Integer x : this.values){
            sum += x;
        }
        return sum;
    }


    // assume we had a node who's min classifications by class were [1, 2, 2] and another who's min classifications by class were:
    //  [0 (because it is guaranteed to NOT be this class by monotonicity already), 6, 12] we would choose the first, since it's min is lower.
    // but in reality, we want that second one, since the first class is just not possible. the real min is 6. not 0. so we have to have a flag for not set.
    // the reason it's max value, is so that when we sort the counts, this number last still, and will serve as a tiebreaker
    private static final Integer NOT_SET = Integer.MAX_VALUE;

    // does a BFS from each node, updating rankings as we go. Ranking are umbrella size and the minimum classifications.
    public static void updateAllNodeRankings(ArrayList<Node> allNodes) {

        for(Node n : allNodes) {
            n.aboveUmbrellaCases = 0;
            n.underneathUmbrellaCases = 0;
            n.totalUmbrellaCases = 0;
            Arrays.fill(n.possibleConfirmationsByClass, NOT_SET);

            // specifically set those class values where it IS possible to assign this class still, as 0. impossible is going to stay as NOT SET.
            for (int minPossibleClassForThisNode = n.classification; minPossibleClassForThisNode <= n.maxPossibleValue; minPossibleClassForThisNode++) {
                n.possibleConfirmationsByClass[minPossibleClassForThisNode] = 0;
            }
        }
        // update the node stats for above and below cases.
        allNodes.parallelStream()
            .forEach(n -> n.updateNodeStatistics(false));
        allNodes.parallelStream()
            .forEach(n -> n.updateNodeStatistics(true));
        
        allNodes.parallelStream()
            .forEach(n -> n.totalUmbrellaCases = n.aboveUmbrellaCases + n.underneathUmbrellaCases);
        
        // compute the new magnitude of above and below umbrella case vector
        allNodes.parallelStream()
            .forEach(n -> n.umbrellaMagnitude = n.computeUmbrellaMagnitude());

        // compute the new balance ratio for each node.
        allNodes.parallelStream()
            .forEach(n -> n.balanceRatio = BALANCE_RATIO.computeBalanceRatio(n));

        // now last step. sort each node's possible confirmations
        allNodes.parallelStream().forEach(n -> 
            n.possibleConfirmationsByClass = Arrays.stream(n.possibleConfirmationsByClass)
                .sorted() // sort ascending
                .toArray(Integer[]::new));
    }

    // Calculate umbrella size using proper graph traversal to avoid double counting
    private void updateNodeStatistics(final boolean countUpwards) {
        java.util.Set<Node> visited = new java.util.HashSet<>();
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Node[] neighbors = countUpwards ? current.upExpansions : current.downExpansions;
            
            for (final Node neighbor : neighbors) {

                final boolean neighborToCheck = neighbor != null && !neighbor.classificationConfirmed && !visited.contains(neighbor);
                if (!neighborToCheck)
                    continue;

                final boolean canUpdateNeighbor = countUpwards
                        ? this.maxPossibleValue > neighbor.classification
                        : this.classification < neighbor.maxPossibleValue;
                if (canUpdateNeighbor)
                    visited.add(neighbor);

                final boolean canConfirmNeighbor = countUpwards
                        ? this.maxPossibleValue == neighbor.maxPossibleValue
                        : this.classification == neighbor.classification;
                if(!canConfirmNeighbor)
                    continue;

                queue.add(neighbor);
                
                // if "this" node were assigned a hypothetical class how does that affect neighbor.
                for (int hypotheticalClass = this.classification; hypotheticalClass <= this.maxPossibleValue; hypotheticalClass++) {
                    if (neighbor.wouldBeConfirmedForClass(hypotheticalClass, countUpwards)) {
                        this.possibleConfirmationsByClass[hypotheticalClass]++;
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

    public int computeHammingDistance(Node other) {
        int distance = 0;
        for (int i = 0; i < this.values.length; i++) {
            distance += Math.abs(this.values[i] - other.values[i]);
        }
        return distance;
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

    public double computeUmbrellaMagnitude() {
        return Math.sqrt(Math.pow(aboveUmbrellaCases, 2) + Math.pow(underneathUmbrellaCases, 2));
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

        s.append("CLASSIFICATION:\t").append(classification).append("\n");

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
        
            s.append("CLASSIFICATION CONFIRMED? :").append(classificationConfirmed ? "\tYES\n" : "\tNO\n");
            s.append("TOTAL UMBRELLA SIZE:\t").append(totalUmbrellaCases).append("\n");
            s.append("UNDER UMBRELLA SIZE:\t").append(underneathUmbrellaCases).append("\n");
            s.append("ABOVE UMBRELLA SIZE:\t").append(aboveUmbrellaCases).append("\n");

            s.append("POSSIBLE CONFIRMATIONS:\n");
            for(int i = 0; i < possibleConfirmationsByClass.length; i++){
                s.append("\tClass: " + i + ": " + possibleConfirmationsByClass[i] + "\n");
            }

        }
        return s.toString();
    }
}

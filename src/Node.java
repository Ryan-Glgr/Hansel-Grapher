import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Node {

    public static boolean DEBUG_PRINTING = false;

    // big list of all the nodes.
    public static HashMap<Integer, Node> Nodes = new HashMap<Integer, Node>();
    
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
    public Integer maxPossibleValue; // comes from above
    
    // the direct neighbors that are up by one in each attribute. null if it is not valid to increase the attribute at that index by one
    public Node[] upExpansions;

    // direct neighbors downwards
    public Node[] downExpansions;

    // count of possible expansions, so that we can choose whichever node has the most possible expansions.
    public int possibleExpansions;

    // used to calculate how far we think a classification can permeate. 
    // For example, if i have 3 down expansions, each with 3 unique down expansions, we have 12 in our umbrella. we could update 12 nodes with one question.
    // the size of the umbrella is helpful to know which nodes are powerful in terms of their question giving us more info.
    public int totalUmbrellaCases;

    // underneath is all the recursive "not confirmed" nodes under. vice versa for above.
    public int underneathUmbrellaCases;
    public int aboveUmbrellaCases;


    // takes in a datapoint, and makes a copy of that and stores that as our "point"
    public Node(Integer[] datapoint){
        // copy the passed in datapoint to this node's point.
        values = Arrays.copyOf(datapoint, datapoint.length);
        
        // cache the sum for efficient sorting
        sum = sumUpDataPoint();
        
        // set classification to 0.
        classification = 0;
        classificationConfirmed = false;

        upExpansions = new Node[dimension];
        downExpansions = new Node[dimension];

        // Set the maximum possible classification value from Main
        maxPossibleValue = Main.highestPossibleClassification;
        possibleExpansions = 0;

        totalUmbrellaCases = 0;
        underneathUmbrellaCases = 0;
        aboveUmbrellaCases = 0;

    }

    // finds our up and down expansions. Since we know that the values of the expansions are just our point +- 1 in some attribute, we can just look it up.
    public void findExpansions(){

        // Parallel computation of expansions for each attribute
        IntStream.range(0, dimension).parallel().forEach(attribute -> {
            Integer[] key = Arrays.copyOf(values, values.length);
            
            // increment the value of key at this attribute, so we can find the one with + 1 in this digit.
            key[attribute]++;
            upExpansions[attribute] = Nodes.get(hash(key));

            // decrement by 2 to find the one with -1 in this attribute.
            key[attribute] -= 2;
            downExpansions[attribute] = Nodes.get(hash(key));
        });

        // Count possible expansions (sequential since it's fast)
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
        
        // Process all nodes sequentially to avoid any race conditions
        while(incrementCounter(finalKValsToMakeNode)){
            Node temp = Nodes.get(hash(finalKValsToMakeNode));
            temp.findExpansions();
        }
    

    }

    // BFS-based expansion to set floor (classification) for nodes above
    public void expandUp(int lowerBound){
        
        if (DEBUG_PRINTING) {
            System.out.println("=== EXPAND UP CALLED ===");
            System.out.println("Starting node: " + Arrays.toString(this.values) + " with lowerBound: " + lowerBound);
        }
        
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        java.util.Set<Node> visited = new java.util.HashSet<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            if (DEBUG_PRINTING) {
                System.out.println("Processing node: " + Arrays.toString(current.values) + 
                    " (classification=" + current.classification + 
                    ", maxPossibleValue=" + current.maxPossibleValue + 
                    ", confirmed=" + current.classificationConfirmed + ")");
            }
            
            // Skip if current node is already confirmed - we've already processed its implications
            if (current.classificationConfirmed) {
                
                if (DEBUG_PRINTING) {
                    System.out.println("  Skipping - already confirmed");
                }
                
                continue;
            }
            
            for (Node upstairsNeighbor : current.upExpansions) {
                
                if (DEBUG_PRINTING && upstairsNeighbor != null) {
                    System.out.println("  Checking upstairs neighbor: " + Arrays.toString(upstairsNeighbor.values) + 
                        " (classification=" + upstairsNeighbor.classification + 
                        ", maxPossibleValue=" + upstairsNeighbor.maxPossibleValue + 
                        ", confirmed=" + upstairsNeighbor.classificationConfirmed + ")");
                }

                // if we have a neighbor, who needs their floor raised, where we haven't been, and importantly, WHO IS NOT CONFIRMED.
                if (upstairsNeighbor != null && upstairsNeighbor.classification < lowerBound && !visited.contains(upstairsNeighbor) && upstairsNeighbor.classificationConfirmed == false) {
                    
                    if (DEBUG_PRINTING) {
                        System.out.println("    UPDATING: Setting classification from " + upstairsNeighbor.classification + " to " + lowerBound);
                    }
                    
                    // Update the floor (classification) of upstairs neighbor
                    upstairsNeighbor.classification = lowerBound;
                    queue.add(upstairsNeighbor);
                    visited.add(upstairsNeighbor);
                } 

                else if (DEBUG_PRINTING && upstairsNeighbor != null) {
                    System.out.println("\tSKIPPING because:");
                    if (upstairsNeighbor.classification >= lowerBound) {
                        System.out.println("\t\tclassification (" + upstairsNeighbor.classification + ") >= lowerBound (" + lowerBound + ")");
                    }
                    if (visited.contains(upstairsNeighbor)) {
                        System.out.println("\t\talready visited");
                    }
                    if (upstairsNeighbor.classificationConfirmed) {
                        System.out.println("\t\talready confirmed");
                    }
                }
            }
        }
        
        if (DEBUG_PRINTING) {
            System.out.println("=== CONFIRMATION CHECK PHASE ===");
            System.out.println("Checking " + visited.size() + " visited nodes for confirmation");
        }
        
        // Confirm nodes after propagation - check if floor equals ceiling
        // Sequential confirmation to avoid race conditions
        for (Node node : visited) {
            if (DEBUG_PRINTING) {
                System.out.println("Checking node " + Arrays.toString(node.values) + 
                    " for confirmation: classification=" + node.classification + 
                    ", maxPossibleValue=" + node.maxPossibleValue + 
                    ", node != this: " + (node != this));
            }
            
            if (node.classification == node.maxPossibleValue && node != this) {
                
                if (DEBUG_PRINTING) 
                    System.out.println("\u001B[31mCONFIRMING NODE: " + node + "\u001B[0m");
                
                    node.classificationConfirmed = true;
            } 
            
            else if (DEBUG_PRINTING) {    
                System.out.println("  NOT confirming - " + 
                    (node.classification != node.maxPossibleValue ? "values don't match" : "node is this"));
            }
            
        }
        
        if (DEBUG_PRINTING) {
            System.out.println("=== EXPAND UP COMPLETE ===\n");
        }
    }

    // BFS-based expansion to set ceiling (maxPossibleValue) for nodes below
    public void expandDown(int upperBound){
        if (DEBUG_PRINTING) {
            System.out.println("=== EXPAND DOWN CALLED ===");
            System.out.println("Starting node: " + Arrays.toString(this.values) + " with upperBound: " + upperBound);
        }
        
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        java.util.Set<Node> visited = new java.util.HashSet<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            if (DEBUG_PRINTING) {
                System.out.println("Processing node: " + Arrays.toString(current.values) + 
                    " (classification=" + current.classification + 
                    ", maxPossibleValue=" + current.maxPossibleValue + 
                    ", confirmed=" + current.classificationConfirmed + ")");
            }
            
            // Skip if current node is already confirmed - we've already processed its implications
            if (current.classificationConfirmed) {
                if (DEBUG_PRINTING) {
                    System.out.println("  Skipping - already confirmed");
                }
                continue;
            }
            
            for (Node downstairsNeighbor : current.downExpansions) {
                
                // DEBUG ONLY
                if (DEBUG_PRINTING && downstairsNeighbor != null) {
                    System.out.println("  Checking downstairs neighbor: " + Arrays.toString(downstairsNeighbor.values) + 
                        " (classification=" + downstairsNeighbor.classification + 
                        ", maxPossibleValue=" + downstairsNeighbor.maxPossibleValue + 
                        ", confirmed=" + downstairsNeighbor.classificationConfirmed + ")");
                }
                
                // if we have a neighbor, who needs their ceiling lowered, where we haven't been, and importantly, WHO IS NOT CONFIRMED.
                if (downstairsNeighbor != null && downstairsNeighbor.maxPossibleValue > upperBound && !visited.contains(downstairsNeighbor) && downstairsNeighbor.classificationConfirmed == false) {
                    
                    if (DEBUG_PRINTING) {
                        System.out.println("    UPDATING: Setting maxPossibleValue from " + downstairsNeighbor.maxPossibleValue + " to " + upperBound);
                    }
                    
                    // lower the ceiling (maxPossibleValue) of downstairs neighbor
                    downstairsNeighbor.maxPossibleValue = upperBound;
                    queue.add(downstairsNeighbor);
                    visited.add(downstairsNeighbor);

                // just for debugging
                } else if (DEBUG_PRINTING && downstairsNeighbor != null) {
                    System.out.println("    SKIPPING because:");
                    if (downstairsNeighbor.maxPossibleValue <= upperBound) {
                        System.out.println("      maxPossibleValue (" + downstairsNeighbor.maxPossibleValue + ") <= upperBound (" + upperBound + ")");
                    }
                    if (visited.contains(downstairsNeighbor)) {
                        System.out.println("      already visited");
                    }
                    if (downstairsNeighbor.classificationConfirmed) {
                        System.out.println("      already confirmed");
                    }
                }
            }
        }
        
        if (DEBUG_PRINTING) {
            System.out.println("=== CONFIRMATION CHECK PHASE ===");
            System.out.println("Checking " + visited.size() + " visited nodes for confirmation");
        }
        
        // Confirm nodes after propagation - check if floor equals ceiling
        // Sequential confirmation to avoid race conditions
        for (Node node : visited) {
            if (DEBUG_PRINTING) {
                System.out.println("Checking node " + Arrays.toString(node.values) + 
                    " for confirmation: classification=" + node.classification + 
                    ", maxPossibleValue=" + node.maxPossibleValue + 
                    ", node != this: " + (node != this));
            }
            
            if (node.classification == node.maxPossibleValue && node != this) {
                if (DEBUG_PRINTING) 
                    System.out.println("\u001B[31mCONFIRMING NODE: " + node + "\u001B[0m");
                node.classificationConfirmed = true;
            } else if (DEBUG_PRINTING) {
                System.out.println("  NOT confirming - " + 
                    (node.classification != node.maxPossibleValue ? "values don't match" : "node is this"));
            }
        }
        
        if (DEBUG_PRINTING) {
            System.out.println("=== EXPAND DOWN COMPLETE ===\n");
        }
    }

    // each node gets this new classification
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

    // Calculate umbrella size using proper graph traversal to avoid double counting
    public int calculateUmbrellaSize(boolean countUp) {
        java.util.Set<Node> visited = new java.util.HashSet<>();
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        
        queue.add(this);
        visited.add(this);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Node[] neighbors = countUp ? current.upExpansions : current.downExpansions;
            
            for (Node neighbor : neighbors) {
                if (neighbor != null && !neighbor.classificationConfirmed && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        return visited.size() - 1; // subtract self from count
    }

    // iterates through all the nodes.
    // Uses proper graph traversal to calculate umbrella sizes without double counting
    public static void updateUmbrellaSizes(ArrayList<Node> nodesToUpdate){

        // sort the nodes by their sum, increasing
        nodesToUpdate.sort((Node x, Node y) -> {
            return Integer.compare(x.sum, y.sum);
        });

        // Calculate underneath umbrella sizes using BFS traversal
        for (Node n : nodesToUpdate) {
            n.underneathUmbrellaCases = n.calculateUmbrellaSize(false); // false = count down
        }

        // sort decreasing now
        nodesToUpdate.sort((Node x, Node y) -> {
            return Integer.compare(y.sum, x.sum);
        });

        // Calculate above umbrella sizes using BFS traversal
        for (Node n : nodesToUpdate) {
            n.aboveUmbrellaCases = n.calculateUmbrellaSize(true); // true = count up
            n.totalUmbrellaCases = n.aboveUmbrellaCases + n.underneathUmbrellaCases;
        }

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
        }
        return s.toString();
    }
}

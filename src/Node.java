import java.util.Arrays;
import java.util.HashMap;

public class Node {

    // big list of nodes
    public static HashMap<Integer, Node> Nodes = new HashMap<Integer, Node>();
    
    // list of max possible value of each attribute
    public static Integer[] kValues;
    public static Integer dimension;

    public Integer[] values;
    public Integer classification;
    
    public Node[] upExpansions;
    public Node[] downExpansions;

    // takes in a datapoint, and makes a copy of that and stores that as our "point"
    public Node(Integer[] datapoint){
        // copy the passed in datapoint to this node's point.
        values = Arrays.copyOf(datapoint, datapoint.length);
        
        // set classification to -1.
        classification = -1;
    }

    // finds our up and down expansions. Since we know that the values of the expansions are just our point +- 1 in some attribute, we can just look it up.
    public void findExpansions(){

        upExpansions = new Node[dimension];
        downExpansions = new Node[dimension];

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
    }

    // hash function is easy. k value [i] * point val [i] as a running sum and then vals [0] gets added.
    public static Integer hash(Integer[] keyVal){

        int sum = 0;
        for(int i = 0; i < keyVal.length; i++){
            sum += (int)Math.pow(31, i) * keyVal[i];
        }
        return sum;
    }

    // makes all our nodes and populates the map
    public static void makeNodes(Integer[] kVals){

        Node.kValues = kVals;

        // set the dimension we are going to use all over
        dimension = kValues.length;

        Integer[] kValsToMakeNode = new Integer[dimension];
        for(int i = 0; i < dimension; i++)
            kValsToMakeNode[i] = 0;
        
        // put our first one in.
        Nodes.put(Node.hash(kValsToMakeNode), new Node(kValsToMakeNode));
        
        // iterate through all the digits, and make all the nodes. 
makingNodes:
        while(true){

            // if we have wrapped around, reset to 0. and continue.
            int attribute = 0;

            // incrementing logic to go through all digits, all k vals.
            while (kValsToMakeNode[attribute] + 1 >= kValues[attribute]){
             
                kValsToMakeNode[attribute] = 0;
                attribute++;

                // break once we've incremented all the way around.
                if (attribute >= dimension)
                    break makingNodes;
            }

            // increment our attribute
            kValsToMakeNode[attribute]++;

            System.out.println("Putting: " + Arrays.toString(kValsToMakeNode));

            // just make a new node and put it in the map.
            Nodes.put(Node.hash(kValsToMakeNode), new Node(kValsToMakeNode));
        }
    
        // re initialize so we can copy paste
        for(int i = 0; i < dimension; i++)
            kValsToMakeNode[i] = 0;


        Nodes.get(hash(kValsToMakeNode)).findExpansions();    
    expanding:
        while(true){

            int attribute = 0;

            // incrementing logic to go through all digits, all k vals.
            while (kValsToMakeNode[attribute] + 1 >= kValues[attribute]){
             
                kValsToMakeNode[attribute] = 0;
                attribute++;

                // break once we've incremented all the way around.
                if (attribute >= dimension)
                    break expanding;
            }

            // increment our attribute
            kValsToMakeNode[attribute]++;

            Node temp = Nodes.get(hash(kValsToMakeNode));
            temp.findExpansions();
        }
    

    }

    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append("DATAPOINT:\n\t");
        s.append(Arrays.toString(values)).append("\n");

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

        return s.toString();
    }

}


public class Main {

    public static void main(String[] args) {

        Integer[] kValues = {2,3,2};
        Node.makeNodes(kValues);

        Integer[] kValsToMakeNode = new Integer[kValues.length];
        for(int i = 0; i < kValsToMakeNode.length; i++)
            kValsToMakeNode[i] = 0;
        while(true){

            // if we have wrapped around, reset to 0. and continue.
            int attribute = 0;

            // incrementing logic to go through all digits, all k vals.
            while (kValsToMakeNode[attribute] + 1 >= kValues[attribute]){
             
                kValsToMakeNode[attribute] = 0;
                attribute++;

                // break once we've incremented all the way around.
                if (attribute >= kValues.length)
                    return;

                Node temp = Node.Nodes.get(Node.hash(kValsToMakeNode));

                System.out.println(temp);


            }

            // increment our attribute
            kValsToMakeNode[attribute]++;
        }

    }
}
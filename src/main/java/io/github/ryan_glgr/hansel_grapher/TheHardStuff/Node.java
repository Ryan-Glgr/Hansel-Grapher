package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.ryan_glgr.hansel_grapher.Stats.PermeationStats;
import org.roaringbitmap.RoaringBitmap;

public class Node {

    public static boolean DEBUG_PRINTING = false;

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

    // stores whether we can reach each node from all other nodes.
    public int nodeID = -1; // needed for the bit mapping operations.
    public RoaringBitmap reachableNodesAbove;
    public RoaringBitmap reachableNodesBelow;

    // stores the number of possible confirmations by class in this way.
    // hypothetically compute how many confirmations we would get by assigning this node to each class. and store them respectively.
    public Integer[] possibleConfirmationsByClass;

    // used as a different measure of how "good" an umbrella is. basically, we want a node which has a lot of nodes in umbrella, and they're balanced.
    // so we take the ratio with the total number / the difference in above and below cases. 
    public double balanceRatio;

    public double umbrellaMagnitude;

    // takes in a datapoint, and makes a copy of that and stores that as our "point"
    private Node(Integer[] datapoint,
                 int numClasses,
                 int dimension,
                 int nodeID) {
        // copy the passed in datapoint to this node's point.
        this.values = Arrays.copyOf(datapoint, datapoint.length);

        // set classification to 0.
        this.classification = 0;
        this.classificationConfirmed = false;

        this.upExpansions = new Node[dimension];
        this.downExpansions = new Node[dimension];

        // Set the maximum possible classification value from Main
        this.maxPossibleValue = numClasses - 1;

        this.totalUmbrellaCases = 0;
        this.underneathUmbrellaCases = 0;
        this.aboveUmbrellaCases = 0;

        this.possibleConfirmationsByClass = new Integer[numClasses];
        Arrays.fill(this.possibleConfirmationsByClass, 0);
        this.balanceRatio = 0.0f;

        this.sum = sumUpDataPoint();
        this.nodeID = nodeID;
    }

    public Node(Node n) {
        this(n.values, n.maxPossibleValue + 1, n.upExpansions.length, n.nodeID);
    }

    public static String printListOfNodes(List<Node> nodes) {
        List<String> valuesStrings = nodes.stream()
                .map(node -> "\n" + Arrays.toString(node.values))
                .toList();
        return valuesStrings.toString();
    }

    private void findExpansions(HashMap<Integer, Node> nodes, int dimension) {

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
    public static Integer hash(Integer[] keyVal) {
        long sum = 0;
        for (int i = 0; i < keyVal.length; i++) {
            sum += (long) Math.pow(31, i) * keyVal[i];
        }
        return (int) (sum % Integer.MAX_VALUE);
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
        int lastNodeID = 0;
        while (incrementCounter(kValsToMakeNode, kVals)) {
            // just make a new node and put it in the map.
            nodes.put(Node.hash(kValsToMakeNode),
                    new Node(kValsToMakeNode,
                            numClasses,
                            dimension,
                            lastNodeID++));
        }

        // re initialize so we can copy paste
        Integer[] finalKValsToMakeNode = counterInitializer(kVals);
        while (incrementCounter(finalKValsToMakeNode, kVals)) {
            Node temp = nodes.get(hash(finalKValsToMakeNode));
            temp.findExpansions(nodes, dimension);
        }

        // now we do a BFS both up and down, and determine whether each node is reachable from each node.
        nodes.values().parallelStream().forEach(node -> {
            node.reachableNodesAbove = node.findAllNodesReachable(true);
            node.aboveUmbrellaCases = node.reachableNodesAbove.getCardinality();
            node.reachableNodesBelow = node.findAllNodesReachable(false);
            node.underneathUmbrellaCases = node.reachableNodesBelow.getCardinality();
        });

        return nodes;
    }

    private PermeationStats expand(final int bound, final boolean countUpwards) {
        // BFS-based expansion to set ceiling of below nodes, and floor of above nodes.
        final Queue<Node> queue = new LinkedList<>();
        final Set<Node> visited = new HashSet<>();
        RoaringBitmap nodesConfirmed = new RoaringBitmap();
        RoaringBitmap nodesWithBoundChanges = new RoaringBitmap();
        Integer numberOfNodesTouched = 0;
        queue.add(this);
        visited.add(this);

        while (!queue.isEmpty()) {

            final Node current = queue.poll();
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
                    if (neighborNeedsUpdate) {
                        neighbor.classification = bound;
                        nodesWithBoundChanges.add(neighbor.nodeID);
                        numberOfNodesTouched++;
                        queue.add(neighbor);
                        visited.add(neighbor);
                    }
                } else {
                    final boolean neighborNeedsUpdate = neighbor != null && !neighbor.classificationConfirmed && neighbor.maxPossibleValue > bound && !visited.contains(neighbor);
                    if (neighborNeedsUpdate) {
                        neighbor.maxPossibleValue = bound;
                        nodesWithBoundChanges.add(neighbor.nodeID);
                        numberOfNodesTouched++;
                        queue.add(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
        }

        // Confirm nodes after propagation - check if floor equals ceiling
        for (final Node node : visited) {
            final boolean nodeWasConfirmed = Objects.equals(node.classification, node.maxPossibleValue) && node != this;
            if (nodeWasConfirmed) {
                node.classificationConfirmed = true;
                nodesConfirmed.add(node.nodeID);
            }
        }

        Integer numberOfNodesTouchedAbove = 0;
        Integer numberOfNodesTouchedBelow = 0;
        if (countUpwards) {
            numberOfNodesTouchedAbove = numberOfNodesTouched;
        } else {
            numberOfNodesTouchedBelow = numberOfNodesTouched;
        }
        return new PermeationStats(nodesConfirmed.getCardinality(),
                numberOfNodesTouchedAbove,
                numberOfNodesTouchedBelow,
                nodesConfirmed,
                nodesWithBoundChanges);
    }

    // each node gets this new classification. it sends the effects of the classification up and down. (not just within one HC, but to all expansions up and down.)
    public PermeationStats permeateClassification(final int newClassification) {
        // lock in our nodes value
        this.classification = newClassification;
        this.maxPossibleValue = this.classification;

        // Set the floor of everyone above to AT LEAST this value
        final PermeationStats aboveStats = expand(this.classification, true);

        // Set the ceiling of everyone below to AT MOST this value
        final PermeationStats belowStats = expand(this.classification, false);

        // get our final stats
        final PermeationStats finalStats = new PermeationStats(aboveStats, belowStats);
        // this node itself was confirmed because we asked about it directly.
        this.classificationConfirmed = true;
        finalStats.nodesConfirmed.add(this.nodeID);

        // return our stats from this permeation.
        return finalStats;
    }

    private void removeConfirmedNodesFromReachableSet(final RoaringBitmap nodesThatGotConfirmed) {
        this.reachableNodesBelow.andNot(nodesThatGotConfirmed);
        this.reachableNodesAbove.andNot(nodesThatGotConfirmed);

        // updating here even though it is kind of redundant since we also do it after updating the reachable set
        this.aboveUmbrellaCases = this.reachableNodesAbove.getCardinality();
        this.underneathUmbrellaCases = this.reachableNodesBelow.getCardinality();
    }

    private void removeUpdatedNodesFromReachableSet(final RoaringBitmap nodesWithBoundChanges,
                                                    final Map<Integer, Node> nodeByID) {
        RoaringBitmap toRemoveFromAbove = new RoaringBitmap();
        RoaringBitmap toRemoveFromBelow = new RoaringBitmap();
        RoaringBitmap relevantAbove = RoaringBitmap.and(nodesWithBoundChanges, this.reachableNodesAbove);
        RoaringBitmap relevantBelow = RoaringBitmap.and(nodesWithBoundChanges, this.reachableNodesBelow);

        relevantAbove.forEach((int updatedNodeID) -> {
            Node updatedNode = nodeByID.get(updatedNodeID);
            if (updatedNode != null && this.maxPossibleValue <= updatedNode.classification) {
                toRemoveFromAbove.add(updatedNodeID);
            }
        });

        relevantBelow.forEach((int updatedNodeID) -> {
            Node updatedNode = nodeByID.get(updatedNodeID);
            if (updatedNode != null && this.classification >= updatedNode.maxPossibleValue) {
                toRemoveFromBelow.add(updatedNodeID);
            }
        });

        this.reachableNodesAbove.andNot(toRemoveFromAbove);
        this.reachableNodesBelow.andNot(toRemoveFromBelow);

        this.aboveUmbrellaCases = this.reachableNodesAbove.getCardinality();
        this.underneathUmbrellaCases = this.reachableNodesBelow.getCardinality();
    }

    // For updated nodes: check all nodes in MY reachable sets
    private void cleanOwnReachableSets(Map<Integer, Node> nodeByID) {
        RoaringBitmap toRemoveAbove = new RoaringBitmap();
        RoaringBitmap toRemoveBelow = new RoaringBitmap();

        this.reachableNodesAbove.forEach((int nodeID) -> {
            Node other = nodeByID.get(nodeID);
            if (other != null && this.maxPossibleValue <= other.classification) {
                toRemoveAbove.add(nodeID);
            }
        });

        this.reachableNodesBelow.forEach((int nodeID) -> {
            Node other = nodeByID.get(nodeID);
            if (other != null && this.classification >= other.maxPossibleValue) {
                toRemoveBelow.add(nodeID);
            }
        });

        this.reachableNodesAbove.andNot(toRemoveAbove);
        this.reachableNodesBelow.andNot(toRemoveBelow);
        this.aboveUmbrellaCases = this.reachableNodesAbove.getCardinality();
        this.underneathUmbrellaCases = this.reachableNodesBelow.getCardinality();
    }


    // assume we had a node who's min classifications by class were [1, 2, 2] and another who's min classifications by class were:
    //  [0 (because it is guaranteed to NOT be this class by monotonicity already), 6, 12] we would choose the first, since it's min is lower.
    // but in reality, we want that second one, since the first class is just not possible. the real min is 6. not 0. so we have to have a flag for not set.
    // the reason it's max value, is so that when we sort the counts, this number last still, and will serve as a tiebreaker
    private static final Integer NOT_SET = Integer.MAX_VALUE;

    // does a BFS from each node, updating rankings as we go. Ranking our umbrella size and the minimum classifications.
    public static void updateAllNodeRankings(ArrayList<Node> aliveNodes,
                                             final BalanceRatio balanceRatio,
                                             final int numClasses,
                                             final PermeationStats statsFromLastUpdate) {

        // this would happen on the FIRST question asked of the day.
        if (statsFromLastUpdate == null)
            return;

        final HashMap<Integer, Node> allNodesToTheirIDsMap = new HashMap<>();
        aliveNodes.forEach(node -> allNodesToTheirIDsMap.put(node.nodeID, node));

        final RoaringBitmap nodesConfirmed = statsFromLastUpdate.nodesConfirmed;
        final RoaringBitmap nodesUpdated = statsFromLastUpdate.nodesWithBoundChanges;

        // for all nodes, if they weren't confirmed, we are going to remove confirmed from their list, and remove nodes which
        // they can no longer update.
        aliveNodes.parallelStream()
                .filter(node -> !nodesConfirmed.contains(node.nodeID))
                .forEach(node -> {
                    node.removeConfirmedNodesFromReachableSet(nodesConfirmed);
                    if (nodesUpdated.contains(node.nodeID)){
                        node.cleanOwnReachableSets(allNodesToTheirIDsMap);
                    }
                    node.removeUpdatedNodesFromReachableSet(nodesUpdated, allNodesToTheirIDsMap);
                    node.totalUmbrellaCases = node.aboveUmbrellaCases + node.underneathUmbrellaCases;
                });

        RoaringBitmap[] nodesThatWouldConfirmForEachClassCountingUpwards = new RoaringBitmap[numClasses];
        for (int i = 0; i < numClasses; i++) {
            nodesThatWouldConfirmForEachClassCountingUpwards[i] = new RoaringBitmap();
        }
        RoaringBitmap[] nodesThatWouldConfirmForEachClassCountingDownwards = new RoaringBitmap[numClasses];
        for (int i = 0; i < numClasses; i++) {
            nodesThatWouldConfirmForEachClassCountingDownwards[i] = new RoaringBitmap();
        }
        // determine whether each node is going to be confirmed for each class, counting both up and downwards
        for (Node n : aliveNodes) {
            for (int classification = n.classification; classification <= n.maxPossibleValue; classification++) {
                // flag whether this node would be confirmed for each class or not, counting both up and down
                if (n.wouldBeConfirmedForClass(classification, true)) {
                    nodesThatWouldConfirmForEachClassCountingUpwards[classification].add(n.nodeID);
                }

                if (n.wouldBeConfirmedForClass(classification, false)) {
                    nodesThatWouldConfirmForEachClassCountingDownwards[classification].add(n.nodeID);
                }
            }
        }

        aliveNodes.parallelStream()
                .forEach(node -> {
                    Arrays.fill(node.possibleConfirmationsByClass, NOT_SET);
                    for (int classification = node.classification; classification <= node.maxPossibleValue; classification++) {
                        node.possibleConfirmationsByClass[classification] = 0;
                    }

                    // now we must have each node go through the nodesThatWouldConfirm for each class, and increment their counts in these cases:
                    // if the target node is in reachableBelow for a given node, we check if it would be confirmed counting downards for each class
                    // if the target node is above, we check if it would be confirmed counting upwards for each class which our node can still be.
                    node.updateConfirmationStats(nodesThatWouldConfirmForEachClassCountingUpwards, true);
                    node.updateConfirmationStats(nodesThatWouldConfirmForEachClassCountingDownwards, false);
                    Arrays.sort(node.possibleConfirmationsByClass);

                    // compute the new magnitude of above and below umbrella case vector
                    node.umbrellaMagnitude = node.computeUmbrellaMagnitude();
                    node.balanceRatio = balanceRatio.computeBalanceRatio(node);
                });
    }

    private void updateConfirmationStats(final RoaringBitmap[] nodesThatWouldConfirmForEachClass,
                                         final boolean countUpwards) {

        RoaringBitmap reachableNodes = countUpwards
                ? this.reachableNodesAbove
                : this.reachableNodesBelow;

        for (int hypotheticalClass = this.classification; hypotheticalClass <= this.maxPossibleValue; hypotheticalClass++) {
            this.possibleConfirmationsByClass[hypotheticalClass] += RoaringBitmap.andCardinality(reachableNodes, nodesThatWouldConfirmForEachClass[hypotheticalClass]);
        }
    }

    // builds the reachable set when we create a node, does this by doing a BFS to all possible nodes both up and down
    private RoaringBitmap findAllNodesReachable(final boolean upWardsUmbrella) {
        RoaringBitmap reachableNodes = new RoaringBitmap();
        Queue<Node> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(this);
        visited.add(this.nodeID);

        while (!queue.isEmpty()) {
            final Node current = queue.poll();
            final Node[] neighbors = upWardsUmbrella
                    ? current.upExpansions
                    : current.downExpansions;

            for (Node neighbor : neighbors) {
                if (neighbor != null && !visited.contains(neighbor.nodeID)) {
                    reachableNodes.add(neighbor.nodeID);
                    visited.add(neighbor.nodeID);
                    queue.add(neighbor);
                }
            }
        }
        return reachableNodes;
    }

    // determines if a neighbor would get confirmed if it's upstairs or downstairs neighbor were given "hypotheticalClass"
    private boolean wouldBeConfirmedForClass(int hypotheticalClass, boolean countingUpwards) {
        // return false if we're already confirmed.
        if (this.classificationConfirmed) {
            return false;
        }

        // if we are counting upwards. this is ABOVE the node which called this on us.
        if (countingUpwards) {
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
    public boolean isDominatedBy(Node otherNode, boolean otherNodeOnTop) {

        // look for ONE digit, where our boy isn't getting covered. if otherNode is greater or equal 
        //(less or equal if otherNodeOnTop is false, meaning 'this' node is on top) in all attributes, we are covered.
        for (int digitPosition = 0; digitPosition < this.values.length; digitPosition++) {
            int thisValue = this.values[digitPosition];
            int otherValue = otherNode.values[digitPosition];

            if (otherNodeOnTop && thisValue > otherValue) {
                return false;
            } else if (!otherNodeOnTop && thisValue < otherValue) {
                return false;
            }
        }
        return true;
    }

    public double computeUmbrellaMagnitude() {
        return Math.sqrt(Math.pow(aboveUmbrellaCases, 2) + Math.pow(underneathUmbrellaCases, 2));
    }

    // used when we are computing the number of umbrella cases. we need to sort by the hamming value of the case.
    public Integer sumUpDataPoint() {
        Integer sum = 0;
        for (Integer x : this.values) {
            sum += x;
        }
        return sum;
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

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("DATAPOINT:\n\t");
        s.append(Arrays.toString(values)).append("\n");

        s.append("CLASSIFICATION:\t").append(classification).append("\n");

        if (DEBUG_PRINTING) {
            s.append("UP EXPANSIONS:\n");
            for (Node t : upExpansions) {
                if (t == null)
                    s.append("\tNULL\n");

                else
                    s.append("\t").append(Arrays.toString(t.values)).append("\n");
            }

            s.append("DOWN EXPANSIONS:\n");
            for (Node t : downExpansions) {
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
            for (int i = 0; i < possibleConfirmationsByClass.length; i++) {
                s.append("\tClass: " + i + ": " + possibleConfirmationsByClass[i] + "\n");
            }

        }
        return s.toString();
    }
}

package io.github.ryan_glgr.hansel_grapher.TheHardStuff;

public enum InterviewMode {

    // methods which use a more traditional binary search. meaning that we are going to fully classify one chain, before
    // we go on and binary search the next one. BINARY_SEARCH_CHUNKS will simply grab the middle node of the longest chunk of a
    // chain which isn't classified yet.
    TRADITIONAL_BINARY_SEARCH,
    TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE,
    TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED,
    TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT,
    TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY,
    TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY,
    TRADITIONAL_BINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC,

    BINARY_SEARCH_CHUNKS,                       // method where we just query midpoint of the chain each time. thus chopping each chain in half. we work on the longest chain at a time.
    BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_SMALLEST_DIFFERENCE,
    BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BEST_MIN_CONFIRMED,
    BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT,
    BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_UNITY,
    BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY,
    BINARY_SEARCH_CHUNKS_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC,
    BINARY_SEARCH_LONGEST_STRING_OF_EXPANSIONS, // basically finds the longest expansion chain, + 1 in some attribute, as far as we can go, and binary searches that chain at each step.

    NONBINARY_SEARCH_CHAINS,
    NONBINARY_SEARCH_COMPLETING_SQUARE_SMALLEST_DIFFERENCE,
    NONBINARY_SEARCH_COMPLETING_SQUARE_BEST_MIN_CONFIRMED,
    NONBINARY_SEARCH_COMPLETING_SQUARE_HIGHEST_TOTAL_UMBRELLA_SORT,
    NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_UNITY,
    NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_SHANNON_ENTROPY,
    NONBINARY_SEARCH_COMPLETING_SQUARE_BALANCE_RATIO_QUADRATIC,

    BEST_MINIMUM_CONFIRMED,                     // method where we check all nodes, and determine which has the best min bound. meaning of all k classes, confirming this one as a particular class, how many nodes get confirmed. The node with the best lower bound is used each iteration.

    HIGHEST_TOTAL_UMBRELLA_SORT,                // sort by just the total amount in the umbrella. This means we find the node who's classification affects the most other nodes.
    SMALLEST_DIFFERENCE_UMBRELLA_SORT,          // sort our nodes by the smallest difference above/below. this way we find balanced nodes first

    BEST_BALANCE_RATIO_UNITY,
    BEST_BALANCE_RATIO_SHANNON_ENTROPY,
    BEST_BALANCE_RATIO_QUADRATIC,
}

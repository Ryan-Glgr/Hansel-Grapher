## PROJECT GOALS ##
The goal of this project is very simple. It serves as a simplistic, understandable, much faster port of the CWU-VKD-LAB MOEKA project. We continue on this task of creating hansel chains and reducing the number of questions asked to an expert by using the properties of monotonicity. We explore new question ordering techniques, and new ways of reducing our final Disjunctive Normal Form function to reduce complexity to an SME.

## TASKS ## 
- Find a way to apply our problem where "asking a question" is a big heavy operation/and or when asking what we are modeling is a very hard domain, where it makes sense to need to classify all possible Nodes, not just find a min/max.
- Create a "standard binary search"
- Refactor k values to use a structure which allows functions and sub functions
- implement even better clause reduction technique(s) than the greedy search.
- implement full end to end functionality for gui. fire it up, make an interview, specify a mode or modes, see question graphs, save outputs
- simple visualization of "new points"
    - perhaps visualize all the low units, and correspondingly, the "high unit" or highest value UNDER each low unit. so that we can show the borders. maybe show each low unit, and their direct underneath neighbors and above neighbors?


## RUNNING ##
- Requires modern maven to run so install maven for your system (package manager/ installer).
- To run the program, simply `./run.sh`. You may need to give it the old `chmod +x run.sh`.
- Alternatively you can run `mvn clean compile`, to compile, and `mvn exec:java` to run. 
- Running in debug gives you more logging, and more info in the pictures as well. To do this './run.sh debug'
- To view logs, 'cat debug' after a run

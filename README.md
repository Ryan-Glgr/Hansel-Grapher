## PROJECT GOALS ##
The goal of this project is very simple. It serves as a simplistic, understandable, much faster port of the CWU-VKD-LAB MOEKA project. We continue on this task of creating hansel chains and reducing the number of questions asked to an expert by using the properties of monotonicity.

## TASKS ## 
- An interactive Interview implementation, and a ML interview implementation.
- A way to make the outputs interactive. This can be quite simple actually. just don't end the program after making the pdf's. Then you look at them, and call some function which updates the nodes you want to update, by looking them up in the map, and then on said node, we call the permeate classification method.
- More advanced heuristics which maybe combine different heuristics which work well in different times.
- Find a way to apply our problem where "asking a question" is a big heavy operation/and or when asking what we are modeling is a very hard domain, where it makes sense to need to classify all possible Nodes, not just find a min/max.
- Different heuristics which will help us reduce clauses needed in the RuleTree implementation.
- Implementing using the adjacent node concept in the Interview phase. if we are grabbing a particular node, determine if there are any adjacent nodes which can cover it's whole chain instead.
  - If a node is present in the chosen nodes, above/below nodes up and down expansions respectively, it is a viable suitor to replace said chosen node.
  - This can be useful in any time where we are doing a binary search.


## RUNNING ##
- Requires modern maven to run so install maven for your system (package manager/ installer).
- To run the program, simply `./run.sh`. You may need to give it the old `chmod +x run.sh`.
- Alternatively you can run `mvn clean compile`, to compile, and `mvn exec:java` to run. 
- Running in debug gives you more logging, and more info in the pictures as well. To do this './run.sh debug'
- To view logs, 'cat debug' after a run

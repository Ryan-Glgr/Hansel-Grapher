## PROJECT GOALS ##
The goal of this project is very simple. It serves as a simplistic, understandable, much faster port of the CWU-VKD-LAB MOEKA project. We continue on this task of creating hansel chains and reducing the number of questions asked to an expert by using the properties of monotonicity. We explore new question ordering techniques, and new ways of reducing our final Disjunctive Normal Form function to reduce complexity to an SME.

## TASKS ## 
- Find a way to apply our problem where "asking a question" is a big heavy operation/and or when asking what we are modeling is a very hard domain, where it makes sense to need to classify all possible Nodes, not just find a min/max.
- implement even better clause reduction technique(s) than the greedy search.
- Implement "expert adjustments" post interview. This allows us to interactively adjust the model. To do this, we will need a method somewhat like the permeateClassification method. This will start at chosen node N, manually assign it the classification given, and:
  - If node N was adjusted to a LOWER classification, we should then traverse to all nodes underneath N, and update their classification to be that of node N (or leave as is, if they were already at or below this classification).
  - If node N was adjusted to a higher classification, we should traverse to all nodes ABOVE N, and update their classifications to this classification given for N, of course leaving as is if they were already at or above this classification.

## RUNNING ##
- Requires modern maven to run so install maven for your system (package manager/ installer).
- To run the program, simply `./run.sh`. You may need to give it the old `chmod +x run.sh`.
- Alternatively you can run `mvn clean compile`, to compile, and `mvn exec:java` to run. 
- Running in debug gives you more logging, and more info in the pictures as well. To do this './run.sh debug'
- To view logs, 'cat debug' after a run

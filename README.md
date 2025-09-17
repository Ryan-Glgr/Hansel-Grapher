## PROJECT GOALS ##
The goal of this project is very simple. It serves as a simplistic, understandable, much faster port of the CWU-VKD-LAB MOEKA project. We continue on this task of creating hansel chains and reducing the number of questions asked to an expert by using the properties of monotonicity.

## TASKS ## 
- An interactive Interview implementation, and a ML interview implementation.
- A way to make the outputs interactive. This can be quite simple actually. just don't end the program after making the pdf's. Then you look at them, and call some function which updates the nodes you want to update, by looking them up in the map, and then on said node, we call the permeate classification method.
- More advanced heuristics which maybe combine different heuristics which work well in different times.
- Find a way to apply our problem where "asking a question" is a big heavy operation/and or when asking what we are modeling is a very hard domain, where it makes sense to need to classify all possible Nodes, not just find a min/max.

## RUNNING ##
- To run the program, cd to the src directory, and simply ./run.sh. you may need to give it the old chmod +x.
- Running in debug gives you more logging, and more info in the pictures as well. to do this './run.sh debug'
- To view logs, 'cat debug' after a run

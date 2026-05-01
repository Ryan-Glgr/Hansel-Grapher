## PROJECT GOALS ##
The goal of this project is very simple. It serves as a simplistic, understandable, much faster port of the CWU-VKD-LAB MOEKA project. We continue on this task of creating hansel chains and reducing the number of questions asked to an expert by using the properties of monotonicity. We explore new question ordering techniques, and new ways of reducing our final Disjunctive Normal Form function to reduce complexity to an SME.

## TASKS ## 
- Find a way to apply our problem where "asking a question" is a big heavy operation/and or when asking what we are modeling is a very hard domain, where it makes sense to need to classify all possible Nodes, not just find a min/max.
- implement even better clause reduction technique(s) than the greedy search.
- Implement "expert adjustments" post interview. This allows us to interactively adjust the model. To do this, we will need a method somewhat like the permeateClassification method. This will start at chosen node N, manually assign it the classification given, and:
  - If node N was adjusted to a LOWER classification, we should then traverse to all nodes underneath N, and update their classification to be that of node N (or leave as is, if they were already at or below this classification).
  - If node N was adjusted to a higher classification, we should traverse to all nodes ABOVE N, and update their classifications to this classification given for N, of course leaving as is if they were already at or above this classification.
  - Node N can be DELETED if deemed "infeasible". If Node N is infeasible because it is physically impossible - either it is too high, or too low, it should permeate that deletion to all such nodes which are above/below that node. this is POWERFUL! So should be handled carefully. Perhaps we don't actually delete the nodes, but rather mark them as some class INT_MIN or something, and color them grey in visualization. (This done more for code sanity than for mathemtical purposes).

## REQUIREMENTS ##
- **Java 21** - This project requires Java 21 or higher
- **Maven** - Modern maven for building and running
- **Graphviz** - For generating visualizations

### Java 21 Setup

Check if you have Java 21 installed:
```bash
java -version
```

If you don't have Java 21, install it:
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# Fedora/RHEL
sudo dnf install java-21-openjdk-devel

# macOS (using Homebrew)
brew install openjdk@21
```

### Setting JAVA_HOME (Required for Gradle)

If you get a "Cannot find a Java installation" error, you need to set `JAVA_HOME`:

**Linux/macOS:**
```bash
# Find your Java installation path
readlink -f $(which java)
# This will show something like: /usr/lib/jvm/java-21-openjdk-amd64/bin/java

# Set JAVA_HOME permanently (use the path WITHOUT /bin/java)
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Verify it's set
echo $JAVA_HOME
```

**For zsh users**, replace `~/.bashrc` with `~/.zshrc`

### Installing Graphviz

```bash
# Ubuntu/Debian
sudo apt install graphviz

# Fedora/RHEL
sudo dnf install graphviz

# macOS
brew install graphviz
```

## RUNNING ##
- Requires modern maven to run so install maven for your system (package manager/ installer).
- To run the program, simply `./run.sh`. You may need to give it the old `chmod +x run.sh`.
- Alternatively you can run `mvn clean compile`, to compile, and `mvn exec:java` to run. 
- Running in debug gives you more logging, and more info in the pictures as well. To do this './run.sh debug'
- To view logs, 'cat debug' after a run
- You will need `graphviz` installed on your machine. This [link] (https://enterprise-architecture.org/university/graphviz-install/) may be useful, or you can ask AI to give you the CLI command to install.

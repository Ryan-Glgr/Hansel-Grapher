package io.github.ryan_glgr.hansel_grapher.Stats;

import java.util.Arrays;
import java.util.List;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;

// data class used to track how we did on a particular run of an interview.
public class InterviewStats {
    
    // general data about a particular run's configuration.
    public Integer[] kValues;
    public int numberOfHanselChains;
    public int numberOfNodes;
    public InterviewMode interviewMode;
    public boolean expertMode; 

    public List<Node> nodesAsked;
    public List<PermeationStats> permeationStatsForEachNodeAsked;

    public InterviewStats(Integer[] kValues,
            int numHanselChains,
            int numNodes,
            InterviewMode interviewMode,
            boolean expertMode,
            List<Node> nodesAsked,
            List<PermeationStats> permeationStatsForEachNodeAsked) {

        this.kValues = Arrays.copyOf(kValues, kValues.length); // copying because we pass from the static field.
        this.numberOfHanselChains = numHanselChains;
        this.numberOfNodes = numNodes;        
        this.interviewMode = interviewMode;
        this.expertMode = expertMode;
        this.nodesAsked = nodesAsked;
        this.permeationStatsForEachNodeAsked = permeationStatsForEachNodeAsked;
    }

    public InterviewStats(List<Node> nodesAsked, List<PermeationStats> permeationStats){
        this.nodesAsked = nodesAsked;
        this.permeationStatsForEachNodeAsked = permeationStats;
    }

    public String toString(){

        StringBuilder sb = new StringBuilder();

        sb.append("kValues: ").append(Arrays.toString(kValues)).append("\n");
        sb.append("numberOfHanselChains: ").append(numberOfHanselChains).append("\n");
        sb.append("numberOfNodes: ").append(numberOfNodes).append("\n");
        sb.append("numberOfQuestions: ").append(nodesAsked.size()).append("\n");
        sb.append("interviewMode: ").append(interviewMode).append("\n");
        sb.append("expertMode: ").append(expertMode).append("\n");

        PermeationStats[] permeationStatsArray = permeationStatsForEachNodeAsked.toArray(new PermeationStats[0]);
        Node[] questionsAsked = nodesAsked.toArray(new Node[0]);
        for(int i = 0; i < questionsAsked.length; i++){
            sb.append(questionsAsked[i]);
            sb.append("\n");
            sb.append(permeationStatsArray[i]);
            sb.append("\n------------------------------------------------\n");
        }
        return sb.toString();
    }

}

package io.github.ryan_glgr.hansel_grapher.stats;

import java.util.Arrays;
import java.util.List;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.MagicFunctionMode;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;

// data class used to track how we did on a particular run of an interview.
public class InterviewStats {
    
    // general data about a particular run's configuration.
    public Integer[] kValues;
    public int numberOfHanselChains;
    public int numberOfNodes;
    public InterviewMode interviewMode;
    public MagicFunctionMode magicFunctionMode;

    public List<Node> nodesAsked;
    public List<PermeationStats> permeationStatsForEachNodeAsked;

    public InterviewStats(final Integer[] kValues,
                          final int numHanselChains,
                          final int numNodes,
                          final InterviewMode interviewMode,
                          final MagicFunctionMode magicFunctionMode,
                          final List<Node> nodesAsked,
                          final List<PermeationStats> permeationStatsForEachNodeAsked) {

        this.kValues = Arrays.copyOf(kValues, kValues.length);
        this.numberOfHanselChains = numHanselChains;
        this.numberOfNodes = numNodes;
        this.interviewMode = interviewMode;
        this.magicFunctionMode = magicFunctionMode;
        this.nodesAsked = nodesAsked;
        this.permeationStatsForEachNodeAsked = permeationStatsForEachNodeAsked;
    }

    public InterviewStats(final List<Node> nodesAsked, final List<PermeationStats> permeationStats){
        this.nodesAsked = nodesAsked;
        this.permeationStatsForEachNodeAsked = permeationStats;
    }

    public String toString(){

        final StringBuilder sb = new StringBuilder();

        sb.append("kValues: ").append(Arrays.toString(kValues)).append("\n");
        sb.append("numberOfHanselChains: ").append(numberOfHanselChains).append("\n");
        sb.append("numberOfNodes: ").append(numberOfNodes).append("\n");
        sb.append("numberOfQuestions: ").append(nodesAsked.size()).append("\n");
        sb.append("interviewMode: ").append(interviewMode).append("\n");
        sb.append("expertMode: ").append(magicFunctionMode).append("\n");

        final PermeationStats[] permeationStatsArray = permeationStatsForEachNodeAsked.toArray(new PermeationStats[0]);
        final Node[] questionsAsked = nodesAsked.toArray(new Node[0]);
        for(int i = 0; i < questionsAsked.length; i++){
            sb.append(questionsAsked[i]);
            sb.append("\n");
            sb.append(permeationStatsArray[i]);
            sb.append("\n------------------------------------------------\n");
        }
        return sb.toString();
    }

}

package io.github.ryan_glgr.hansel_grapher.Stats;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Node;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.HashSet;

public class PermeationStats {

    public Integer numberOfConfirmations;
    public Integer totalNumberOfNodesTouched;
    public Integer numberOfNodesTouchedAbove;
    public Integer numberOfNodesTouchedBelow;
    public RoaringBitmap nodesConfirmed;
    public RoaringBitmap nodesWithBoundChanges;

    public PermeationStats(Integer numberOfConfirmations, Integer numberOfNodesTouchedAbove, Integer numberOfNodesTouchedBelow, RoaringBitmap nodesConfirmed, RoaringBitmap nodesWithBoundChanges) {
        this.numberOfConfirmations = numberOfConfirmations;
        this.numberOfNodesTouchedAbove = numberOfNodesTouchedAbove;
        this.numberOfNodesTouchedBelow = numberOfNodesTouchedBelow;
        this.totalNumberOfNodesTouched = numberOfNodesTouchedAbove + numberOfNodesTouchedBelow;
        this.nodesConfirmed = nodesConfirmed;
        this.nodesWithBoundChanges = nodesWithBoundChanges;
    }

    public PermeationStats(final PermeationStats aboveStats, final PermeationStats belowStats) {
        this.numberOfConfirmations = aboveStats.numberOfConfirmations + belowStats.numberOfConfirmations;
        this.numberOfNodesTouchedAbove = aboveStats.numberOfNodesTouchedAbove;
        this.numberOfNodesTouchedBelow = belowStats.numberOfNodesTouchedBelow;
        this.totalNumberOfNodesTouched = aboveStats.totalNumberOfNodesTouched + belowStats.totalNumberOfNodesTouched;
        this.nodesConfirmed = RoaringBitmap.or(aboveStats.nodesConfirmed, belowStats.nodesConfirmed);
        this.nodesWithBoundChanges = RoaringBitmap.or(aboveStats.nodesWithBoundChanges, belowStats.nodesWithBoundChanges);
    }

    public String toString(){
        return "\tPermeationStats {" +
                "\n\t\tnumberOfConfirmations:\t\t" + numberOfConfirmations +
                "\n\t\ttotalNumberOfNodesTouched:\t" + totalNumberOfNodesTouched +
                "\n\t\tnumberOfNodesTouchedAbove:\t" + numberOfNodesTouchedAbove +
                "\n\t\tnumberOfNodesTouchedBelow:\t" + numberOfNodesTouchedBelow +
                "\n\t}";
    }

    public enum PermeationStatistic {
        TOTAL_NUMBER_OF_NODES_TOUCHED,
        NUMBER_OF_NODES_TOUCHED_ABOVE,
        NUMBER_OF_NODES_TOUCHED_BELOW,
        NUMBER_OF_CONFIRMATIONS,
    }

}

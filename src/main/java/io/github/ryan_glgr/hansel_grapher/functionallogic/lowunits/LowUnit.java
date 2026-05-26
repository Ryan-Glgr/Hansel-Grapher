package io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import static io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit.Type.EXCLUSIVE;

@AllArgsConstructor
@Getter
public class LowUnit {

    public enum Type {
        INCLUSIVE, // your regular lowUnit. A Node which tells us "any node >= in all attributes is this classification"
        EXCLUSIVE, // this kind of low unit says "any node >= in all attributes, AND NOT THIS EXACT NODE is this classification"
        ;
    }

    @NonNull
    private final Node datapoint;

    @NonNull
    private final Type lowUnitType;

    @NonNull
    private final Integer classification;

    public boolean classifies(@NonNull final Node nodeToClassify) {
        // an exclusive node classifies if the node to classify is >= in all attributes, AND it's not the exact same
        // point. exact same point would have hamming distance 0
        if (EXCLUSIVE.equals(this.lowUnitType)) {
            return datapoint.isDominatedBy(nodeToClassify, true) && datapoint.computeHammingDistance(nodeToClassify) != 0;
        }
        // an inclusive node simply classifies if the node to classify is >= in all attributes
        return datapoint.isDominatedBy(nodeToClassify, true);
    }
}

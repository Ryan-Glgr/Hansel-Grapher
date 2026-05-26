package io.github.ryan_glgr.hansel_grapher.functionallogic.Interview;

import io.github.ryan_glgr.hansel_grapher.functionrules.Attribute;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.PythonInterpreter;

import java.util.*;

import static java.lang.Math.min;

public class QuestionHelper {

    public static int linearFunctionQuestion(final Node datapoint, final Attribute[] attributes, final int numClasses) {
        int sum = 0;
        for (int i = 0; i < datapoint.values.length; i++) {
            sum += (int) (datapoint.values[i] * attributes[i].weight);
        }
        return min((sum / datapoint.values.length), (numClasses - 1));
    }

    public static int knownLowUnitsQuestion(final Node datapoint, final Map<Integer, Set<Node>> lowUnitsForEachClassification) {
        return lowUnitsForEachClassification.entrySet().stream()
                .filter(entry -> entry.getKey() > 0)
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(lowUnit -> lowUnit.isDominatedBy(datapoint, true)))
                .map(Map.Entry::getKey)
                .max(Integer::compareTo)
                .orElse(0);
    }

    public static int questionExpert(final Node datapoint, final Scanner inputScanner) {
        System.out.println("WHAT IS THE CLASSIFICATION FOR THIS DATAPOINT?");
        System.out.println(Arrays.toString(datapoint.values));
        System.out.println("\tCURRENT MINIMUM:\t" + datapoint.classification);
        System.out.println("\tCURRENT MAXIMUM:\t" + datapoint.maxPossibleValue);
        System.out.println("INPUT:\t");
        final int expertInput = inputScanner.nextInt();
        if (expertInput < datapoint.classification || expertInput > datapoint.maxPossibleValue) {
            System.out.println("MONOTONICITY VIOLATION!");
            throw new RuntimeException("MONOTONICITY RUINED!");
        }
        return expertInput;
    }

    public static int queryPython(final Node nodeToQuery, final PythonInterpreter pythonInterpreter) {
        return pythonInterpreter.predict(List.of(nodeToQuery)).getFirst();
    }

    public static List<Integer> queryPythonBatch(final List<Node> nodesToQuery, final PythonInterpreter pythonInterpreter) {
        return pythonInterpreter.predict(nodesToQuery);
    }


}

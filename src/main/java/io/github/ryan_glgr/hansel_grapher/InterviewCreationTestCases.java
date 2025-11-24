package io.github.ryan_glgr.hansel_grapher;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.InterviewHelperFunctions;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.MagicFunctionMode;

import java.util.stream.IntStream;

public class InterviewCreationTestCases {
    
    public static Interview createBasicInterviewWithSubfunctions(final InterviewMode interviewMode,
                                                                 final Integer[] kValues,
                                                                 final Float[] weights) {
        
        int maxSum = IntStream.range(0, kValues.length)
            .map(i -> (int) ((kValues[i] - 1) * weights[i]))
            .sum();
        int numClasses = maxSum / kValues.length + 1;

        String[] attributeNames = InterviewHelperFunctions.createDefaultAttributeNames(kValues.length);
        String[] classificationNames = InterviewHelperFunctions.createDefaultClassificationNames(numClasses);

        Interview child0 = InterviewHelperFunctions.createSubFunction(
                new Integer[] {3, 5, 4, 3},
                new Float[] {.5f, 1.65f, 1.25f, 1.0f},
                5,
                interviewMode,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE
        );

        Interview child1 = InterviewHelperFunctions.createSubFunction(
                new Integer[] {3, 4, 6, 7},
                new Float[] {.5f, 2.25f, 1.85f, 1.0f},
                5,
                interviewMode,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE
        );

        Interview child2 = InterviewHelperFunctions.createSubFunction(
                new Integer[] {2, 2, 7, 5},
                new Float[] {.5f, 2.25f, 1.25f, 1.0f},
                3,
                interviewMode,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE
        );

        Interview[] childFunctions = new Interview[]{
            child0,
            child1,
            child2,
            null,
            null,
            null
        };

        assert(childFunctions.length == kValues.length);

        Interview interview = new Interview(kValues,
                weights,
                interviewMode,
                numClasses,
                attributeNames,
                classificationNames,
                null,
                childFunctions,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE);

        System.out.println(interviewMode + " INTERVIEW COMPLETE!");
//        System.out.println(interview);
        System.out.println("number of questions asked: " + interview.interviewStats.nodesAsked.size() + "\n");
        System.out.println("number of nodes total: " + interview.interviewStats.numberOfNodes);
        return interview;
    }
}

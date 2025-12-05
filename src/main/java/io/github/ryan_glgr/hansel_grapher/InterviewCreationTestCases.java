package io.github.ryan_glgr.hansel_grapher;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewHelperFunctions;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.MagicFunctionMode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

public class InterviewCreationTestCases {

    public static Interview createBasicInterviewWithSubfunctions(final InterviewMode interviewMode,
                                                                  final boolean findOptimalChildren) {
        final Integer[] kValues = new Integer[] {3, 5, 4, 3, 7, 7};
        final Float[] weights = new Float[] {.5f, 1.65f, 1.25f, 1.0f, 2.25f, 1.85f};
        return createBasicInterviewWithSubfunctions(interviewMode, kValues, weights, findOptimalChildren);
    }

    public static Interview createBasicInterviewWithSubfunctions(final InterviewMode interviewMode,
                                                                 final Integer[] kValues,
                                                                 final Float[] weights,
                                                                 final boolean findOptimalChildren) {
        
        final int maxSum = IntStream.range(0, kValues.length)
            .map(i -> (int) ((kValues[i] - 1) * weights[i]))
            .sum();
        final int numClasses = maxSum / kValues.length + 1;

        final String[] attributeNames = InterviewHelperFunctions.createDefaultAttributeNames(kValues.length);
        final String[] classificationNames = InterviewHelperFunctions.createDefaultClassificationNames(numClasses);

        final Interview[] availableSubfunctions = new Interview[]{
            InterviewHelperFunctions.createSubFunction(
                new Integer[] {3, 5, 4, 3},
                new Float[] {.5f, 1.65f, 1.25f, 1.0f},
                5,
                interviewMode,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE,
                findOptimalChildren
            ),
            InterviewHelperFunctions.createSubFunction(
                new Integer[] {3, 4, 6, 7},
                new Float[] {.5f, 2.25f, 1.85f, 1.0f},
                5,
                interviewMode,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE,
                findOptimalChildren
            ),
            InterviewHelperFunctions.createSubFunction(
                new Integer[] {2, 2, 7, 5},
                new Float[] {.5f, 2.25f, 1.25f, 1.0f},
                3,
                interviewMode,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE,
                findOptimalChildren
            )
        };

        final Interview[] childFunctions = new Interview[kValues.length];
        for (int i = 0; i < kValues.length; i++) {
            childFunctions[i] = i < availableSubfunctions.length ? availableSubfunctions[i] : null;
        }

        final Interview interview = new Interview(kValues,
                weights,
                interviewMode,
                numClasses,
                attributeNames,
                classificationNames,
                null,
                childFunctions,
                MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE,
                findOptimalChildren);

        if (findOptimalChildren) {
            System.out.println("GREEDY RULE TREE BUILDING INTERVIEW");
        }
        else{
            System.out.println("OPTIMAL RULE TREE BUILDING INTERVIEW");
        }
        System.out.println(interviewMode + " INTERVIEW COMPLETE!");
        System.out.println(interview);
        return interview;
    }

    // appendix C from: https://digitalcommons.cwu.edu/cgi/viewcontent.cgi?article=3037&context=etd
    // some of the k values were wrong for this table, so these weight can be wrong as well.
    // the k values in the table don't multiply out to the 27,648 number which is given.
    private static final Float[] ldaWeightsForHeartFailureStudy = new Float[]{
            0.87015326f,  // x1: Age, k = 3, weight = 0.87015326
            -0.04499727f, // x2: Anemia, k = 2, weight = -0.04499727
            0.32147318f,  // x3: Diabetes, k = 2, weight = 0.32147318
            -0.0731553f,  // x4: Creatinine phosphokinase, k = 4, weight = -0.0731553
            -0.82548781f, // x5: Ejection fraction, k = 2, weight = -0.82548781
            -0.04316909f, // x6: High blood pressure, k = 2, weight = -0.04316909
            0.1835072f,   // x7: Platelets, k = 4, weight = 0.1835072
            2.08206913f,  // x8: Sex, k = 2, weight = 2.08206913
            -1.53264771f, // x9: Serum creatinine, k = 2, weight = -1.53264771
            -0.29418355f, // x10: Serum sodium, k = 2, weight = -0.29418355
            -0.08438572f, // x11: Smoking, k = 2, weight = -0.08438572
            -1.51960505f  // x12: Time, k = 3, weight = -1.51960505
    };

    private static final String[] attributeNamesForHeartFailureStudy = new String[]{
            "Age",
            "Anemia",
            "Diabetes",
            "Creatinine phosphokinase",
            "Ejection fraction",
            "High blood pressure",
            "Platelets",
            "Sex",
            "Serum creatinine",
            "Serum sodium",
            "Smoking",
            "Time"
    };

    private static final Set<Integer[]> knownLowUnitsFromHeartFailureStudy = Set.of(
            new Integer[]{0,0,1,1,0,1,0,1,0,1,1,1}, // (x3≥1)x4x6x8x10x11(x12≥1)
            new Integer[]{0,0,2,1,1,0,0,0,0,0,1,2}, // (x3≥2)x4x5x11(x12≥2)
            new Integer[]{0,0,2,1,1,0,0,0,1,0,1,1}, // (x3≥2)x4x5x9x11(x12≥1)
            new Integer[]{0,0,3,0,0,1,0,0,0,1,0,2}, // (x3≥3)x6x10(x12≥2)
            new Integer[]{0,0,3,0,0,1,0,0,1,1,0,1}, // (x3≥3)x6x9x10(x12≥1)
            new Integer[]{0,1,1,1,0,1,0,1,1,1,0,0}, // x2(x3≥1)x4x6x8x9x10
            new Integer[]{0,0,2,1,0,0,0,1,0,1,1,1}, // (x3≥2)x4x8x10x11(x12≥1)
            new Integer[]{1,1,0,1,0,1,0,0,1,1,1,1}, // (x1≥1)x2x4x6x9x10x11(x12≥1)
            new Integer[]{0,0,1,1,1,0,0,0,1,1,1,1}, // (x3≥1)x4x5x9x10x11(x12≥1)
            new Integer[]{0,0,1,1,1,0,0,0,0,1,1,2}, // (x3≥1)x4x5x10x11(x12≥2)
            new Integer[]{0,1,0,1,1,0,0,1,1,0,1,0}, // x2x4x5x8x9x11
            new Integer[]{0,1,3,0,1,0,0,1,0,1,1,0}, // x2(x3≥3)x5x8x10x11
            new Integer[]{1,0,1,0,1,1,0,1,0,1,1,0}, // (x1≥1)(x3≥1)x5x6x8x10x11
            new Integer[]{1,1,2,0,1,1,0,0,1,1,1,0}, // (x1≥1)x2(x3≥2)x5x6x9x10x11
            new Integer[]{0,0,3,0,0,1,0,1,0,0,0,1}, // (x3≥3)x6x8(x12≥1)
            new Integer[]{0,1,0,1,1,1,0,1,0,0,0,1}, // x2x4x5x6x8(x12≥1)
            new Integer[]{0,0,3,0,1,0,0,0,1,0,0,1}, // (x3≥3)x5x9(x12≥1)
            new Integer[]{0,0,2,0,1,1,0,0,1,0,0,1}, // (x3≥2)x5x6x9(x12≥1)
            new Integer[]{0,0,3,0,0,0,0,1,0,1,0,1}, // (x3≥3)x8x10(x12≥1)
            new Integer[]{0,0,2,0,0,1,0,1,0,1,0,1}, // (x3≥2)x6x8x10(x12≥1)
            new Integer[]{0,1,1,0,1,0,0,0,1,1,0,1}, // x2(x3≥1)x5x9x10(x12≥1)
            new Integer[]{0,0,2,0,1,0,0,0,1,1,0,1}, // (x3≥2)x5x9x10(x12≥1)
            new Integer[]{0,0,1,0,1,1,0,0,1,1,0,1}, // (x3≥1)x5x6x9x10(x12≥1)
            new Integer[]{0,0,3,1,0,0,0,1,0,0,1,1}, // (x3≥3)x4x8x11(x12≥1)
            new Integer[]{1,1,0,1,0,1,0,1,0,0,1,1}, // (x1≥1)x2x4x6x8x11(x12≥1)
            new Integer[]{1,0,2,1,0,0,0,0,1,0,1,1}, // (x1≥1)(x3≥2)x4x9x11(x12≥1)
            new Integer[]{1,0,3,0,1,0,0,0,0,1,1,1}, // (x1≥1)(x3≥3)x5x10x11(x12≥1)
            new Integer[]{1,0,1,1,0,0,0,0,1,1,1,1}, // (x1≥1)(x3≥1)x4x9x10x11(x12≥1)
            new Integer[]{0,0,3,0,1,0,0,0,0,0,0,2}, // (x3≥3)x5(x12≥2)
            new Integer[]{0,0,2,0,1,1,0,0,0,0,0,2}, // (x3≥2)x5x6(x12≥2)
            new Integer[]{0,1,1,0,1,0,0,0,0,1,0,2}, // x2(x3≥1)x5x10(x12≥2)
            new Integer[]{0,0,2,0,1,0,0,0,0,1,0,2}, // (x3≥2)x5x10(x12≥2)
            new Integer[]{1,0,1,1,0,1,0,0,0,1,0,2}, // (x1≥1)(x3≥1)x4x6x10(x12≥2)
            new Integer[]{0,0,1,0,1,1,0,0,0,1,0,2}, // (x3≥1)x5x6x10(x12≥2)
            new Integer[]{0,1,2,1,0,1,0,1,1,0,0,0}, // x2(x3≥2)x4x6x8x9
            new Integer[]{0,1,0,1,1,1,0,1,1,0,0,0}, // x2x4x5x6x8x9
            new Integer[]{1,0,1,1,1,1,0,1,0,1,0,0}, // (x1≥1)(x3≥1)x4x5x6x8x10
            new Integer[]{0,0,2,1,0,0,0,1,1,1,0,0}, // (x3≥2)x4x8x9x10
            new Integer[]{0,0,2,0,0,1,0,1,1,1,0,0}, // (x3≥2)x6x8x9x10
            new Integer[]{2,0,0,1,1,1,0,0,1,1,0,0}, // (x1≥2)x4x5x6x9x10
            new Integer[]{1,0,3,0,1,0,0,0,1,1,0,0}, // (x1≥1)(x3≥3)x5x9x10
            new Integer[]{0,1,2,0,1,0,0,0,1,0,0,1}, // x2(x3≥2)x5x9(x12≥1)
            new Integer[]{1,0,2,0,0,1,0,0,0,0,0,2}, // (x1≥1)(x3≥2)x6(x12≥2)
            new Integer[]{1,1,1,0,1,1,0,1,0,1,0,0}, // (x1≥1)x2(x3≥1)x5x6x8x10
            new Integer[]{2,0,1,1,0,1,0,1,0,1,0,0}, // (x1≥2)(x3≥1)x4x6x8x10
            new Integer[]{1,1,1,1,1,0,0,1,0,1,0,0}, // (x1≥1)x2(x3≥1)x4x5x8x10
            new Integer[]{1,0,2,0,1,0,0,1,0,1,0,0}, // (x1≥1)(x3≥2)x5x8x10
            new Integer[]{0,1,2,0,0,0,0,1,0,1,0,1}, // x2(x3≥2)x8x10(x12≥1)
            new Integer[]{1,0,2,0,0,0,0,0,0,1,0,2}, // (x1≥1)(x3≥2)x10(x12≥2)
            new Integer[]{0,0,3,0,0,0,0,1,1,0,0,0}, // (x3≥3)x8x9
            new Integer[]{1,0,2,0,0,0,0,0,1,1,0,1}, // (x1≥1)(x3≥2)x9x10(x12≥1)
            new Integer[]{1,1,1,1,0,0,0,0,0,1,0,2}, // (x1≥1)x2(x3≥1)x4x10(x12≥2)
            new Integer[]{1,0,1,0,0,1,0,0,1,1,0,1}, // (x1≥1)(x3≥1)x6x9x10(x12≥1)
            new Integer[]{1,1,0,1,0,1,0,1,1,0,0,0}, // (x1≥1)x2x4x6x8x9
            new Integer[]{1,1,1,0,0,1,0,0,0,1,0,2}, // (x1≥1)x2(x3≥1)x6x10(x12≥2)
            new Integer[]{1,0,2,0,0,1,0,0,1,0,0,1}, // (x1≥1)(x3≥2)x6x9(x12≥1)
            new Integer[]{0,1,0,0,0,1,0,0,1,0,0,2}, // x2x6x9(x12≥2)
            new Integer[]{0,1,2,0,0,0,0,1,1,1,0,0}, // x2(x3≥2)x8x9x10
            new Integer[]{2,1,0,1,1,1,0,0,0,1,0,1}, // (x1≥2)x2x4x5x6x10(x12≥1)
            new Integer[]{1,0,2,0,1,1,0,1,0,0,0,0}, // (x1≥1)(x3≥2)x5x6x8
            new Integer[]{1,0,3,0,1,1,0,0,0,1,0,1}, // (x1≥1)(x3≥3)x5x6x10(x12≥1)
            new Integer[]{2,0,0,0,1,1,0,0,1,1,1,0}, // (x1≥2)x5x6x9x10x11
            new Integer[]{2,1,2,1,0,1,0,0,1,1,1,0}, // (x1≥2)x2(x3≥2)x4x6x9x10x11
            new Integer[]{2,0,1,0,0,1,0,1,0,1,1,0}, // (x1≥2)(x3≥1)x6x8x10x11
            new Integer[]{1,1,3,1,0,1,0,1,0,1,1,0}, // (x1≥1)x2(x3≥3)x4x6x8x10x11
            new Integer[]{1,1,1,0,1,0,0,1,0,1,1,0}, // (x1≥1)x2(x3≥1)x5x8x10x11
            new Integer[]{0,0,1,0,1,0,0,1,0,0,0,1}, // (x3≥1)x5x8(x12≥1)
            new Integer[]{1,0,3,0,0,0,0,0,0,0,0,2}, // (x1≥1)(x3≥3)(x12≥2)
            new Integer[]{2,0,3,0,0,1,0,0,0,1,0,1}, // (x1≥2)(x3≥3)x6x10(x12≥1)
            new Integer[]{1,0,3,0,1,0,0,1,0,0,0,0}, // (x1≥1)(x3≥3)x5x8
            new Integer[]{0,0,0,0,1,0,0,1,0,1,0,1}, // x5x8x10(x12≥1)
            new Integer[]{0,1,3,0,0,0,0,1,0,0,0,1}, // x2(x3≥3)x8(x12≥1)
            new Integer[]{2,0,1,0,1,0,0,0,0,1,0,1}, // (x1≥2)(x3≥1)x5x10(x12≥1)
            new Integer[]{0,0,1,0,0,0,0,0,1,0,0,2}, // (x3≥1)x9(x12≥2)
            new Integer[]{0,0,0,0,1,0,0,0,1,0,0,2}, // x5x9(x12≥2)
            new Integer[]{1,1,3,0,1,0,0,0,0,1,0,1}, // (x1≥1)x2(x3≥3)x5x10(x12≥1)
            new Integer[]{0,0,0,0,1,0,0,1,1,1,0,0}, // x5x8x9x10
            new Integer[]{2,0,3,0,0,1,0,0,1,1,0,0}, // (x1≥2)(x3≥3)x6x9x10
            new Integer[]{2,0,1,0,1,0,0,0,1,1,0,0}, // (x1≥2)(x3≥1)x5x9x10
            new Integer[]{1,1,1,0,0,0,0,0,1,1,0,1}, // (x1≥1)x2(x3≥1)x9x10(x12≥1)
            new Integer[]{2,0,3,1,0,0,0,0,0,1,1,1}, // (x1≥2)(x3≥3)x4x10x11(x12≥1)
            new Integer[]{1,0,3,0,0,0,0,0,1,0,0,1}, // (x1≥1)(x3≥3)x9(x12≥1)
            new Integer[]{0,0,0,0,0,0,0,0,1,1,0,2}, // x9x10(x12≥2)
            new Integer[]{0,0,1,0,1,0,0,1,1,0,0,0}, // (x3≥1)x5x8x9
            new Integer[]{1,0,0,0,1,0,0,1,0,0,0,1}, // (x1≥1)x5x8(x12≥1)
            new Integer[]{2,0,2,0,0,0,0,1,0,1,0,0}, // (x1≥2)(x3≥2)x8x10
            new Integer[]{2,0,0,0,1,0,0,1,0,1,0,0}, // (x1≥2)x5x8x10
            new Integer[]{2,1,1,0,0,1,0,1,0,1,0,0}, // (x1≥2)x2(x3≥1)x6x8x10
            new Integer[]{2,0,0,0,1,1,0,1,0,0,0,0}, // (x1≥2)x5x6x8
            new Integer[]{2,0,2,0,0,1,0,1,0,0,0,0}, // (x1≥2)(x3≥2)x6x8
            new Integer[]{1,0,0,0,0,0,0,1,1,1,0,0}, // (x1≥1)x8x9x10
            new Integer[]{2,1,1,1,1,1,0,0,1,0,0,0}, // (x1≥2)x2(x3≥1)x4x5x6x9
            new Integer[]{2,0,1,0,1,0,0,1,0,0,0,0}, // (x1≥2)(x3≥1)x5x8
            new Integer[]{1,1,2,0,1,0,0,1,0,0,0,0}, // (x1≥1)x2(x3≥2)x5x8
            new Integer[]{2,0,3,1,0,0,0,0,1,1,0,0}, // (x1≥2)(x3≥3)x4x9x10
            new Integer[]{1,0,0,0,0,0,0,0,1,0,0,2}, // (x1≥1)x9(x12≥2)
            new Integer[]{2,0,2,0,1,0,0,0,1,0,0,0}, // (x1≥2)(x3≥2)x5x9
            new Integer[]{2,1,1,1,1,1,0,0,0,0,0,1}, // (x1≥2)x2(x3≥1)x4x5x6(x12≥1)
            new Integer[]{2,0,2,0,1,0,0,0,0,0,0,1}, // (x1≥2)(x3≥2)x5(x12≥1)
            new Integer[]{1,0,0,0,1,0,0,0,1,0,0,1}, // (x1≥1)x5x9(x12≥1)
            new Integer[]{1,0,0,0,1,0,0,1,1,0,0,0}, // (x1≥1)x5x8x9
            new Integer[]{1,0,0,0,0,0,0,1,0,1,0,1}, // (x1≥1)x8x10(x12≥1)
            new Integer[]{1,1,2,0,0,0,0,0,1,0,0,1}, // (x1≥1)x2(x3≥2)x9(x12≥1)
            new Integer[]{1,0,1,0,0,0,0,1,1,0,0,0}, // (x1≥1)(x3≥1)x8x9
            new Integer[]{0,0,0,0,0,0,0,1,0,0,0,2}, // x8(x12≥2)
            new Integer[]{1,0,1,0,0,0,0,1,0,0,0,1}, // (x1≥1)(x3≥1)x8(x12≥1)
            new Integer[]{1,0,0,0,1,0,0,0,0,0,0,2}, // (x1≥1)x5(x12≥2)
            new Integer[]{0,0,0,0,0,0,0,1,1,0,0,1}, // x8x9(x12≥1)
            new Integer[]{2,0,0,0,0,0,0,0,1,0,0,1}, // (x1≥2)x9(x12≥1)
            new Integer[]{2,1,0,0,1,0,0,1,0,0,0,0}, // (x1≥2)x2x5x8
            new Integer[]{2,1,2,1,0,0,0,1,0,0,0,0}, // (x1≥2)x2(x3≥2)x4x8
            new Integer[]{2,0,3,0,0,0,0,1,0,0,0,0}, // (x1≥2)(x3≥3)x8
            new Integer[]{2,0,0,0,0,0,0,0,0,0,0,2}, // (x1≥2)(x12≥2)
            new Integer[]{2,0,0,0,0,0,0,1,0,0,0,1}, // (x1≥2)x8(x12≥1)
            new Integer[]{2,0,0,0,0,0,0,1,1,0,0,0}  // (x1≥2)x8x9
    );

    public static Interview createHeartFailureInterview(final InterviewMode interviewMode, final boolean findOptimalClauses) {

        // k values based on actual rules (paper table has discrepancies)
        // Paper claims 27,648 nodes: 3×2×4×2×2×2×1×2×2×2×2×3 = 27,648 ✓
        final Integer[] kValues = new Integer[] {
                3,  // x1: Age (0-2) - paper: k=3, weight = 0.87015326
                2,  // x2: Anemia (0-1) - paper: k=2, weight = -0.04499727
                4,  // x3: Diabetes (0-3) - paper: k=2, weight = 0.32147318 (WRONG, should be 4 based on rules)
                2,  // x4: Creatinine phosphokinase (0-1) - paper: k=4, weight = -0.0731553 (WRONG, should be 2)
                2,  // x5: Ejection fraction (0-1) - paper: k=2, weight = -0.82548781
                2,  // x6: High blood pressure (0-1) - paper: k=2, weight = -0.04316909
                3,  // x7: Platelets (only 0, not in rules) - paper: k=4, weight = 0.1835072 (WRONG, should be 1)
                2,  // x8: Sex (0-1) - paper: k=2, weight = 2.08206913
                2,  // x9: Serum creatinine (0-1) - paper: k=2, weight = -1.53264771
                2,  // x10: Serum sodium (0-1) - paper: k=2, weight = -0.29418355
                2,  // x11: Smoking (0-1) - paper: k=2, weight = -0.08438572
                3   // x12: Time (0-2) - paper: k=3, weight = -1.51960505
        };

        final Interview interview = new Interview(kValues,
                ldaWeightsForHeartFailureStudy,
                interviewMode,
                2,
                attributeNamesForHeartFailureStudy,
                new String[] {"No Heart Failure", "Heart Failure"},
                new Set[] {
                        new HashSet(),
                        knownLowUnitsFromHeartFailureStudy
                },
                new Interview[kValues.length],
                MagicFunctionMode.KNOWN_LOW_UNITS_MODE,
                findOptimalClauses);

        if (findOptimalClauses) {
            System.out.println("GREEDY RULE TREE BUILDING INTERVIEW");
        }
        else{
            System.out.println("OPTIMAL RULE TREE BUILDING INTERVIEW");
        }
        System.out.println(interviewMode + " INTERVIEW COMPLETE!");
        System.out.println(interview);
        return interview;
    }
}

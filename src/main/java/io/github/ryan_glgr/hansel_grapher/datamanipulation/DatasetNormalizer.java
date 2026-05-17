package io.github.ryan_glgr.hansel_grapher.datamanipulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class DatasetNormalizer {

    // ---------------------------------------------------------------------------
    // Normalization mode — add cases here as new techniques are implemented
    // ---------------------------------------------------------------------------

    public enum NormalizationMode {
        EQUAL_WIDTH,                     // fixed bin width across [min, max]
        EQUAL_FREQUENCY,                 // bins contain equal number of datapoints
        FREEDMAN_DIACONIS,               // IQR-based adaptive bin width per attribute
        UNIQUE_INTEGERS,                 // k = distinct integer values, full resolution
        UNIQUE_INTEGERS_SOME_RESOLUTION  // k = distinct integer values / resolution
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    public static NormalizedDataset loadOrCreateNormalizedDataset(
            final String datasetPath,
            final NormalizationMode normalizationMode,
            final float resolution) throws IOException {

        final String datasetName = stripExtension(new File(datasetPath).getName());
        final String normalizedName = datasetName + "_" + normalizationMode.name();
        final String normalizedPath = NormalizedDataset.OUTPUT_DIRECTORY
                + File.separator + normalizedName + ".json";

        final File normalizedFile = new File(normalizedPath);
        if (normalizedFile.exists()) {
            System.out.println("[normalizer] Loading existing: " + normalizedPath);
            return NormalizedDataset.deserializeFromJsonFile(normalizedPath);
        }

        System.out.println("[normalizer] Creating: " + normalizedPath);
        final NormalizedDataset created = createNormalizedDataset(datasetPath, normalizedName, normalizationMode, resolution);

        System.out.println("Serializing normalized dataset");
        created.serializeToJsonFile();
        return created;
    }

    // ---------------------------------------------------------------------------
    // Creation
    // ---------------------------------------------------------------------------

    private static NormalizedDataset createNormalizedDataset(
            final String datasetPath,
            final String normalizedName,
            final NormalizationMode mode,
            final float resolution) throws FileNotFoundException {

        try (final Scanner scanner = new Scanner(new File(datasetPath))) {

            // --- Header: last column is the target ---
            final String[] headers = scanner.nextLine().split(",");
            final int numFeatures = headers.length - 1;
            final String[] attributeNames = Arrays.copyOf(headers, numFeatures);

            // --- Read all raw rows ---
            final List<String[]> rawRows = new ArrayList<>();
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine().trim();
                if (!line.isEmpty()) rawRows.add(line.split(","));
            }

            final int numRows = rawRows.size();

            // --- Parse feature matrix and targets ---
            final double[][] featureMatrix = new double[numRows][numFeatures];
            final int[] targets = new int[numRows];

            for (int row = 0; row < numRows; row++) {
                final String[] tokens = rawRows.get(row);
                for (int col = 0; col < numFeatures; col++) {
                    try {
                        featureMatrix[row][col] = Double.parseDouble(tokens[col].trim());
                    } catch (final NumberFormatException e) {
                        System.out.println("[normalizer] Non-numeric feature value '" + tokens[col]
                                + "' at row " + row + ", col " + col
                                + ". Preprocess string values into integers before normalizing.");
                        throw new RuntimeException(e);
                    }
                }
                targets[row] = Integer.parseInt(tokens[numFeatures].trim());
            }

            // --- Validate targets ---
            assertValidClassifications(targets);

            final int numClasses = Arrays.stream(targets).max().orElseThrow(() -> new IllegalStateException("Not able to determine number of classes")) + 1;

            // --- Normalize each column in place, collecting k values as we go ---
            // Each normalizeColumn call does two passes over its column and returns k.
            // featureMatrix is mutated in place — rows become normalized bin indices.
            final Integer[] kValues = new Integer[numFeatures];
            for (int col = 0; col < numFeatures; col++) {
                kValues[col] = normalizeColumn(featureMatrix, col, mode, resolution);
            }

            // --- Read normalized matrix into datapoints, appending target ---
            final List<Integer[]> allDatapoints = buildDatapoints(featureMatrix, targets, numFeatures);

            return new NormalizedDataset(normalizedName, kValues, attributeNames,numClasses, allDatapoints);
        }
    }

    // ---------------------------------------------------------------------------
    // Column normalization — one method per strategy
    // Each mutates featureMatrix[:][col] in place and returns the k value.
    // ---------------------------------------------------------------------------

    private static int normalizeColumn(
            final double[][] featureMatrix,
            final int col,
            final NormalizationMode mode,
            final float resolution) {
        return switch (mode) {
            case EQUAL_WIDTH -> normalizeColumnEqualWidth(featureMatrix, col);
            case EQUAL_FREQUENCY -> normalizeColumnEqualFrequency(featureMatrix, col);
            case FREEDMAN_DIACONIS -> normalizeColumnFreedmanDiaconis(featureMatrix, col);
            case UNIQUE_INTEGERS -> normalizeColumnUniqueIntegers(featureMatrix, col, 1.0f);
            case UNIQUE_INTEGERS_SOME_RESOLUTION -> normalizeColumnUniqueIntegers(featureMatrix, col, resolution);
        };
    }

    private static int normalizeColumnEqualWidth(final double[][] featureMatrix, final int col) {
        throw new UnsupportedOperationException("EQUAL_WIDTH not yet implemented.");
    }

    private static int normalizeColumnEqualFrequency(final double[][] featureMatrix, final int col) {
        throw new UnsupportedOperationException("EQUAL_FREQUENCY not yet implemented.");
    }

    private static int normalizeColumnFreedmanDiaconis(final double[][] featureMatrix, final int col) {
        throw new UnsupportedOperationException("FREEDMAN_DIACONIS not yet implemented.");
    }

    private static int normalizeColumnUniqueIntegers(
            final double[][] featureMatrix,
            final int col,
            final float resolution) {

        // Pass 1: find min and number of unique values
        double min = Double.MAX_VALUE;
        final HashSet<Integer> unique = new HashSet<>();
        for (final double[] row : featureMatrix) {
            final int val = (int) row[col];
            unique.add(val);
            min = Math.min(min, val);
        }

        // bucketSize > 1 collapses values together; resolution=1.0 → bucketSize=1 (no collapse)
        final int k          = Math.max(2, (int) Math.ceil(unique.size() * resolution));
        final int bucketSize = Math.max(1, (int) Math.ceil((double) unique.size() / k));

        // Pass 2: shift to 0-based, divide into bucket, clamp to [0, k-1]
        for (final double[] row : featureMatrix) {
            final int rank = (int) row[col] - (int) min;
            row[col] = Math.min(rank / bucketSize, k - 1);
        }

        return k;
    }

    // ---------------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------------

    /**
     * Read the already-normalized featureMatrix into Integer[] datapoints,
     * appending the target class index as the final element of each row.
     */
    private static List<Integer[]> buildDatapoints(
            final double[][] featureMatrix,
            final int[] targets,
            final int numFeatures) {

        final List<Integer[]> datapoints = new ArrayList<>(featureMatrix.length);

        for (int row = 0; row < featureMatrix.length; row++) {
            final Integer[] point = new Integer[numFeatures + 1];
            for (int col = 0; col < numFeatures; col++) {
                point[col] = (int) featureMatrix[row][col];
            }
            point[numFeatures] = targets[row];
            datapoints.add(point);
        }

        return datapoints;
    }

    private static void assertValidClassifications(final int[] targets) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        final HashSet<Integer> found = new HashSet<>();

        for (int i = 0; i < targets.length; i++) {
            final int val = targets[i];
            if (val < 0) throw new IllegalArgumentException(
                    "[normalizer] Negative class index " + val + " at row " + i + ".");
            found.add(val);
            min = Math.min(min, val);
            max = Math.max(max, val);
        }

        if (min != 0) throw new IllegalArgumentException(
                "[normalizer] Lowest class index is " + min + ", expected 0.");

        if (max != found.size() - 1) throw new IllegalArgumentException(
                "[normalizer] Class indices have gaps. Found " + found.size()
                        + " distinct classes but max index is " + max
                        + ". Expected {0.." + (found.size() - 1) + "}.");
    }

    private static String stripExtension(final String filename) {
        final int dot = filename.lastIndexOf('.');
        return dot == -1 ? filename : filename.substring(0, dot);
    }
}
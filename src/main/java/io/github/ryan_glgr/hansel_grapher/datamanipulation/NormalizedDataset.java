package io.github.ryan_glgr.hansel_grapher.datamanipulation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class NormalizedDataset {

    public static final String OUTPUT_DIRECTORY = String.join(
            File.separator, "src", "main", "resources", "data", "normalizeddatasets");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String               inputDatasetName;              // used for model filename convention
    private final Integer[]            kValues;                       // distinct integer values per attribute
    private final String[]             attributeNames;                // one per attribute, same length as kValues
    private Integer                    numClasses;
    private List<Integer[]>            allDatapoints;                 // each row: [...features, classIndex]

    /**
     * Serializes this dataset to a JSON file in OUTPUT_DIRECTORY.
     *
     * @return the absolute path of the written file.
     * @throws IOException if the file cannot be written.
     */
    public String serializeToJsonFile() throws IOException {
        final File outputDir = new File(OUTPUT_DIRECTORY);
        if (!outputDir.exists()) outputDir.mkdirs();

        final File outputFile = new File(outputDir, inputDatasetName + ".json");

        try (final FileWriter writer = new FileWriter(outputFile)) {
            GSON.toJson(this, writer);
        }

        return outputFile.getAbsolutePath();
    }

    /**
     * Deserializes a NormalizedDataset from a JSON file.
     *
     * @param jsonFilePath absolute or relative path to the .json file.
     * @return the deserialized NormalizedDataset.
     * @throws IOException if the file cannot be read.
     */
    public static NormalizedDataset deserializeFromJsonFile(final String jsonFilePath) throws IOException {
        try (final java.io.FileReader reader = new java.io.FileReader(jsonFilePath)) {
            return GSON.fromJson(reader, NormalizedDataset.class);
        }
    }
}
package io.github.ryan_glgr.hansel_grapher.thehardstuff;

import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.MLModel;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PythonInterpreter implements AutoCloseable {

    private static final String PYTHON_SCRIPT_PATH = String.join(
            File.separator, "src", "main", "resources", "python", "interview.py");
    private static final String PYTHON_COMMAND = "python3";

    private static final String VALUES_KEY = "values";

    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;

    public PythonInterpreter(final MLModel model, final String datasetPath) throws IOException {
        this.process = new ProcessBuilder(PYTHON_COMMAND, PYTHON_SCRIPT_PATH,
                model.name(), datasetPath)
                .redirectErrorStream(false) // keep stderr separate so it doesn't pollute stdout
                .start();
        this.stdin  = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public List<Integer> predict(final List<Node> nodesToPredict) {
        final String json = serializeNodes(nodesToPredict);
        try {
            stdin.write(json);
            stdin.newLine();
            stdin.flush();
            final String response = stdout.readLine();
            return deserializePredictions(response);
        } catch (final IOException ioException) {
            ioException.printStackTrace();
            return null;
        }
    }

    private static String serializeNodes(final List<Node> nodes) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"").append(VALUES_KEY).append("\":[");
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(Arrays.toString(nodes.get(i).values));
            if (i < nodes.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static List<Integer> deserializePredictions(final String response) {
        // expects: {"predictions": [0, 1, 2, ...]}
        final String stripped = response
                .substring(response.indexOf('[') + 1, response.lastIndexOf(']'));
        final List<Integer> predictions = new ArrayList<>();
        for (final String token : stripped.split(",")) {
            final String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                predictions.add(Integer.parseInt(trimmed));
            }
        }
        return predictions;
    }

    @Override
    public void close() throws IOException {
        stdin.close();
        process.destroy();
    }
}
package io.github.ryan_glgr.hansel_grapher.thehardstuff;

import io.github.ryan_glgr.hansel_grapher.datamanipulation.DatasetNormalizer;
import io.github.ryan_glgr.hansel_grapher.datamanipulation.NormalizedDataset;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.MLModel;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PythonInterpreter implements AutoCloseable {

    private static final String PYTHON_SCRIPT_PATH = String.join(
            File.separator, "src", "main", "resources", "python", "beginPredictions.py");
    private static final String PYTHON_COMMAND = "python3";

    private static final String VALUES_KEY   = "values";
    private  static final String STOP_MESSAGE = "exit";

    private final Process        process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;

    public PythonInterpreter(final MLModel model, final String normalizedDatasetPath) throws IOException {
        this.process = new ProcessBuilder(
                PYTHON_COMMAND, PYTHON_SCRIPT_PATH,
                "--classifier", model.pythonName,
                "--dataset",    normalizedDatasetPath)
                .redirectErrorStream(false)
                .redirectError(ProcessBuilder.Redirect.DISCARD)  // ← drop stderr entirely
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.process.destroy();           // SIGTERM first
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();   // SIGKILL if it doesn't respond
                }
            } catch (final InterruptedException ignored) {}
        }));

        this.stdin  = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

        final Thread stderrDrainer = new Thread(() -> {
            try (final BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = err.readLine()) != null)
                    System.err.println("[python] " + line);
            } catch (final IOException ignored) {}
        });
        stderrDrainer.setDaemon(true);
        stderrDrainer.start();
    }

    // ---------------------------------------------------------------------------
    // Predict
    // ---------------------------------------------------------------------------

    public List<Integer> predict(final List<Node> nodesToPredict) {
        try {
            final String json = serializeNodes(nodesToPredict);
            stdin.write(json);
            stdin.newLine();
            stdin.flush();

            final String response = stdout.readLine();
            if (response == null)
                throw new IOException("[oracle] Process closed stdout unexpectedly.");

            return deserializePredictions(response);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException("error while predicting datapoint", e);
        }
    }

    // ---------------------------------------------------------------------------
    // Shutdown
    // ---------------------------------------------------------------------------

    public void killPython() throws IOException {
        stdin.write(STOP_MESSAGE);
        stdin.newLine();
        stdin.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            killPython();
        } catch (final IOException ignored) {} // process may already be gone
        stdin.close();
        process.destroy();
    }

    // ---------------------------------------------------------------------------
    // Serialization
    // ---------------------------------------------------------------------------

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
            if (!trimmed.isEmpty())
                predictions.add(Integer.parseInt(trimmed));
        }
        return predictions;
    }

    public static PythonInterpreter getNormalizedDatasetAndBeginPredictionServer(final MLModel model, final NormalizedDataset dataset) {

        try {
            return new PythonInterpreter(model, dataset.serializeToJsonFile());
        } catch (final IOException ioException) {
            System.out.println("something has gone wrong in starting up the python process.");
            ioException.printStackTrace();
            throw new RuntimeException(ioException);
        }
    }
}
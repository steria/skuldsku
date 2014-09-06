package no.steria.skuldsku.testrunner.httprunner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.steria.skuldsku.recorder.httprecorder.ReportObject;

public class HttpPlayer {
    private final String baseUrl;
    private final List<PlaybackManipulator> manipulators = new ArrayList<>();

    public HttpPlayer(String baseUrl) {
        this.baseUrl = baseUrl;
        manipulators.add(new CookieHandler());
    }


    public void play(List<ReportObject> recordedHttp) {
        List<PlayStep> playBook = new ArrayList<>();
        for (ReportObject reportObject : recordedHttp) {
            playBook.add(new PlayStep(reportObject));
        }
        playSteps(playBook);
    }
    
    void playSteps(List<PlayStep> playbook) {
        for (PlayStep playStep : playbook) {
            try {
                playStep(playStep);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addManipulator(PlaybackManipulator manipulator) {
        manipulators.add(manipulator);
    }

    public void playStep(PlayStep playStep) throws IOException {

        ReportObject recordObject = playStep.getReportObject();

        System.out.println(String.format("Step: %s %s ***", recordObject.getMethod(), recordObject.getPath()));

        URL url = new URL(baseUrl + recordObject.getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String method = recordObject.getMethod();
        conn.setRequestMethod(method);
        String readInputStream = playStep.getReportObject().getReadInputStream();

        for (PlaybackManipulator manipulator : manipulators) {
            readInputStream = manipulator.computePayload(readInputStream);
        }

        Map<String, List<String>> headers = recordObject.getHeaders();

        // adjusts the headers of the request
        for (PlaybackManipulator manipulator : manipulators) {
            headers = manipulator.getHeaders(headers);
        }

        // writing headers
        if (headers != null) {
            Set<Map.Entry<String, List<String>>> entries = headers.entrySet();

            for (Map.Entry<String, List<String>> entry : entries) {
                String key = entry.getKey();
                for (String propval : entry.getValue()) {
                    String val = propval;
                    if ("Content-Length".equals(key) && "POST".equals(method) && readInputStream != null) {
                        val = "" + readInputStream.length();
                    }
                    conn.addRequestProperty(key, val);
                }
            }
        }

        // writes the body of the request
        if (readInputStream != null && !readInputStream.isEmpty()) {
            conn.setDoOutput(true);
            try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "utf-8"))) {
                printWriter.append(readInputStream);
            }
        }
        final Map<String, List<String>> headerFields = conn.getHeaderFields();

        manipulators.forEach(m -> m.reportHeaderFields(headerFields));


        //writes the parameters of the request
        String parameters = recordObject.getParametersRead().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + "&" + b).orElse(null);
        if (parameters != null) {
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(parameters);
            wr.flush();
            wr.close();
        }

        //recording response from server
        final StringBuilder result = new StringBuilder();
        try (InputStream is = conn.getInputStream()) {
            try (Reader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
                int c;
                while ((c = reader.read()) != -1) {
                    result.append((char) c);
                }
            }

        }

        playStep.setRecorded(result.toString());

        manipulators.forEach(m -> m.reportResult(result.toString()));
    }
}
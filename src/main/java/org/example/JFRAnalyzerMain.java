package org.example;

import jdk.jfr.consumer.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JFRAnalyzerMain {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello world!");

        getHotMethods();


    }

    private static void getHotMethods() throws IOException {
        Map<String, Long> methodCpuTime = new HashMap<>();
        Map<String, Long> methodSamples = new HashMap<>();
        int totalSamples = 0;
        long totalDuration = 0;

        try (RecordingFile rec = new RecordingFile(Paths.get("C:\\Amit\\Projects\\memoryLeak\\JFR\\OOM.jfr"))) {
            while (rec.hasMoreEvents()) {

                RecordedEvent event = rec.readEvent();

                //Events for hot methods
                if ("jdk.ExecutionSample".equals(event.getEventType().getName())) {
                    RecordedStackTrace stackTrace = event.getStackTrace();
                    List<RecordedFrame> stackTraceFrames = stackTrace.getFrames();

                    // get top most method in the stack
                    Instant startTime = event.getStartTime();

                    RecordedFrame frame = stackTrace.getFrames().getFirst();
                    RecordedMethod method = frame.getMethod();
                    String methodName = method.getName();
                    String className = method.getType().getName();
                    String fullyQualifiedMethodName = className + "." + methodName;

                    // Record the time spent by this method in nanos
                    long duration = event.getDuration().get(ChronoUnit.NANOS);
                    if (duration == 0) {duration++;}

                    long finalDuration = duration;
                    methodCpuTime.compute(fullyQualifiedMethodName, (k, v) -> v == null ? finalDuration : (v + finalDuration));
                    // number of samples per method
                    methodSamples.compute(fullyQualifiedMethodName, (k, v) -> v == null ? 1 : (v + 1));
                    // total samples
                    totalSamples++;
                    totalDuration += duration;

                    // Filter to pick only those call chains which are initiated via my application
                    if (stackTraceFrames.getLast().getMethod().getType().getName().contains("org.example")) {
                        System.out.println("-----------------Start----------------------");
                        System.out.printf(
                                "%s start=%s duration=%dns%n",
                                method.getName(), startTime, duration);

                        printStackTrace(stackTraceFrames);
                        System.out.println("-----------------End----------------------");
                    }
                }
            }
            printCPUUsageSummary(methodCpuTime, totalDuration, methodSamples);

        }
    }

    private static void printCPUUsageSummary(Map<String, Long> methodCpuTime, long totalDuration, Map<String, Long> methodSamples) {
        //cpuPerMethod = durationInTheMethod * 100 / totalDuration
        for (String m: methodCpuTime.keySet()) {
            Long durationInTheMethod = methodCpuTime.get(m);
            double cpuPerMethod = Math.round(durationInTheMethod * 100.00 / totalDuration);
            System.out.println("Method: "+ m + ", Samples: " + methodSamples.get(m) + ", CPU: "+ cpuPerMethod);
        }
    }

    private static void printStackTrace(List<RecordedFrame> stackTraceFrames) {
        int depth = 0;
        for (RecordedFrame recordedFrame : stackTraceFrames) {
            String methodName = recordedFrame.getMethod().getName();
            String className = recordedFrame.getMethod().getType().getName();
            int lineNumber = recordedFrame.getLineNumber();

            for (int j = 0; j < depth; j++) {
                System.out.print(" ");
            }
            System.out.println(className + "." + methodName + " line: " + lineNumber); // + "( " + fields + " )");
            depth++;
        }
    }
}
package org.example;

import jdk.jfr.consumer.*;
import org.example.models.Methods;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class JFRAnalyzerMain {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello world!");

        getHotMethods();


    }

    private static void getHotMethods() throws IOException {
        Map<String, Long> methodCpuTime = new HashMap<>();
        Map<String, Long> methodSamples = new HashMap<>();
        Map<String, Methods> methods = new HashMap<>();
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
                    if (duration == 0) {
                        duration++;
                    }

                    long finalDuration = duration;
                    String stackTraceString = getStackTrace(stackTraceFrames);
                    // total time spent in this method
                    methodCpuTime.compute(fullyQualifiedMethodName, (k, v) -> v == null ? finalDuration : (v + finalDuration));

                    // number of samples per method
                    methodSamples.compute(fullyQualifiedMethodName, (k, v) -> v == null ? 1 : (v + 1));

                    // save this method object
                    methods.compute(fullyQualifiedMethodName, (k, v) -> {
                        if (v == null) {
                            Methods m = new Methods(fullyQualifiedMethodName);
                            m.getStackTrace().add(stackTraceString);
                            m.setFinalDuration(finalDuration);
                            m.setSamples(1);
                            return m;
                        } else {
                            v.getStackTrace().add(stackTraceString);
                            v.setFinalDuration(v.getFinalDuration() + finalDuration);
                            v.setSamples(v.getSamples() + 1);
                        }
                        return v;
                    });

                    // total samples
                    totalSamples++;
                    totalDuration += duration;

                    // Filter to pick only those call chains which are initiated via my application
//                    if (stackTraceFrames.getLast().getMethod().getType().getName().contains("org.example")) {
//                        System.out.println("-----------------Start----------------------");
//                        System.out.printf(
//                                "%s start=%s duration=%dns%n",
//                                method.getName(), startTime, duration);
//
//                        printStackTrace(stackTraceFrames);
//                        System.out.println("-----------------End----------------------");
//                    }
                }
            }
//            printCPUUsageSummary(methodCpuTime, totalDuration, methodSamples);
            printCPUUsageSummary(methods, totalDuration);

        }
    }

    private static void printCPUUsageSummary(Map<String, Methods> methods, long totalDuration) {
        System.out.println("                                                  Java Methods that Executes the Most");
        System.out.println("Method                                                                                                Samples      Percent");
        System.out.println("(stackTrace.topFrame)                                                                                (startTime) (startTime)");
        System.out.println("---------------------------------------------------------------------------------------------------- ----------- -----------");

        MathContext mathContext = new MathContext(2);
        List<Methods> methodsByCPUUsage = methods.keySet().stream().map(methods::get).map(m -> {
            m.setCpu(new BigDecimal(m.getFinalDuration() * 100.0 / totalDuration, mathContext));
            return m;
        }).sorted().toList();

        List<Methods> methodsListJfrExcluded = methodsByCPUUsage.stream().filter(m -> !m.getName().contains("jdk.jfr")).toList();
        methodsListJfrExcluded.stream().limit(50).forEach(m -> System.out.printf("%-100s %11d %11s\n", m.getName(), m.getSamples(), m.getCpu().toPlainString()));

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("----Stack Trace of the top 5 methods----");
        methodsListJfrExcluded.stream().limit(5).forEach(m -> {
            System.out.println("Method name: " + m.getName());
            System.out.println("Stacktrace: ");
            int seq = 0;
            for (String s : m.getStackTrace()) {
                seq++;
                System.out.println(seq + ")\n" + s);
            }
        });
//        for(Methods m: methodsListJfrExcluded){
//            System.out.println("Method name: " + m.getName());
//            System.out.println("Stacktrace: ");
//            for(String s: m.getStackTrace()){
//                System.out.println(s);
//            }
//        }

    }

//    private static void printCPUUsageSummary(Map<String, Long> methodCpuTime, long totalDuration, Map<String, Long> methodSamples) {
//        //cpuPerMethod = durationInTheMethod * 100 / totalDuration
//        MathContext mathContext = new MathContext(2);
//        System.out.println("                                                  Java Methods that Executes the Most");
//        System.out.println("Method                                                                                                Samples      Percent");
//        System.out.println("(stackTrace.topFrame)                                                                                (startTime) (startTime)");
//        System.out.println("---------------------------------------------------------------------------------------------------- ----------- -----------");
//        for (String m : methodCpuTime.keySet()) {
//            Long durationInTheMethod = methodCpuTime.get(m);
//            BigDecimal cpuPerMethod = new BigDecimal(durationInTheMethod * 100.00 / totalDuration, mathContext);
//            System.out.printf("%-100s %11d %11s\n", m, methodSamples.get(m), cpuPerMethod.toPlainString());
////            System.out.println(m + " " + methodSamples.get(m) + " "+ cpuPerMethod);
//        }
//    }

//    private static void printStackTrace(List<RecordedFrame> stackTraceFrames) {
//        int depth = 0;
//        for (RecordedFrame recordedFrame : stackTraceFrames) {
//            String methodName = recordedFrame.getMethod().getName();
//            String className = recordedFrame.getMethod().getType().getName();
//            int lineNumber = recordedFrame.getLineNumber();
//
//            for (int j = 0; j < depth; j++) {
//                System.out.print(" ");
//            }
//            System.out.println(className + "." + methodName + " line: " + lineNumber); // + "( " + fields + " )");
//            depth++;
//        }
//    }

    private static String getStackTrace(List<RecordedFrame> stackTraceFrames) {
        int depth = 0;
        StringBuilder stackTraceString = new StringBuilder();
        for (RecordedFrame recordedFrame : stackTraceFrames) {
            String methodName = recordedFrame.getMethod().getName();
            String className = recordedFrame.getMethod().getType().getName();
            int lineNumber = recordedFrame.getLineNumber();

            for (int j = 0; j < depth; j++) {
                stackTraceString.append(" ");
            }
            stackTraceString.append(className).append(".").append(methodName).append(" line: ").append(lineNumber).append("\n");
            depth++;
        }
        return stackTraceString.toString();
    }
}
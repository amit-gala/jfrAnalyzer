package org.example.jfr.views;

import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryLeakCandidate {
    public void getMemoryLeakCandidateByClass() throws IOException {
        try (RecordingFile rec = new RecordingFile(Paths.get("C:\\Amit\\Projects\\memoryLeak\\JFR\\OOM.jfr"))) {
            Map<String, MemoryLeakInfo> leakCandidates = new HashMap<>();
            Map<String, MemoryLeakInfo> leakCandidatesByMethod = new HashMap<>();
            while (rec.hasMoreEvents()) {

                RecordedEvent event = rec.readEvent();

                //Events for hot methods
                if ("jdk.OldObjectSample".equals(event.getEventType().getName())) {
//                    event.toString();
                    String className = "";
                    String fullyQualifiedMethodName = "";
                    String methodType = "";
                    String methodName = "";
//                    String startTime = event.getStartTime().toString().substring(0, 19);

                    String allocationTime = OffsetDateTime.ofInstant(event.getInstant("allocationTime"), ZoneOffset.of("+05:30")).toString().substring(0, 19);
                    String objectAge = new BigDecimal(event.getDuration("objectAge").toSeconds() / 60.0, new MathContext(3, RoundingMode.HALF_EVEN)).toPlainString().concat(" m");

                    // Get the object class name
                    RecordedObject recordedObject = event.getValue("object");
                    RecordedClass clazz = recordedObject.getClass("type");
                    if (clazz != null) {
                        className = clazz.getName();
                        if (className != null && className.startsWith("[")) {
                            long arraySize = event.getLong("arrayElements");
                            className = decodeDescriptors(className, arraySize > 0 ? Long.toString(arraySize) : "").getFirst();
                        }
                    }

                    // Get the method name
                    methodType = event.getStackTrace().getFrames().getLast().getMethod().getType().getName();
                    methodName = event.getStackTrace().getFrames().getLast().getMethod().getName();
                    fullyQualifiedMethodName = methodType + "." + methodName;

                    String lastKnownHeapUsage = new BigDecimal(event.getLong("lastKnownHeapUsage") / (1024.0 * 1024.0), new MathContext(3, RoundingMode.HALF_EVEN)).toPlainString().concat(" MB");

                    // Aggregate by object type
                    leakCandidates.computeIfAbsent(className, k -> new MemoryLeakInfo())
                            .update(allocationTime, className, objectAge, lastKnownHeapUsage);

                    leakCandidatesByMethod.computeIfAbsent(fullyQualifiedMethodName, k -> new MemoryLeakInfo())
                            .update(allocationTime, fullyQualifiedMethodName, objectAge, lastKnownHeapUsage);
                }
            }
            // Print out results similar to "Memory Leak Candidates by Class"
            printMemLeakByClass(leakCandidates);
            System.out.println();
            System.out.println();
            printMemLeakByMethod(leakCandidatesByMethod);
        }
    }

    private static void printMemLeakByClass(Map<String, MemoryLeakInfo> leakCandidates) {
        System.out.println("Memory Leak Candidates by Class:");
        System.out.printf("%-20s %-70s %-15s %-15s%n", "Alloc. Time", "Object Class", "Object Age", "Heap Usage");
        for (MemoryLeakInfo info : leakCandidates.values().stream().sorted().toList()) {
            System.out.printf("%-20s %-70s %-15s %-15s%n",
                    info.allocationTime, info.objectClass, info.objectAge, info.heapUsage);
        }
    }

    private static void printMemLeakByMethod(Map<String, MemoryLeakInfo> leakCandidates) {
        System.out.println("Memory Leak Candidates by Method:");
        System.out.printf("%-20s %-70s %-15s %-15s%n", "Alloc. Time", "Application Method", "Object Age", "Heap Usage");
        for (MemoryLeakInfo info : leakCandidates.values().stream().sorted().toList()) {
            System.out.printf("%-20s %-70s %-15s %-15s%n",
                    info.allocationTime, info.objectClass, info.objectAge, info.heapUsage);
        }
    }

    // Helper class to store memory leak candidate information
    private static class MemoryLeakInfo implements Comparable<MemoryLeakInfo> {
        String allocationTime;
        String objectClass;
        String objectAge;
        String heapUsage;

        void update(String allocationTime, String objectClass, String objectAge, String heapUsage) {
            this.allocationTime = allocationTime;
            this.objectClass = objectClass;
            this.objectAge = objectAge;
            this.heapUsage = heapUsage;
        }

        @Override
        public int compareTo(MemoryLeakInfo o) {
            return o.objectAge.compareTo(this.objectAge);
        }
    }

    public static List<String> decodeDescriptors(String descriptor, String arraySize) {
        List<String> descriptors = new ArrayList<>();
        for (int index = 0; index < descriptor.length(); index++) {
            String arrayBrackets = "";
            while (descriptor.charAt(index) == '[') {
                arrayBrackets = arrayBrackets + "[" + arraySize + "]";
                arraySize = "";
                index++;
            }
            char c = descriptor.charAt(index);
            String type;
            switch (c) {
                case 'L':
                    int endIndex = descriptor.indexOf(';', index);
                    type = descriptor.substring(index + 1, endIndex);
                    index = endIndex;
                    break;
                case 'I':
                    type = "int";
                    break;
                case 'J':
                    type = "long";
                    break;
                case 'Z':
                    type = "boolean";
                    break;
                case 'D':
                    type = "double";
                    break;
                case 'F':
                    type = "float";
                    break;
                case 'S':
                    type = "short";
                    break;
                case 'C':
                    type = "char";
                    break;
                case 'B':
                    type = "byte";
                    break;
                default:
                    type = "<unknown-descriptor-type>";
            }
            descriptors.add(type + arrayBrackets);
        }
        return descriptors;
    }
}

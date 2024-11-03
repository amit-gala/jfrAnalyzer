package org.example.jfr.views;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class GarbageCollectionView {
    public void getGarbageCollectionInfo() throws IOException {
        try (
                RecordingFile rec = new RecordingFile(Paths.get("C:\\Amit\\Projects\\memoryLeak\\JFR\\OOM.jfr"))) {
            Map<Long, GCInfo> gcInfoMap = new HashMap<>();
            Map<Instant, GCHeapConfiguration> gcHeapConfigurationMap = new HashMap<>();
            String beforeOrAfter;
            long gcId;
            String gcType;
            Long heapBeforeGC;
            Long heapAfterGC;
            long longestPause;
            Long heapUsed;
            Instant startTime;
            long initialSize;
            long minSize;
            long maxSize;

            while (rec.hasMoreEvents()) {

                RecordedEvent event = rec.readEvent();
                String eventName = event.getEventType().getName();

                //Events for hot methods
                switch (eventName) {
                    case "jdk.GarbageCollection":
                        startTime = event.getStartTime();
                        gcId = event.getLong("gcId");
                        gcType = event.getValue("name");
                        longestPause = ((Duration) event.getDuration("longestPause")).toNanos();
                        gcInfoMap.computeIfAbsent(gcId, k -> new GCInfo())
                                .update(startTime, gcId, gcType, longestPause);
                        break;

                    case "jdk.GCHeapSummary":
                        gcId = event.getLong("gcId");
                        beforeOrAfter = event.getValue("when");
                        heapUsed = event.getLong("heapUsed");
                        if (beforeOrAfter.equals("Before GC")) {
                            gcInfoMap.computeIfAbsent(gcId, k -> new GCInfo())
                                    .setHeapBeforeGC(heapUsed);
                        } else if (beforeOrAfter.equals("After GC")) {
                            gcInfoMap.computeIfAbsent(gcId, k -> new GCInfo())
                                    .setHeapAfterGC(heapUsed);
                        }
                        break;

                    case "jdk.GCHeapConfiguration":
                        startTime = event.getStartTime();
                        initialSize = event.getLong("initialSize");
                        minSize = event.getLong("minSize");
                        maxSize = event.getLong("maxSize");
                        gcHeapConfigurationMap.computeIfAbsent(startTime, k -> new GCHeapConfiguration())
                                .update(startTime, initialSize, minSize, maxSize);
                        break;
                    default:
                        break;

                }
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());
            System.out.println("Garbage Collections");
            System.out.printf("%12s\t%5s\t%5s\t%16s\t%16s\t%s\n", "Start", "GC ID", "Type", "Heap Before GC", "Heap After GC", "Longest Pause");

            gcInfoMap.keySet().stream().map(gcInfoMap::get).forEach(gc -> {
                System.out.printf("%12s\t%5d\t%5s\t%13d MB\t%13d MB\t%d ms\n",
                        formatter.format(gc.getStartTime()), gc.getGcID(), gc.getGcType(), gc.getHeapBeforeGC()/(1024 * 1024), gc.getHeapAfterGC()/(1024 * 1024), gc.getLongestPause()/1_000_000);
            });


            System.out.println();
            System.out.println();
            System.out.println("Heap Configuration");
            gcHeapConfigurationMap.keySet().stream().limit(1).map(gcHeapConfigurationMap::get).forEach(g ->
                    System.out.printf("Initial Heap Size: %d MB\n" +
                            "\n" +
                            "Minimum Heap Size: %d MB\n" +
                            "\n" +
                            "Maximum Heap Size: %d MB", g.getInitialSize()/(1024 * 1024), g.getMinSize()/(1024 * 1024), g.getMaxSize()/(1024 * 1024)));

        }

    }

    @Getter
    @Setter
    private static class GCInfo {
        private Long gcID;
        private Instant startTime;
        private String gcType;
        private Long heapBeforeGC;
        private Long heapAfterGC;
        private Long longestPause;

        public void update(Instant startTime, long gcId, String gcType, long longestPause) {
            this.gcID = gcId;
            this.gcType = gcType;
            this.startTime = startTime;
            this.longestPause = longestPause;
        }

        public Long getGcID() {
            return gcID;
        }

        public void setGcID(Long gcID) {
            this.gcID = gcID;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }

        public String getGcType() {
            return gcType;
        }

        public void setGcType(String gcType) {
            this.gcType = gcType;
        }

        public Long getHeapBeforeGC() {
            return heapBeforeGC;
        }

        public void setHeapBeforeGC(Long heapBeforeGC) {
            this.heapBeforeGC = heapBeforeGC;
        }

        public Long getHeapAfterGC() {
            return heapAfterGC;
        }

        public void setHeapAfterGC(Long heapAfterGC) {
            this.heapAfterGC = heapAfterGC;
        }

        public Long getLongestPause() {
            return longestPause;
        }

        public void setLongestPause(Long longestPause) {
            this.longestPause = longestPause;
        }
    }

    private static class GCHeapConfiguration {
        long initialSize;
        long minSize;
        long maxSize;
        Instant startTime;

        public void update(Instant startTime, long initialSize, long minSize, long maxSize) {
            this.initialSize = initialSize;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.startTime = startTime;
        }

        public long getInitialSize() {
            return initialSize;
        }

        public long getMinSize() {
            return minSize;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}


package org.example.models;

import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
public class Methods implements Comparator<Methods>, Comparable<Methods> {
    final private String name;
    private int samples;
    private BigDecimal cpu;
    private Set<String> stackTrace;
    private String stackTraceString;
    private long finalDuration;

    public Methods(String name) {
        this.name = name;
        this.samples = 0;
        this.cpu = BigDecimal.ZERO;
        this.stackTrace = new HashSet<>();
        this.finalDuration = 0;
    }

    @Override
    public int compare(Methods o1, Methods o2) {
        return o1.getCpu().compareTo(o2.getCpu());
    }

    // Highest to lowest cpu usage
    @Override
    public int compareTo(Methods o) {
        return o.getCpu().compareTo(this.getCpu());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Methods methods = (Methods) o;
        return Objects.equals(name, methods.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public String getName() {
        return name;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public BigDecimal getCpu() {
        return cpu;
    }

    public void setCpu(BigDecimal cpu) {
        this.cpu = cpu;
    }

    public Set<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(Set<String> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStackTraceString() {
        return stackTraceString;
    }

    public void setStackTraceString(String stackTraceString) {
        this.stackTraceString = stackTraceString;
    }

    public long getFinalDuration() {
        return finalDuration;
    }

    public void setFinalDuration(long finalDuration) {
        this.finalDuration = finalDuration;
    }
}

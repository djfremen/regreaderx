package com.example.regreader;

public final class InstallationType implements Comparable<InstallationType> {
    private final int ord;
    private final String label;
    private InstallationType prev;
    private InstallationType next;

    private static InstallationType first = null;
    private static InstallationType last = null;

    public static final InstallationType STANDALONE = new InstallationType("STANDALONE", 1);
    public static final InstallationType SERVER = new InstallationType("SERVER", 2);

    private InstallationType(String label, int ord) {
        this.label = label;
        this.ord = ord;

        if (first == null) {
            first = this;
        }
        if (last != null) {
            this.prev = last;
            last.next = this;
        }

        last = this;
    }

    public static InstallationType get(int ordinal) {
        if (ordinal == 1) return STANDALONE;
        if (ordinal == 2) return SERVER;
        return null;
    }

    public int ord() {
        return this.ord;
    }

    public String toString() {
        return this.label;
    }

    public int compareTo(InstallationType other) {
        return this.ord - other.ord;
    }

    public InstallationType next() {
        return this.next;
    }
}
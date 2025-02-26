package com.example.regreader;

public final class AuthorizationStatus implements Comparable<AuthorizationStatus> {
    private final int ord;
    private final String label;
    private AuthorizationStatus prev;
    private AuthorizationStatus next;

    private static AuthorizationStatus first = null;
    private static AuthorizationStatus last = null;

    public static final AuthorizationStatus TEMPORARY = new AuthorizationStatus("TEMPORARY", 1);
    public static final AuthorizationStatus PENDING = new AuthorizationStatus("PENDING", 2);
    public static final AuthorizationStatus MIGRATED = new AuthorizationStatus("MIGRATED", 4);
    public static final AuthorizationStatus AUTHORIZED = new AuthorizationStatus("AUTHORIZED", 8);
    public static final AuthorizationStatus REVOKED = new AuthorizationStatus("REVOKED", 16);

    private AuthorizationStatus(String label, int ord) {
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

    public static AuthorizationStatus get(int ordinal) {
        if (ordinal == 1) return TEMPORARY;
        if (ordinal == 2) return PENDING;
        if (ordinal == 4) return MIGRATED;
        if (ordinal == 8) return AUTHORIZED;
        if (ordinal == 16) return REVOKED;
        return null;
    }

    public int ord() {
        return this.ord;
    }

    public String toString() {
        return this.label;
    }

    public int compareTo(AuthorizationStatus other) {
        return this.ord - other.ord;
    }

    public AuthorizationStatus next() {
        return this.next;
    }
}
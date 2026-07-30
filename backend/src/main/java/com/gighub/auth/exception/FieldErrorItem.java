package com.gighub.auth.exception;

import java.util.Objects;

public final class FieldErrorItem {

    private final String field;
    private final String reason;

    public FieldErrorItem(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldErrorItem)) {
            return false;
        }
        FieldErrorItem that = (FieldErrorItem) other;
        return Objects.equals(field, that.field) && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, reason);
    }
}

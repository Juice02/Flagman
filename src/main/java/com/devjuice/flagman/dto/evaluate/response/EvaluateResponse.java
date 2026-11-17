package com.devjuice.flagman.dto.evaluate.response;

public class EvaluateResponse {

    private boolean enabled;
    private EvaluationReason reason;

    public EvaluateResponse(boolean enabled, EvaluationReason reason) {
        this.enabled = enabled;
        this.reason = reason;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public EvaluationReason getReason() {
        return reason;
    }
}

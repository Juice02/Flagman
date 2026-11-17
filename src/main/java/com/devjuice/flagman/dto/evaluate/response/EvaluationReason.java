package com.devjuice.flagman.dto.evaluate.response;

// Why a flag evaluated the way it did, in evaluation order.
public enum EvaluationReason {
    FLAG_DISABLED,
    RULES_NOT_MATCHED,
    IN_ROLLOUT,
    OUT_OF_ROLLOUT
}

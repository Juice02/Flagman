package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.dto.evaluate.response.EvaluationReason;
import com.devjuice.flagman.dto.snapshot.response.FlagState;
import com.devjuice.flagman.dto.snapshot.response.RuleState;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

// Pure flag evaluation, no database. Browser clients run a copy of this logic (flagman-demo/src/evaluate.ts);
// src/test/resources/evaluation-cases.json is the shared contract both copies are tested against.
public final class Evaluator {

    // The operators matches() understands; RuleService rejects anything else. Keep the two in sync.
    public static final Set<String> OPERATORS = Set.of("EQUALS", "NOT_EQUALS", "IN", "CONTAINS");

    private Evaluator() {
    }

    // Order: flag disabled -> rules (all must match) -> percentage rollout among the users who matched.
    public static EvaluateResponse evaluate(FlagState flag, String userId, Map<String, String> attributes) {
        if (!flag.enabled()) {
            return new EvaluateResponse(false, EvaluationReason.FLAG_DISABLED);
        }
        for (RuleState rule : flag.rules()) {
            if (!matches(rule, attributes.get(rule.attribute()))) {
                return new EvaluateResponse(false, EvaluationReason.RULES_NOT_MATCHED);
            }
        }
        boolean inRollout = bucket(flag.flagKey(), userId) < flag.rolloutPercentage();
        return new EvaluateResponse(inRollout, inRollout ? EvaluationReason.IN_ROLLOUT : EvaluationReason.OUT_OF_ROLLOUT);
    }

    // The same user and flag always land in the same 0-99 bucket; the flag key keeps buckets independent per flag.
    static int bucket(String flagKey, String userId) {
        CRC32 crc = new CRC32();
        crc.update((flagKey + ":" + userId).getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % 100);
    }

    // Fails closed: a missing attribute or an unknown operator never matches.
    static boolean matches(RuleState rule, String actual) {
        String expected = rule.value();
        if (actual == null || expected == null || rule.operator() == null) {
            return false;
        }
        return switch (rule.operator().toUpperCase()) {
            case "EQUALS" -> actual.equals(expected);
            case "NOT_EQUALS" -> !actual.equals(expected);
            case "IN" -> Arrays.stream(expected.split(",")).map(String::trim).anyMatch(actual::equals);
            case "CONTAINS" -> actual.contains(expected);
            default -> false;
        };
    }
}

package com.devjuice.flagman.dto.snapshot.response;

// One targeting rule as clients need it to evaluate a flag.
public record RuleState(String attribute, String operator, String value) {
}

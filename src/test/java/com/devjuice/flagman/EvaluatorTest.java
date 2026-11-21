package com.devjuice.flagman;

import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.dto.snapshot.response.FlagState;
import com.devjuice.flagman.service.Evaluator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Runs evaluation-cases.json against Evaluator. Browser clients run the same file against their copy
// (flagman-demo/scripts/evaluate.test.ts), so both give the same answer. No database needed:
// ./mvnw test -Dtest=EvaluatorTest
class EvaluatorTest {

    record Expected(boolean enabled, String reason) {
    }

    record Case(String name, FlagState flag, String userId, Map<String, String> attributes, Expected expected) {
    }

    @TestFactory
    List<DynamicTest> sharedCases() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/evaluation-cases.json")) {
            List<Case> cases = JsonMapper.builder().build().readValue(in, new TypeReference<List<Case>>() {});
            return cases.stream().map(c -> DynamicTest.dynamicTest(c.name(), () -> {
                EvaluateResponse result = Evaluator.evaluate(c.flag(), c.userId(), c.attributes());
                assertEquals(c.expected().enabled(), result.isEnabled(), "enabled");
                assertEquals(c.expected().reason(), result.getReason().name(), "reason");
            })).toList();
        }
    }
}

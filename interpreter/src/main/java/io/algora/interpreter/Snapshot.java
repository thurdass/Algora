package io.algora.interpreter;

import java.util.Map;

public record Snapshot(int line, Map<String, Object> variables, Status status, long steps) {}

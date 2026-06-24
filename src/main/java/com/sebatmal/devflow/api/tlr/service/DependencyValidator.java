package com.sebatmal.devflow.api.tlr.service;

import com.sebatmal.devflow.api.tlr.dto.DependencyCandidate;
import com.sebatmal.devflow.api.tlr.dto.TlrIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DependencyValidator {

    private static final double MIN_CONFIDENCE = 0.75;
    private static final Set<String> VALID_TYPES = Set.of(
            "API_CONTRACT", "DATA", "AUTH", "WORKFLOW", "COMMON_MODULE", "INFRA", "TEST"
    );

    public record ValidationResult(
            List<DependencyCandidate> valid,
            List<String> errors
    ) {
    }

    public ValidationResult validate(final List<DependencyCandidate> candidates, final List<TlrIssue> issues) {
        final Set<Integer> issueNumbers = new HashSet<>();
        for (final TlrIssue issue : issues) {
            issueNumbers.add(issue.issueNumber());
        }

        final List<DependencyCandidate> valid = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        for (final DependencyCandidate candidate : candidates) {
            final int from = candidate.fromIssueNumber();
            final int to = candidate.toIssueNumber();
            final String edgeKey = from + "->" + to;

            if (from == to) {
                errors.add("#%d → #%d: 자기 자신 의존 불가".formatted(from, to));
                continue;
            }
            if (!issueNumbers.contains(from)) {
                errors.add("#%d → #%d: fromIssueNumber #%d가 입력 이슈 목록에 없음".formatted(from, to, from));
                continue;
            }
            if (!issueNumbers.contains(to)) {
                errors.add("#%d → #%d: toIssueNumber #%d가 입력 이슈 목록에 없음".formatted(from, to, to));
                continue;
            }
            if (!seen.add(edgeKey)) {
                errors.add("#%d → #%d: 중복 간선".formatted(from, to));
                continue;
            }
            if (candidate.confidence() < MIN_CONFIDENCE) {
                errors.add("#%d → #%d: confidence %.2f < 0.75 기준 미달".formatted(from, to, candidate.confidence()));
                continue;
            }
            if (!VALID_TYPES.contains(candidate.type())) {
                errors.add("#%d → #%d: 알 수 없는 type '%s'".formatted(from, to, candidate.type()));
                continue;
            }

            valid.add(candidate);
        }

        return new ValidationResult(valid, errors);
    }
}

package com.sebatmal.devflow.api.tlr.service;

import com.sebatmal.devflow.api.tlr.dto.DependencyCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DagValidator {

    public record DagResult(
            List<Integer> cycle,
            List<Integer> topologicalOrder
    ) {
        public boolean hasCycle() {
            return !cycle.isEmpty();
        }
    }

    /**
     * DFS 기반 순환 탐지 + 위상정렬.
     * state: 0=미방문, 1=탐색중(GRAY), 2=완료(BLACK)
     */
    public DagResult analyze(final List<Integer> nodes, final List<DependencyCandidate> dependencies) {
        final Map<Integer, List<Integer>> adjacency = buildAdjacency(dependencies);

        final Map<Integer, Integer> state = new HashMap<>();
        for (final Integer node : nodes) {
            state.put(node, 0);
        }

        final List<Integer> cycleResult = new ArrayList<>();
        final List<Integer> topoResult = new ArrayList<>();

        for (final Integer node : nodes) {
            if (state.get(node) == 0) {
                final List<Integer> path = new ArrayList<>();
                if (dfs(node, adjacency, state, path, cycleResult, topoResult)) {
                    return new DagResult(cycleResult, List.of());
                }
            }
        }

        Collections.reverse(topoResult);
        return new DagResult(List.of(), topoResult);
    }

    private boolean dfs(
            final Integer node,
            final Map<Integer, List<Integer>> adjacency,
            final Map<Integer, Integer> state,
            final List<Integer> path,
            final List<Integer> cycleResult,
            final List<Integer> topoResult
    ) {
        state.put(node, 1);
        path.add(node);

        for (final Integer next : adjacency.getOrDefault(node, List.of())) {
            final int nextState = state.getOrDefault(next, 0);
            if (nextState == 1) {
                // GRAY 노드를 다시 만나면 사이클 — path에서 사이클 구간 추출
                final int cycleStart = path.indexOf(next);
                cycleResult.addAll(path.subList(cycleStart, path.size()));
                cycleResult.add(next);
                return true;
            }
            if (nextState == 0) {
                if (dfs(next, adjacency, state, path, cycleResult, topoResult)) {
                    return true;
                }
            }
        }

        state.put(node, 2);
        path.remove(path.size() - 1);
        topoResult.add(node);
        return false;
    }

    private Map<Integer, List<Integer>> buildAdjacency(final List<DependencyCandidate> dependencies) {
        final Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (final DependencyCandidate dep : dependencies) {
            adjacency.computeIfAbsent(dep.fromIssueNumber(), k -> new ArrayList<>())
                    .add(dep.toIssueNumber());
        }
        return adjacency;
    }
}

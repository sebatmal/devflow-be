package com.sebatmal.devflow.common.graph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 의존성 그래프 알고리즘 (AI 아님 — 결정론적).
 * 엣지 from→to 의 의미: from 이 끝나야 to 시작. (to is blocked_by from)
 */
public final class DependencyGraph {

    private DependencyGraph() {
    }

    /**
     * 기존 엣지에 from→to 를 추가했을 때 순환이 생기는지.
     * to 에서 from 으로 이미 도달 가능하면(to →* from), from→to 추가 시 사이클이 닫힌다.
     */
    public static boolean wouldCreateCycle(final Map<Long, List<Long>> adjacency, final Long fromId, final Long toId) {
        return reachable(adjacency, toId, fromId);
    }

    /** start 에서 target 으로 도달 가능한지 (DFS). */
    public static boolean reachable(final Map<Long, List<Long>> adjacency, final Long start, final Long target) {
        if (start.equals(target)) {
            return true;
        }
        final Deque<Long> stack = new ArrayDeque<>();
        final Set<Long> visited = new HashSet<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            final Long node = stack.pop();
            if (!visited.add(node)) {
                continue;
            }
            if (node.equals(target)) {
                return true;
            }
            for (final Long next : adjacency.getOrDefault(node, List.of())) {
                stack.push(next);
            }
        }
        return false;
    }

    /** 전체 그래프에 사이클이 있는지 (Kahn's algorithm — 위상정렬 가능 여부). */
    public static boolean hasCycle(final Collection<Long> nodes, final Map<Long, List<Long>> adjacency) {
        final Map<Long, Integer> indegree = new HashMap<>();
        for (final Long node : nodes) {
            indegree.put(node, 0);
        }
        for (final Long from : adjacency.keySet()) {
            for (final Long to : adjacency.get(from)) {
                indegree.merge(to, 1, Integer::sum);
            }
        }
        final Deque<Long> queue = new ArrayDeque<>();
        for (final Long node : nodes) {
            if (indegree.getOrDefault(node, 0) == 0) {
                queue.add(node);
            }
        }
        int processed = 0;
        while (!queue.isEmpty()) {
            final Long node = queue.poll();
            processed++;
            for (final Long next : adjacency.getOrDefault(node, List.of())) {
                indegree.merge(next, -1, Integer::sum);
                if (indegree.get(next) == 0) {
                    queue.add(next);
                }
            }
        }
        return processed != nodes.size();
    }
}

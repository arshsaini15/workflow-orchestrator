package com.arsh.workflow.validation;

import com.arsh.workflow.dto.request.BatchTaskRequest;
import com.arsh.workflow.exception.InvalidWorkflowDefinitionException;
import com.arsh.workflow.model.Task;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WorkflowGraphValidator {

    /**
     * DTO-level validation.
     * Validates clientIds, dependency references, and detects cycles
     * BEFORE entities are created.
     */
    public void validateOrThrow(List<BatchTaskRequest> tasks) {

        List<String> errors = new ArrayList<>();

        if (tasks == null || tasks.isEmpty()) {
            throw new InvalidWorkflowDefinitionException(
                    List.of("Workflow must contain at least one task.")
            );
        }

        // ---- 1. Validate unique aliases ----
        Map<String, BatchTaskRequest> aliasToTask = new HashMap<>();

        for (BatchTaskRequest t : tasks) {
            String alias = t.getClientId();

            if (alias == null || alias.isBlank()) {
                errors.add("Task alias (clientId) cannot be null or blank.");
                continue;
            }

            if (aliasToTask.containsKey(alias)) {
                errors.add("Duplicate task alias: " + alias);
            } else {
                aliasToTask.put(alias, t);
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidWorkflowDefinitionException(errors);
        }

        // ---- 2. Build adjacency list ----
        Map<String, List<String>> adj = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();

        for (String alias : aliasToTask.keySet()) {
            adj.put(alias, new ArrayList<>());
            indegree.put(alias, 0);
        }

        // ---- 3. Validate dependencies ----
        for (BatchTaskRequest task : tasks) {

            String alias = task.getClientId();
            List<String> deps = task.getDependsOn();

            if (deps == null) continue;

            Set<String> seenDeps = new HashSet<>();

            for (String depAlias : deps) {

                if (!seenDeps.add(depAlias)) {
                    errors.add(
                            "Task '" + alias + "' has duplicate dependency '" + depAlias + "'"
                    );
                    continue;
                }

                if (depAlias == null || depAlias.isBlank()) {
                    errors.add(
                            "Task '" + alias + "' has a blank dependency alias."
                    );
                    continue;
                }

                if (alias.equals(depAlias)) {
                    errors.add(
                            "Task '" + alias + "' cannot depend on itself."
                    );
                    continue;
                }

                if (!aliasToTask.containsKey(depAlias)) {
                    errors.add(
                            "Task '" + alias + "' depends on non-existent alias '" + depAlias + "'"
                    );
                    continue;
                }

                // Edge: dep → alias
                adj.get(depAlias).add(alias);
                indegree.put(alias, indegree.get(alias) + 1);
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidWorkflowDefinitionException(errors);
        }

        // ---- 4. Cycle detection (Kahn’s Algorithm) ----
        Queue<String> queue = new ArrayDeque<>();
        int visited = 0;

        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.offer(e.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            visited++;

            for (String next : adj.get(current)) {
                indegree.put(next, indegree.get(next) - 1);
                if (indegree.get(next) == 0) {
                    queue.offer(next);
                }
            }
        }

        if (visited != aliasToTask.size()) {
            throw new InvalidWorkflowDefinitionException(
                    List.of("Workflow contains a cyclic dependency and is not a DAG.")
            );
        }
    }

    /**
     * ENTITY-level validation.
     * Validates the resolved Task graph AFTER entities are wired together.
     * Protects against runtime corruption.
     */
    public void validateResolvedDag(List<Task> tasks) {

        if (tasks == null || tasks.isEmpty()) return;

        Map<Task, List<Task>> graph = new HashMap<>();

        for (Task task : tasks) {
            graph.putIfAbsent(task, new ArrayList<>());

            if (task.getDependsOn() != null) {
                for (Task parent : task.getDependsOn()) {

                    if (task == parent) {
                        throw new IllegalStateException(
                                "Task cannot depend on itself: " + task.getTitle()
                        );
                    }

                    graph.get(task).add(parent);
                }
            }
        }

        // DFS cycle detection
        Set<Task> visiting = new HashSet<>();
        Set<Task> visited = new HashSet<>();

        for (Task task : graph.keySet()) {
            if (!visited.contains(task)) {
                dfs(task, graph, visiting, visited);
            }
        }
    }

    private void dfs(
            Task node,
            Map<Task, List<Task>> graph,
            Set<Task> visiting,
            Set<Task> visited
    ) {

        if (visiting.contains(node)) {
            throw new IllegalStateException(
                    "Cycle detected in workflow DAG at task: " + node.getTitle()
            );
        }

        if (visited.contains(node)) return;

        visiting.add(node);

        for (Task parent : graph.getOrDefault(node, List.of())) {
            dfs(parent, graph, visiting, visited);
        }

        visiting.remove(node);
        visited.add(node);
    }
}

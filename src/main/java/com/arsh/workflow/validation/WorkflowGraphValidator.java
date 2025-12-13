package com.arsh.workflow.validation;

import com.arsh.workflow.dto.request.BatchTaskRequest;
import com.arsh.workflow.exception.InvalidWorkflowDefinitionException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WorkflowGraphValidator {

    public void validateOrThrow(List<BatchTaskRequest> tasks) {

        List<String> errors = new ArrayList<>();

        if (tasks == null || tasks.isEmpty()) {
            throw new InvalidWorkflowDefinitionException(
                    List.of("Workflow must contain at least one task.")
            );
        }

        // 1. Validate unique aliases
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

        int n = aliasToTask.size();

        // 2. Build adjacency list and indegree
        Map<String, List<String>> adj = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();

        for (String alias : aliasToTask.keySet()) {
            adj.put(alias, new ArrayList<>());
            indegree.put(alias, 0);
        }

        // 3. Validate dependencies and build edges
        for (BatchTaskRequest task : tasks) {
            String alias = task.getClientId();
            List<String> deps = task.getDependsOn();

            if (deps == null) continue;

            Set<String> seenDeps = new HashSet<>();

            for (String depAlias : deps) {

                if (!seenDeps.add(depAlias)) {
                    errors.add("Task '" + alias + "' has duplicate dependency '" + depAlias + "'");
                    continue;
                }

                if (depAlias == null || depAlias.isBlank()) {
                    errors.add("Task '" + alias + "' has a blank dependency alias.");
                    continue;
                }

                if (alias.equals(depAlias)) {
                    errors.add("Task '" + alias + "' cannot depend on itself.");
                    continue;
                }

                if (!aliasToTask.containsKey(depAlias)) {
                    errors.add("Task '" + alias + "' depends on non-existent alias '" + depAlias + "'");
                    continue;
                }

                // Add edge dependency: dep → alias
                adj.get(depAlias).add(alias);
                indegree.put(alias, indegree.get(alias) + 1);
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidWorkflowDefinitionException(errors);
        }

        // 4. Cycle detection (Kahn's Algorithm)
        Queue<String> q = new ArrayDeque<>();

        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                q.offer(e.getKey());
            }
        }

        int visited = 0;

        while (!q.isEmpty()) {
            String current = q.poll();
            visited++;

            for (String next : adj.get(current)) {
                int newIn = indegree.get(next) - 1;
                indegree.put(next, newIn);

                if (newIn == 0) {
                    q.offer(next);
                }
            }
        }

        if (visited != n) {
            throw new InvalidWorkflowDefinitionException(
                    List.of("Workflow contains a cyclic dependency and is not a DAG.")
            );
        }
    }
}

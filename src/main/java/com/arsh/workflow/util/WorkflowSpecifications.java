package com.arsh.workflow.util;

import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.model.Workflow;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class WorkflowSpecifications {

    public static Specification<Workflow> filter(
            WorkflowStatus status,
            String username,
            String search
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (username != null && !username.isBlank()) {
                predicates.add(cb.equal(root.get("name"), username));
            }

            if (search != null && !search.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%")
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.example.paymenthub.repository.specification;

import com.example.paymenthub.entity.ProcessingComponent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComponentSpecification {

    private ComponentSpecification() {}

    public static Specification<ProcessingComponent> filter(
            String componentCode,
            String componentName,
            String messageType,
            String connectionMethod,
            List<Integer> statuses,
            List<Integer> isActives
    ) {
        return Specification
                .where(matchAnyString("componentCode", componentCode))
                .and(matchAnyString("componentName", componentName))
                .and(matchAnyString("messageType", messageType))
                .and(matchAnyString("connectionMethod", connectionMethod))
                .and(inList("status", statuses))
                .and(inList("isActive", isActives));
    }

    private static Specification<ProcessingComponent> matchAnyString(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return null;
            String[] tokens = value.split(",");
            List<String> cleanTokens = Arrays.stream(tokens)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (cleanTokens.isEmpty()) return null;

            List<Predicate> predicates = new ArrayList<>();
            for (String token : cleanTokens) {
                predicates.add(cb.like(cb.upper(root.get(field)), "%" + token.toUpperCase() + "%"));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<ProcessingComponent> inList(String field, List<Integer> values) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) return null;
            return root.get(field).in(values);
        };
    }
}

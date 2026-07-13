package com.example.paymenthub.repository.specification;

import com.example.paymenthub.entity.ProcessingComponent;
import org.springframework.data.jpa.domain.Specification;

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
                .where(likeIgnoreCase("componentCode", componentCode))
                .and(likeIgnoreCase("componentName", componentName))
                .and(likeIgnoreCase("messageType", messageType))
                .and(likeIgnoreCase("connectionMethod", connectionMethod))
                .and(inList("status", statuses))
                .and(inList("isActive", isActives));
    }

    private static Specification<ProcessingComponent> likeIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return null;
            return cb.like(cb.upper(root.get(field)), "%" + value.toUpperCase() + "%");
        };
    }

    private static Specification<ProcessingComponent> inList(String field, List<Integer> values) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) return null;
            return root.get(field).in(values);
        };
    }
}

package com.example.paymenthub.repository.specification;

import com.example.paymenthub.entity.GroupCategory;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public class GroupCategorySpecification {

    private GroupCategorySpecification() {
    }

    public static Specification<GroupCategory> filter(
            String paramType,
            String paramValue,
            String paramName,
            List<Integer> statuses,
            List<Integer> isActives) {
        return Specification
                .where(likeIgnoreCase("paramType", paramType))
                .and(likeIgnoreCase("paramValue", paramValue))
                .and(likeIgnoreCase("paramName", paramName))
                .and(inList("status", statuses))
                .and(inList("isActive", isActives));
    }

    private static Specification<GroupCategory> likeIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank())
                return null;
            return cb.like(cb.upper(root.get(field)), "%" + value.toUpperCase() + "%");
        };
    }

    private static Specification<GroupCategory> inList(String field, List<Integer> values) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty())
                return null;
            return root.get(field).in(values);
        };
    }
}
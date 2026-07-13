package com.example.paymenthub.repository.specification;

import com.example.paymenthub.entity.GroupCategory;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public class GroupCategorySpecification {

    private GroupCategorySpecification() {}

    public static Specification<GroupCategory> filter(
            String paramType,
            String paramValue,
            String paramName,
            List<Integer> statuses,
            List<Integer> isActives
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (paramType != null && !paramType.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("paramType")), "%" + paramType.trim().toLowerCase() + "%"));
            }

            if (paramValue != null && !paramValue.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("paramValue")), "%" + paramValue.trim().toLowerCase() + "%"));
            }

            if (paramName != null && !paramName.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("paramName")), "%" + paramName.trim().toLowerCase() + "%"));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicate = cb.and(predicate, root.get("status").in(statuses));
            }

            if (isActives != null && !isActives.isEmpty()) {
                predicate = cb.and(predicate, root.get("isActive").in(isActives));
            }

            return predicate;
        };
    }
}

package com.example.paymenthub.service;

import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.entity.GroupCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface GroupCategoryService {

    Page<GroupCategory> search(
            String paramType,
            String paramValue,
            String paramName,
            List<Integer> statuses,
            List<Integer> isActives,
            Pageable pageable
    );

    GroupCategory getById(Long id);

    GroupCategory create(GroupCategoryDTO dto, String username);

    GroupCategory update(Long id, GroupCategoryDTO dto, String username);

    void delete(Long id, String username);

    GroupCategory sendForApproval(Long id, String username);

    List<Map<String, Object>> getJoinedList();

    List<Map<String, Object>> getRawDataForExport();

    List<Map<String, Object>> batchApprove(List<Long> ids, String approver);

    List<Map<String, Object>> batchReject(List<Long> ids, String reason, String approver);
}

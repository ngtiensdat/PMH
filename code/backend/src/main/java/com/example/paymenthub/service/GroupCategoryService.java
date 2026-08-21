package com.example.paymenthub.service;

import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.request.GroupCategorySearchCriteria;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface GroupCategoryService {

    Page<GroupCategory> search(GroupCategorySearchCriteria criteria, Pageable pageable);

    GroupCategory getById(Long id);

    GroupCategory create(GroupCategoryDTO dto, String username);

    GroupCategory update(Long id, GroupCategoryDTO dto, String username);

    void delete(Long id, String username);

    GroupCategory sendForApproval(Long id, String username);

    GroupCategory cancelApproval(Long id, String username);

    List<Map<String, Object>> getJoinedList();

    List<Map<String, Object>> getRawDataForExport();

    List<BatchItemResultDTO> batchApprove(List<Long> ids, String approver);

    List<BatchItemResultDTO> batchReject(List<Long> ids, String reason, String approver);
}

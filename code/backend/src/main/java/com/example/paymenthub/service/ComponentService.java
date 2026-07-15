package com.example.paymenthub.service;

import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ComponentService {

    Page<ProcessingComponent> search(
            String componentCode,
            String componentName,
            String messageType,
            String connectionMethod,
            List<Integer> statuses,
            List<Integer> isActives,
            Pageable pageable
    );

    ProcessingComponent getByCode(String code);

    List<ProcessingComponent> getActiveList(Integer status);

    ProcessingComponent create(ComponentDTO dto, String username);

    ProcessingComponent update(String code, ComponentDTO dto, String username);

    void delete(String code);

    ProcessingComponent sendForApproval(String code, String username);

    List<Map<String, Object>> getRawDataForExport();

    List<Map<String, Object>> batchApprove(List<String> codes, String approver);

    List<Map<String, Object>> batchReject(List<String> codes, String reason, String approver);
}

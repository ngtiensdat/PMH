package com.example.paymenthub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentSearchCriteria {
    private String componentCode;
    private String componentName;
    private String messageType;
    private String connectionMethod;
    private List<Integer> status;
    private List<Integer> isActive;
}

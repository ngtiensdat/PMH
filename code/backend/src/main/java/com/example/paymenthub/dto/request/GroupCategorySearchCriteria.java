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
public class GroupCategorySearchCriteria {
    private String paramType;
    private String paramValue;
    private String paramName;
    private List<Integer> status;
    private List<Integer> isActive;
}

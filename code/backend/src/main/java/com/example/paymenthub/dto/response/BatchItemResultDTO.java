package com.example.paymenthub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchItemResultDTO {
    private Long id;
    private String code;
    private String status; // "SUCCESS" or "FAILED"
    private String errorMessage;
}

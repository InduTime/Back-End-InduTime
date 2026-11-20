package com.indutime.backend.service;

import com.indutime.backend.dto.OperationSummaryResponseDTO;

public record OperationTelemetryResult(
        OperationSummaryResponseDTO operation,
        boolean created) {
}

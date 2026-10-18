package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;

public class BatchBlockRequest {

    @NotBlank(message = "Reason for blocking is required")
    private String reason;

    public BatchBlockRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package com.smartagri.dto;

import com.smartagri.enums.TicketPriority;

public class CreateSupportTicketRequest {

    private Long orderId;
    private Long returnRequestId;
    private String subject;
    private String description;
    private TicketPriority priority = TicketPriority.MEDIUM;

    public CreateSupportTicketRequest() {}

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getReturnRequestId() {
        return returnRequestId;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }
}

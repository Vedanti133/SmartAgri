package com.smartagri.dto;

import com.smartagri.enums.TicketPriority;
import com.smartagri.enums.TicketStatus;

public class UpdateTicketStatusRequest {

    private TicketStatus status;
    private TicketPriority priority;
    private String assignedTo;

    public UpdateTicketStatusRequest() {}

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
}

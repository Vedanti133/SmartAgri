package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;

public class AddTrackingEventRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String eventCode;

    @NotBlank(message = "Description is required")
    private String description;

    private String location;

    public AddTrackingEventRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}

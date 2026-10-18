package com.smartagri.dto;

import java.time.LocalDateTime;

public class ShipmentTrackingEventDTO {
    private Long id;
    private Long shipmentId;
    private String status;
    private String eventCode;
    private String description;
    private String location;
    private LocalDateTime eventTime;
    private String createdBy;

    public ShipmentTrackingEventDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

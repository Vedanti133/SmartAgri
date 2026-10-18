package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_tracking_events", indexes = {
        @Index(name = "idx_ret_evt_shp", columnList = "return_shipment_id")
})
public class ReturnTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_shipment_id", nullable = false)
    private ReturnShipment returnShipment;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "event_code", nullable = false, length = 50)
    private String eventCode = "STATUS_UPDATE";

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 150)
    private String location;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public ReturnTrackingEvent() {}

    public ReturnTrackingEvent(ReturnShipment returnShipment, String status, String eventCode, String description, String location, String createdBy) {
        this.returnShipment = returnShipment;
        this.status = status;
        this.eventCode = eventCode;
        this.description = description;
        this.location = location;
        this.createdBy = createdBy;
        this.eventTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReturnShipment getReturnShipment() { return returnShipment; }
    public void setReturnShipment(ReturnShipment returnShipment) { this.returnShipment = returnShipment; }

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

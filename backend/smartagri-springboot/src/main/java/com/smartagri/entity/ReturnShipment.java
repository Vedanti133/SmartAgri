package com.smartagri.entity;

import com.smartagri.enums.ReturnShipmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_shipments", indexes = {
        @Index(name = "idx_ret_shp_status", columnList = "status"),
        @Index(name = "idx_ret_shp_num", columnList = "shipment_number", unique = true),
        @Index(name = "idx_ret_trk_num", columnList = "tracking_number", unique = true)
})
public class ReturnShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false, unique = true)
    private ReturnRequest returnRequest;

    @Column(name = "shipment_number", nullable = false, unique = true, length = 50)
    private String shipmentNumber;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 50)
    private String trackingNumber;

    @Column(length = 100)
    private String carrier;

    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnShipmentStatus status = ReturnShipmentStatus.CREATED;

    @Column(name = "pickup_scheduled_at")
    private LocalDateTime pickupScheduledAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "returnShipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnTrackingEvent> events = new ArrayList<>();

    public ReturnShipment() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addEvent(ReturnTrackingEvent event) {
        events.add(event);
        event.setReturnShipment(this);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReturnRequest getReturnRequest() { return returnRequest; }
    public void setReturnRequest(ReturnRequest returnRequest) { this.returnRequest = returnRequest; }

    public String getShipmentNumber() { return shipmentNumber; }
    public void setShipmentNumber(String shipmentNumber) { this.shipmentNumber = shipmentNumber; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Warehouse getDestinationWarehouse() { return destinationWarehouse; }
    public void setDestinationWarehouse(Warehouse destinationWarehouse) { this.destinationWarehouse = destinationWarehouse; }

    public ReturnShipmentStatus getStatus() { return status; }
    public void setStatus(ReturnShipmentStatus status) { this.status = status; }

    public LocalDateTime getPickupScheduledAt() { return pickupScheduledAt; }
    public void setPickupScheduledAt(LocalDateTime pickupScheduledAt) { this.pickupScheduledAt = pickupScheduledAt; }

    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ReturnTrackingEvent> getEvents() { return events; }
    public void setEvents(List<ReturnTrackingEvent> events) { this.events = events; }

    public List<ReturnTrackingEvent> getTrackingEvents() { return events; }
    public void setTrackingEvents(List<ReturnTrackingEvent> trackingEvents) { this.events = trackingEvents; }
}

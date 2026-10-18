package com.smartagri.entity;

import com.smartagri.enums.ItemCondition;
import com.smartagri.enums.ReturnResolution;
import jakarta.persistence.*;

@Entity
@Table(name = "return_request_items", indexes = {
        @Index(name = "idx_reti_req", columnList = "return_request_id"),
        @Index(name = "idx_reti_item", columnList = "order_item_id"),
        @Index(name = "idx_reti_fert", columnList = "fertilizer_id")
})
public class ReturnRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fertilizer_id", nullable = false)
    private Fertilizer fertilizer;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "approved_quantity", nullable = false)
    private Integer approvedQuantity = 0;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity = 0;

    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 50)
    private ItemCondition itemCondition = ItemCondition.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnResolution resolution = ReturnResolution.REFUND;

    @Column(length = 255)
    private String reason;

    public ReturnRequestItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReturnRequest getReturnRequest() { return returnRequest; }
    public void setReturnRequest(ReturnRequest returnRequest) { this.returnRequest = returnRequest; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public Fertilizer getFertilizer() { return fertilizer; }
    public void setFertilizer(Fertilizer fertilizer) { this.fertilizer = fertilizer; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public Integer getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(Integer approvedQuantity) { this.approvedQuantity = approvedQuantity; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }

    public Integer getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(Integer rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }

    public ItemCondition getItemCondition() { return itemCondition; }
    public void setItemCondition(ItemCondition itemCondition) { this.itemCondition = itemCondition; }

    public ReturnResolution getResolution() { return resolution; }
    public void setResolution(ReturnResolution resolution) { this.resolution = resolution; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

package com.smartagri.dto;

import com.smartagri.entity.StockTransfer;
import com.smartagri.entity.StockTransferStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StockTransferDTO {

    private Long id;
    private String transferNumber;

    private Long sourceWarehouseId;
    private String sourceWarehouseCode;
    private String sourceWarehouseName;

    private Long destinationWarehouseId;
    private String destinationWarehouseCode;
    private String destinationWarehouseName;

    private String requestedBy;
    private String approvedBy;
    private String dispatchedBy;
    private String receivedBy;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime receivedAt;

    private StockTransferStatus status;
    private String notes;

    private List<StockTransferItemDTO> items = new ArrayList<>();
    private Integer totalRequestedQuantity = 0;
    private Integer totalDispatchedQuantity = 0;
    private Integer totalReceivedQuantity = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StockTransferDTO() {
    }

    public static StockTransferDTO fromEntity(StockTransfer st) {
        if (st == null) return null;
        StockTransferDTO dto = new StockTransferDTO();
        dto.setId(st.getId());
        dto.setTransferNumber(st.getTransferNumber());

        if (st.getSourceWarehouse() != null) {
            dto.setSourceWarehouseId(st.getSourceWarehouse().getId());
            dto.setSourceWarehouseCode(st.getSourceWarehouse().getWarehouseCode());
            dto.setSourceWarehouseName(st.getSourceWarehouse().getWarehouseName());
        }

        if (st.getDestinationWarehouse() != null) {
            dto.setDestinationWarehouseId(st.getDestinationWarehouse().getId());
            dto.setDestinationWarehouseCode(st.getDestinationWarehouse().getWarehouseCode());
            dto.setDestinationWarehouseName(st.getDestinationWarehouse().getWarehouseName());
        }

        dto.setRequestedBy(st.getRequestedBy());
        dto.setApprovedBy(st.getApprovedBy());
        dto.setDispatchedBy(st.getDispatchedBy());
        dto.setReceivedBy(st.getReceivedBy());

        dto.setRequestedAt(st.getRequestedAt());
        dto.setApprovedAt(st.getApprovedAt());
        dto.setDispatchedAt(st.getDispatchedAt());
        dto.setReceivedAt(st.getReceivedAt());

        dto.setStatus(st.getStatus());
        dto.setNotes(st.getNotes());

        if (st.getItems() != null) {
            dto.setItems(st.getItems().stream()
                    .map(StockTransferItemDTO::fromEntity)
                    .collect(Collectors.toList()));
            int reqSum = 0;
            int dispSum = 0;
            int recSum = 0;
            for (StockTransferItemDTO i : dto.getItems()) {
                reqSum += (i.getRequestedQuantity() != null ? i.getRequestedQuantity() : 0);
                dispSum += (i.getDispatchedQuantity() != null ? i.getDispatchedQuantity() : 0);
                recSum += (i.getReceivedQuantity() != null ? i.getReceivedQuantity() : 0);
            }
            dto.setTotalRequestedQuantity(reqSum);
            dto.setTotalDispatchedQuantity(dispSum);
            dto.setTotalReceivedQuantity(recSum);
        }

        dto.setCreatedAt(st.getCreatedAt());
        dto.setUpdatedAt(st.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransferNumber() {
        return transferNumber;
    }

    public void setTransferNumber(String transferNumber) {
        this.transferNumber = transferNumber;
    }

    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(Long sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public String getSourceWarehouseCode() {
        return sourceWarehouseCode;
    }

    public void setSourceWarehouseCode(String sourceWarehouseCode) {
        this.sourceWarehouseCode = sourceWarehouseCode;
    }

    public String getSourceWarehouseName() {
        return sourceWarehouseName;
    }

    public void setSourceWarehouseName(String sourceWarehouseName) {
        this.sourceWarehouseName = sourceWarehouseName;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public String getDestinationWarehouseCode() {
        return destinationWarehouseCode;
    }

    public void setDestinationWarehouseCode(String destinationWarehouseCode) {
        this.destinationWarehouseCode = destinationWarehouseCode;
    }

    public String getDestinationWarehouseName() {
        return destinationWarehouseName;
    }

    public void setDestinationWarehouseName(String destinationWarehouseName) {
        this.destinationWarehouseName = destinationWarehouseName;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(String dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public StockTransferStatus getStatus() {
        return status;
    }

    public void setStatus(StockTransferStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<StockTransferItemDTO> getItems() {
        return items;
    }

    public void setItems(List<StockTransferItemDTO> items) {
        this.items = items;
    }

    public Integer getTotalRequestedQuantity() {
        return totalRequestedQuantity;
    }

    public void setTotalRequestedQuantity(Integer totalRequestedQuantity) {
        this.totalRequestedQuantity = totalRequestedQuantity;
    }

    public Integer getTotalDispatchedQuantity() {
        return totalDispatchedQuantity;
    }

    public void setTotalDispatchedQuantity(Integer totalDispatchedQuantity) {
        this.totalDispatchedQuantity = totalDispatchedQuantity;
    }

    public Integer getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public void setTotalReceivedQuantity(Integer totalReceivedQuantity) {
        this.totalReceivedQuantity = totalReceivedQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalRequestedUnits() {
        return totalRequestedQuantity;
    }

    public Integer getTotalDispatchedUnits() {
        return totalDispatchedQuantity;
    }

    public Integer getTotalReceivedUnits() {
        return totalReceivedQuantity;
    }
}

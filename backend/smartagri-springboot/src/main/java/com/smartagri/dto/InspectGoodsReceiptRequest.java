package com.smartagri.dto;

import com.smartagri.entity.QualityInspectionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class InspectGoodsReceiptRequest {

    private String packagingCondition;
    private String sealCondition;
    private Boolean labelVerification = true;
    private Boolean quantityVerification = true;

    @NotNull(message = "Inspection status is required")
    private QualityInspectionStatus inspectionStatus;

    private String inspectionNotes;

    @Valid
    private List<ItemInspectionBreakdown> itemInspections;

    public InspectGoodsReceiptRequest() {}

    public static class ItemInspectionBreakdown {
        @NotNull(message = "Receipt item ID is required")
        private Long itemId;

        @NotNull(message = "Accepted quantity is required")
        private Integer acceptedQuantity;

        @NotNull(message = "Rejected quantity is required")
        private Integer rejectedQuantity;

        private String rejectionReason;

        public ItemInspectionBreakdown() {}

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getAcceptedQuantity() {
            return acceptedQuantity;
        }

        public void setAcceptedQuantity(Integer acceptedQuantity) {
            this.acceptedQuantity = acceptedQuantity;
        }

        public Integer getRejectedQuantity() {
            return rejectedQuantity;
        }

        public void setRejectedQuantity(Integer rejectedQuantity) {
            this.rejectedQuantity = rejectedQuantity;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }
    }

    public String getPackagingCondition() {
        return packagingCondition;
    }

    public void setPackagingCondition(String packagingCondition) {
        this.packagingCondition = packagingCondition;
    }

    public String getSealCondition() {
        return sealCondition;
    }

    public void setSealCondition(String sealCondition) {
        this.sealCondition = sealCondition;
    }

    public Boolean getLabelVerification() {
        return labelVerification;
    }

    public void setLabelVerification(Boolean labelVerification) {
        this.labelVerification = labelVerification;
    }

    public Boolean getQuantityVerification() {
        return quantityVerification;
    }

    public void setQuantityVerification(Boolean quantityVerification) {
        this.quantityVerification = quantityVerification;
    }

    public QualityInspectionStatus getInspectionStatus() {
        return inspectionStatus;
    }

    public void setInspectionStatus(QualityInspectionStatus inspectionStatus) {
        this.inspectionStatus = inspectionStatus;
    }

    public String getInspectionNotes() {
        return inspectionNotes;
    }

    public void setInspectionNotes(String inspectionNotes) {
        this.inspectionNotes = inspectionNotes;
    }

    public List<ItemInspectionBreakdown> getItemInspections() {
        return itemInspections;
    }

    public void setItemInspections(List<ItemInspectionBreakdown> itemInspections) {
        this.itemInspections = itemInspections;
    }
}

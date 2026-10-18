package com.smartagri.dto;

import com.smartagri.enums.InspectionResult;
import com.smartagri.enums.ItemCondition;
import java.time.LocalDateTime;

public class ReturnInspectionDTO {

    private Long id;
    private Long returnRequestId;
    private String inspectedBy;
    private LocalDateTime inspectionDate;
    private ItemCondition itemCondition;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;
    private Integer quarantinedQuantity;
    private String inspectionNotes;
    private InspectionResult result;
    private Long quarantineLocationId;
    private String quarantineLocationCode;

    public ReturnInspectionDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReturnRequestId() {
        return returnRequestId;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public String getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(String inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public LocalDateTime getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDateTime inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public ItemCondition getItemCondition() {
        return itemCondition;
    }

    public void setItemCondition(ItemCondition itemCondition) {
        this.itemCondition = itemCondition;
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

    public Integer getQuarantinedQuantity() {
        return quarantinedQuantity;
    }

    public void setQuarantinedQuantity(Integer quarantinedQuantity) {
        this.quarantinedQuantity = quarantinedQuantity;
    }

    public String getInspectionNotes() {
        return inspectionNotes;
    }

    public void setInspectionNotes(String inspectionNotes) {
        this.inspectionNotes = inspectionNotes;
    }

    public InspectionResult getResult() {
        return result;
    }

    public void setResult(InspectionResult result) {
        this.result = result;
    }

    public Long getQuarantineLocationId() {
        return quarantineLocationId;
    }

    public void setQuarantineLocationId(Long quarantineLocationId) {
        this.quarantineLocationId = quarantineLocationId;
    }

    public String getQuarantineLocationCode() {
        return quarantineLocationCode;
    }

    public void setQuarantineLocationCode(String quarantineLocationCode) {
        this.quarantineLocationCode = quarantineLocationCode;
    }
}

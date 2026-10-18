package com.smartagri.dto;

import com.smartagri.enums.InspectionResult;
import com.smartagri.enums.ItemCondition;

import java.util.Map;

public class InspectReturnRequest {

    private String inspectedBy;
    private String inspectionNotes;
    private ItemCondition itemCondition = ItemCondition.UNOPENED;
    private InspectionResult result = InspectionResult.ACCEPTED;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity = 0;
    private Integer quarantinedQuantity = 0;
    private Long quarantineLocationId;
    private Long acceptedLocationId;
    // Map of itemId -> acceptedQuantity (optional if single item or aggregate used)
    private Map<Long, Integer> acceptedQuantities;
    private Map<Long, Integer> rejectedQuantities;

    public InspectReturnRequest() {}

    public String getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(String inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public String getInspectionNotes() {
        return inspectionNotes;
    }

    public void setInspectionNotes(String inspectionNotes) {
        this.inspectionNotes = inspectionNotes;
    }

    public ItemCondition getItemCondition() {
        return itemCondition;
    }

    public void setItemCondition(ItemCondition itemCondition) {
        this.itemCondition = itemCondition;
    }

    public InspectionResult getResult() {
        return result;
    }

    public void setResult(InspectionResult result) {
        this.result = result;
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

    public Long getQuarantineLocationId() {
        return quarantineLocationId;
    }

    public void setQuarantineLocationId(Long quarantineLocationId) {
        this.quarantineLocationId = quarantineLocationId;
    }

    public Long getAcceptedLocationId() {
        return acceptedLocationId;
    }

    public void setAcceptedLocationId(Long acceptedLocationId) {
        this.acceptedLocationId = acceptedLocationId;
    }

    public Map<Long, Integer> getAcceptedQuantities() {
        return acceptedQuantities;
    }

    public void setAcceptedQuantities(Map<Long, Integer> acceptedQuantities) {
        this.acceptedQuantities = acceptedQuantities;
    }

    public Map<Long, Integer> getRejectedQuantities() {
        return rejectedQuantities;
    }

    public void setRejectedQuantities(Map<Long, Integer> rejectedQuantities) {
        this.rejectedQuantities = rejectedQuantities;
    }
}

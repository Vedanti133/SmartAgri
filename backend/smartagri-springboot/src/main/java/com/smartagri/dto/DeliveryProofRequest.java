package com.smartagri.dto;

public class DeliveryProofRequest {
    private String receiverName;
    private String receiverPhone;
    private String notes;

    public DeliveryProofRequest() {}

    public DeliveryProofRequest(String receiverName, String notes) {
        this.receiverName = receiverName;
        this.notes = notes;
    }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    private String proofType;
    private String otpEntered;

    public String getProofType() { return proofType; }
    public void setProofType(String proofType) { this.proofType = proofType; }

    public String getOtpEntered() { return otpEntered; }
    public void setOtpEntered(String otpEntered) { this.otpEntered = otpEntered; }

    public String getReceivedBy() { return receiverName; }
    public void setReceivedBy(String receivedBy) { this.receiverName = receivedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

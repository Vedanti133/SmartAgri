package com.smartagri.dto;

public class BatchSummaryDTO {

    private long totalBatches;
    private long activeBatches;
    private long nearExpiryBatches;
    private long expiredBatches;
    private long depletedBatches;
    private long blockedBatches;
    private long totalBatchUnits;
    private long nearExpiryUnits;
    private long expiredUnits;

    public BatchSummaryDTO() {
    }

    public BatchSummaryDTO(long totalBatches, long activeBatches, long nearExpiryBatches, long expiredBatches,
                           long depletedBatches, long blockedBatches, long totalBatchUnits,
                           long nearExpiryUnits, long expiredUnits) {
        this.totalBatches = totalBatches;
        this.activeBatches = activeBatches;
        this.nearExpiryBatches = nearExpiryBatches;
        this.expiredBatches = expiredBatches;
        this.depletedBatches = depletedBatches;
        this.blockedBatches = blockedBatches;
        this.totalBatchUnits = totalBatchUnits;
        this.nearExpiryUnits = nearExpiryUnits;
        this.expiredUnits = expiredUnits;
    }

    public long getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(long totalBatches) {
        this.totalBatches = totalBatches;
    }

    public long getActiveBatches() {
        return activeBatches;
    }

    public void setActiveBatches(long activeBatches) {
        this.activeBatches = activeBatches;
    }

    public long getNearExpiryBatches() {
        return nearExpiryBatches;
    }

    public void setNearExpiryBatches(long nearExpiryBatches) {
        this.nearExpiryBatches = nearExpiryBatches;
    }

    public long getExpiredBatches() {
        return expiredBatches;
    }

    public void setExpiredBatches(long expiredBatches) {
        this.expiredBatches = expiredBatches;
    }

    public long getDepletedBatches() {
        return depletedBatches;
    }

    public void setDepletedBatches(long depletedBatches) {
        this.depletedBatches = depletedBatches;
    }

    public long getBlockedBatches() {
        return blockedBatches;
    }

    public void setBlockedBatches(long blockedBatches) {
        this.blockedBatches = blockedBatches;
    }

    public long getTotalBatchUnits() {
        return totalBatchUnits;
    }

    public void setTotalBatchUnits(long totalBatchUnits) {
        this.totalBatchUnits = totalBatchUnits;
    }

    public long getNearExpiryUnits() {
        return nearExpiryUnits;
    }

    public void setNearExpiryUnits(long nearExpiryUnits) {
        this.nearExpiryUnits = nearExpiryUnits;
    }

    public long getExpiredUnits() {
        return expiredUnits;
    }

    public void setExpiredUnits(long expiredUnits) {
        this.expiredUnits = expiredUnits;
    }
}

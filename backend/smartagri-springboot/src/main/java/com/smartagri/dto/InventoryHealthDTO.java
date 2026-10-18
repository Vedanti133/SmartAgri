package com.smartagri.dto;

import java.util.ArrayList;
import java.util.List;

public class InventoryHealthDTO {

    private String status; // HEALTHY or ISSUES_DETECTED
    private long productsChecked;
    private long negativeStockCount;
    private long lowStockCount;
    private long outOfStockCount;
    private long totalStockUnits;
    private long totalTransactions;
    private long batchesChecked;
    private long batchIssuesCount;
    private long goodsReceiptsChecked;
    private long procurementIssuesCount;
    private long warehousesChecked;
    private long locationsChecked;
    private long batchLocationRecordsChecked;
    private long warehouseIssuesCount;
    private long transferIssuesCount;
    private long shipmentsChecked;
    private long fulfillmentIssuesCount;
    private int issuesFound;
    private List<String> issuesList = new ArrayList<>();

    public InventoryHealthDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getProductsChecked() {
        return productsChecked;
    }

    public void setProductsChecked(long productsChecked) {
        this.productsChecked = productsChecked;
    }

    public long getNegativeStockCount() {
        return negativeStockCount;
    }

    public void setNegativeStockCount(long negativeStockCount) {
        this.negativeStockCount = negativeStockCount;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public long getTotalStockUnits() {
        return totalStockUnits;
    }

    public void setTotalStockUnits(long totalStockUnits) {
        this.totalStockUnits = totalStockUnits;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getBatchesChecked() {
        return batchesChecked;
    }

    public void setBatchesChecked(long batchesChecked) {
        this.batchesChecked = batchesChecked;
    }

    public long getBatchIssuesCount() {
        return batchIssuesCount;
    }

    public void setBatchIssuesCount(long batchIssuesCount) {
        this.batchIssuesCount = batchIssuesCount;
    }

    public long getGoodsReceiptsChecked() {
        return goodsReceiptsChecked;
    }

    public void setGoodsReceiptsChecked(long goodsReceiptsChecked) {
        this.goodsReceiptsChecked = goodsReceiptsChecked;
    }

    public long getProcurementIssuesCount() {
        return procurementIssuesCount;
    }

    public void setProcurementIssuesCount(long procurementIssuesCount) {
        this.procurementIssuesCount = procurementIssuesCount;
    }

    public long getWarehousesChecked() {
        return warehousesChecked;
    }

    public void setWarehousesChecked(long warehousesChecked) {
        this.warehousesChecked = warehousesChecked;
    }

    public long getLocationsChecked() {
        return locationsChecked;
    }

    public void setLocationsChecked(long locationsChecked) {
        this.locationsChecked = locationsChecked;
    }

    public long getBatchLocationRecordsChecked() {
        return batchLocationRecordsChecked;
    }

    public void setBatchLocationRecordsChecked(long batchLocationRecordsChecked) {
        this.batchLocationRecordsChecked = batchLocationRecordsChecked;
    }

    public long getWarehouseIssuesCount() {
        return warehouseIssuesCount;
    }

    public void setWarehouseIssuesCount(long warehouseIssuesCount) {
        this.warehouseIssuesCount = warehouseIssuesCount;
    }

    public long getTransferIssuesCount() {
        return transferIssuesCount;
    }

    public void setTransferIssuesCount(long transferIssuesCount) {
        this.transferIssuesCount = transferIssuesCount;
    }

    public long getShipmentsChecked() {
        return shipmentsChecked;
    }

    public void setShipmentsChecked(long shipmentsChecked) {
        this.shipmentsChecked = shipmentsChecked;
    }

    public long getFulfillmentIssuesCount() {
        return fulfillmentIssuesCount;
    }

    public void setFulfillmentIssuesCount(long fulfillmentIssuesCount) {
        this.fulfillmentIssuesCount = fulfillmentIssuesCount;
    }

    public long getTotalShipments() {
        return shipmentsChecked;
    }

    public boolean isFulfillmentInvariantHealthy() {
        return fulfillmentIssuesCount == 0;
    }

    public int getIssuesFound() {
        return issuesFound;
    }

    public void setIssuesFound(int issuesFound) {
        this.issuesFound = issuesFound;
    }

    public List<String> getIssuesList() {
        return issuesList;
    }

    public void setIssuesList(List<String> issuesList) {
        this.issuesList = issuesList;
    }
}

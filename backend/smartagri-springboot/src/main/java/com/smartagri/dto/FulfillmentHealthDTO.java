package com.smartagri.dto;

import java.util.ArrayList;
import java.util.List;

public class FulfillmentHealthDTO {
    private String status;
    private long ordersChecked;
    private long shipmentsChecked;
    private long packagesChecked;
    private long allocationsChecked;
    private int issuesCount;
    private List<String> issuesList = new ArrayList<>();

    public FulfillmentHealthDTO() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getOrdersChecked() { return ordersChecked; }
    public void setOrdersChecked(long ordersChecked) { this.ordersChecked = ordersChecked; }

    public long getShipmentsChecked() { return shipmentsChecked; }
    public void setShipmentsChecked(long shipmentsChecked) { this.shipmentsChecked = shipmentsChecked; }

    public long getPackagesChecked() { return packagesChecked; }
    public void setPackagesChecked(long packagesChecked) { this.packagesChecked = packagesChecked; }

    public long getAllocationsChecked() { return allocationsChecked; }
    public void setAllocationsChecked(long allocationsChecked) { this.allocationsChecked = allocationsChecked; }

    public int getIssuesCount() { return issuesCount; }
    public void setIssuesCount(int issuesCount) { this.issuesCount = issuesCount; }

    public List<String> getIssuesList() { return issuesList; }
    public void setIssuesList(List<String> issuesList) { this.issuesList = issuesList; }

    public Boolean getInvariantsPassed() {
        return issuesCount == 0;
    }

    public void setInvariantsPassed(Boolean invariantsPassed) {
        // dynamic based on issuesCount == 0
    }
}

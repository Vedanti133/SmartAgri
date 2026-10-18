package com.smartagri.service;

import com.smartagri.dto.ProcurementDashboardDTO;
import com.smartagri.entity.GoodsReceiptStatus;
import com.smartagri.entity.PurchaseOrderStatus;
import com.smartagri.entity.SupplierStatus;
import com.smartagri.repository.GoodsReceiptRepository;
import com.smartagri.repository.PurchaseOrderRepository;
import com.smartagri.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ProcurementDashboardService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;

    public ProcurementDashboardService(SupplierRepository supplierRepository,
                                       PurchaseOrderRepository purchaseOrderRepository,
                                       GoodsReceiptRepository goodsReceiptRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
    }

    @Transactional(readOnly = true)
    public ProcurementDashboardDTO getDashboardMetrics() {
        ProcurementDashboardDTO dto = new ProcurementDashboardDTO();

        dto.setTotalSuppliers(supplierRepository.count());
        dto.setActiveSuppliers(supplierRepository.countByStatus(SupplierStatus.ACTIVE));

        long draft = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.DRAFT);
        long pending = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        long approved = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.APPROVED);
        long sent = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.SENT_TO_SUPPLIER);
        long partial = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        long full = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.FULLY_RECEIVED);

        dto.setDraftPOs(draft);
        dto.setPendingApprovalPOs(pending);
        dto.setOpenPurchaseOrders(approved + sent + partial);
        dto.setPartiallyReceivedPOs(partial);
        dto.setFullyReceivedPOs(full);

        long grnDraft = goodsReceiptRepository.countByStatus(GoodsReceiptStatus.DRAFT);
        long grnInspect = goodsReceiptRepository.countByStatus(GoodsReceiptStatus.PENDING_INSPECTION);
        dto.setPendingGRNs(grnDraft + grnInspect);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal monthVal = purchaseOrderRepository.calculatePurchaseValueSince(startOfMonth);
        dto.setThisMonthPurchaseValue(monthVal != null ? monthVal : BigDecimal.ZERO);

        return dto;
    }
}

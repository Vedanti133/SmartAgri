package com.smartagri.service;

import com.smartagri.dto.ProcessRefundRequest;
import com.smartagri.dto.RefundDTO;
import com.smartagri.entity.Order;
import com.smartagri.entity.Refund;
import com.smartagri.entity.ReturnRequest;
import com.smartagri.enums.RefundStatus;
import com.smartagri.enums.ReturnStatus;
import com.smartagri.repository.RefundRepository;
import com.smartagri.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RefundService {

    private final RefundRepository refundRepository;
    private final ReturnRequestRepository returnRequestRepository;

    public RefundService(RefundRepository refundRepository,
                         ReturnRequestRepository returnRequestRepository) {
        this.refundRepository = refundRepository;
        this.returnRequestRepository = returnRequestRepository;
    }

    public RefundDTO processRefund(Long refundId, ProcessRefundRequest request, String processedBy) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        if (refund.getStatus() == RefundStatus.PROCESSED) {
            throw new IllegalStateException("Refund #" + refund.getRefundNumber() + " is already processed");
        }

        // Apply bank details if passed in request
        if (request != null && request.getBankAccountDetails() != null && !request.getBankAccountDetails().trim().isEmpty()) {
            refund.setBankAccountDetails(request.getBankAccountDetails().trim());
        }

        // If payment method is COD, verify bank details are present
        if ("COD".equalsIgnoreCase(refund.getPaymentMethod()) &&
                (refund.getBankAccountDetails() == null || refund.getBankAccountDetails().trim().isEmpty())) {
            refund.setStatus(RefundStatus.PENDING_CUSTOMER_REFUND_DETAILS);
            refundRepository.save(refund);
            throw new IllegalStateException("Bank account details are required to process a COD refund");
        }

        // Enforce invariant: total refunds for this order must not exceed order grand total
        Order order = refund.getOrder();
        if (order != null) {
            BigDecimal processedSum = refundRepository.sumAmountByOrderIdAndStatusIn(
                    order.getId(), List.of(RefundStatus.PROCESSED));
            BigDecimal newTotal = processedSum.add(refund.getAmount());
            if (newTotal.compareTo(order.getGrandTotal()) > 0) {
                throw new IllegalArgumentException("Total refunded amount (" + newTotal +
                        ") cannot exceed order grand total (" + order.getGrandTotal() + ")");
            }
        }

        if (request != null && request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getAmount().compareTo(refund.getAmount()) <= 0) {
                refund.setAmount(request.getAmount());
            } else {
                throw new IllegalArgumentException("Refund amount cannot exceed calculated refundable amount: " + refund.getAmount());
            }
        }

        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setProcessedBy(processedBy != null ? processedBy : "SYSTEM");
        if (request != null && request.getPaymentReference() != null && !request.getPaymentReference().isEmpty()) {
            refund.setPaymentReference(request.getPaymentReference());
        } else if (refund.getPaymentReference() == null) {
            refund.setPaymentReference("REF-TXN-" + System.currentTimeMillis());
        }
        if (request != null && request.getNotes() != null) {
            refund.setNotes(request.getNotes());
        }
        refund = refundRepository.save(refund);

        // Update parent ReturnRequest to REFUNDED
        if (refund.getReturnRequest() != null) {
            ReturnRequest ret = refund.getReturnRequest();
            ret.setStatus(ReturnStatus.REFUNDED);
            ret.setCompletedAt(LocalDateTime.now());
            returnRequestRepository.save(ret);
        }

        return mapToDTO(refund);
    }

    public RefundDTO updateBankDetails(Long customerId, Long refundId, String bankAccountDetails) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        if (refund.getOrder() != null && refund.getOrder().getUser() != null) {
            if (!refund.getOrder().getUser().getId().equals(customerId)) {
                throw new SecurityException("Access denied: You do not own this order/refund");
            }
        }

        if (bankAccountDetails == null || bankAccountDetails.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank account details cannot be empty");
        }

        refund.setBankAccountDetails(bankAccountDetails.trim());
        if (refund.getStatus() == RefundStatus.PENDING_CUSTOMER_REFUND_DETAILS) {
            refund.setStatus(RefundStatus.PENDING);
        }
        refund.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(refundRepository.save(refund));
    }

    public RefundDTO retryRefund(Long refundId, String processedBy) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        if (refund.getStatus() != RefundStatus.FAILED && refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Only FAILED or PENDING refunds can be retried");
        }

        return processRefund(refundId, null, processedBy);
    }

    @Transactional(readOnly = true)
    public RefundDTO getRefundById(Long id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + id));
        return mapToDTO(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundDTO> getRefundsByOrderId(Long orderId) {
        return refundRepository.findByOrderId(orderId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RefundDTO> getAllRefunds(RefundStatus status) {
        List<Refund> list = (status != null) ? refundRepository.findByStatus(status) : refundRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public RefundDTO mapToDTO(Refund refund) {
        RefundDTO dto = new RefundDTO();
        dto.setId(refund.getId());
        dto.setRefundNumber(refund.getRefundNumber());
        if (refund.getReturnRequest() != null) {
            dto.setReturnRequestId(refund.getReturnRequest().getId());
        }
        if (refund.getOrder() != null) {
            dto.setOrderId(refund.getOrder().getId());
        }
        if (refund.getPayment() != null) {
            dto.setPaymentId(refund.getPayment().getId());
        }
        dto.setAmount(refund.getAmount());
        dto.setCurrency(refund.getCurrency());
        dto.setStatus(refund.getStatus());
        dto.setReason(refund.getReason());
        dto.setPaymentMethod(refund.getPaymentMethod());
        dto.setPaymentReference(refund.getPaymentReference());
        dto.setBankAccountDetails(refund.getBankAccountDetails());
        dto.setRequestedAt(refund.getRequestedAt());
        dto.setProcessedAt(refund.getProcessedAt());
        dto.setProcessedBy(refund.getProcessedBy());
        dto.setFailureReason(refund.getFailureReason());
        dto.setNotes(refund.getNotes());
        return dto;
    }
}

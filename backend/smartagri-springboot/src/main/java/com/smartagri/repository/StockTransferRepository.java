package com.smartagri.repository;

import com.smartagri.entity.StockTransfer;
import com.smartagri.entity.StockTransferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    Optional<StockTransfer> findByTransferNumber(String transferNumber);

    boolean existsByTransferNumber(String transferNumber);

    List<StockTransfer> findByStatus(StockTransferStatus status);

    List<StockTransfer> findBySourceWarehouseIdOrDestinationWarehouseId(Long sourceId, Long destId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM StockTransfer st WHERE st.id = :id")
    Optional<StockTransfer> findByIdWithLock(@Param("id") Long id);
}

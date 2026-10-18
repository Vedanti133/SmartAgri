package com.smartagri.repository;

import com.smartagri.entity.PickList;
import com.smartagri.enums.PickListStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickListRepository extends JpaRepository<PickList, Long> {

    List<PickList> findByOrderId(Long orderId);

    Optional<PickList> findByPickListNumber(String pickListNumber);

    List<PickList> findByWarehouseId(Long warehouseId);

    List<PickList> findByStatus(PickListStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pl FROM PickList pl WHERE pl.id = :id")
    Optional<PickList> findByIdWithLock(@Param("id") Long id);
}

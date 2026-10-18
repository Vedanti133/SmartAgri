package com.smartagri.repository;

import com.smartagri.entity.ReturnInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReturnInspectionRepository extends JpaRepository<ReturnInspection, Long> {

    Optional<ReturnInspection> findByReturnRequestId(Long returnRequestId);
}

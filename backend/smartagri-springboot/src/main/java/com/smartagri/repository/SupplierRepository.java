package com.smartagri.repository;

import com.smartagri.entity.Supplier;
import com.smartagri.entity.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierCode(String supplierCode);

    boolean existsBySupplierCode(String supplierCode);

    List<SupplierStatus> findByStatus(SupplierStatus status);

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY s.companyName ASC")
    List<Supplier> searchSuppliers(@Param("status") SupplierStatus status, @Param("search") String search);

    long countByStatus(SupplierStatus status);
}

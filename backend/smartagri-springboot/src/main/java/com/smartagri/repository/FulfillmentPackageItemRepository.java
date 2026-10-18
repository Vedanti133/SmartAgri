package com.smartagri.repository;

import com.smartagri.entity.FulfillmentPackageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FulfillmentPackageItemRepository extends JpaRepository<FulfillmentPackageItem, Long> {
    List<FulfillmentPackageItem> findByFulfillmentPackageId(Long packageId);
}

package com.smartagri.repository;

import com.smartagri.entity.PickListItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickListItemRepository extends JpaRepository<PickListItem, Long> {

    List<PickListItem> findByPickListId(Long pickListId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pli FROM PickListItem pli WHERE pli.id = :id")
    Optional<PickListItem> findByIdWithLock(@Param("id") Long id);
}

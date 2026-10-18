package com.smartagri.repository;

import com.smartagri.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByWishlistIdAndFertilizerId(Long wishlistId, Long fertilizerId);

    Optional<WishlistItem> findByIdAndWishlistUserId(Long id, Long userId);

    Optional<WishlistItem> findByFertilizerIdAndWishlistUserId(Long fertilizerId, Long userId);

    boolean existsByWishlistUserIdAndFertilizerId(Long userId, Long fertilizerId);

    List<WishlistItem> findByWishlistUserId(Long userId);
}

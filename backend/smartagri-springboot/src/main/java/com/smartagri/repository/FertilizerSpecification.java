package com.smartagri.repository;

import com.smartagri.entity.Category;
import com.smartagri.entity.Fertilizer;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FertilizerSpecification {

    public static Specification<Fertilizer> build(
            String q,
            Long categoryId,
            String categoryName,
            String brand,
            String fertilizerType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String stockStatus,
            String npk) {

        return (Root<Fertilizer> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // Always show only active products in storefront search
            predicates.add(cb.isTrue(root.get("active")));

            // Search Query 'q': match against name, brand, sku, description, composition, suitableCrops, type, npkRatio, category.name
            // Splits multi-word phrases (e.g. "IFFCO Urea", "NPK 20-20-20") so each token matches across fields
            if (q != null && !q.trim().isBlank()) {
                String cleanQ = q.trim().toLowerCase(Locale.ROOT);
                String[] tokens = cleanQ.split("\\s+");
                Join<Fertilizer, Category> catJoin = root.join("category", JoinType.LEFT);

                List<Predicate> tokenPredicates = new ArrayList<>();
                for (String token : tokens) {
                    if (token.isBlank()) continue;
                    String t = "%" + token + "%";
                    String altT = t.contains("-") ? t.replace("-", ":") : (t.contains(":") ? t.replace(":", "-") : t);

                    Predicate nameMatch = cb.or(cb.like(cb.lower(root.get("name")), t), cb.like(cb.lower(root.get("name")), altT));
                    Predicate brandMatch = cb.like(cb.lower(root.get("brand")), t);
                    Predicate skuMatch = cb.like(cb.lower(root.get("sku")), t);
                    Predicate descMatch = cb.like(cb.lower(root.get("description")), t);
                    Predicate compMatch = cb.or(cb.like(cb.lower(root.get("composition")), t), cb.like(cb.lower(root.get("composition")), altT));
                    Predicate cropsMatch = cb.like(cb.lower(root.get("suitableCrops")), t);
                    Predicate typeMatch = cb.like(cb.lower(root.get("type")), t);
                    Predicate npkMatch = cb.or(cb.like(cb.lower(root.get("npkRatio")), t), cb.like(cb.lower(root.get("npkRatio")), altT));
                    Predicate catMatch = cb.like(cb.lower(catJoin.get("name")), t);

                    tokenPredicates.add(cb.or(nameMatch, brandMatch, skuMatch, descMatch, compMatch, cropsMatch, typeMatch, npkMatch, catMatch));
                }

                if (!tokenPredicates.isEmpty()) {
                    predicates.add(cb.and(tokenPredicates.toArray(new Predicate[0])));
                }
            }

            // Category Filter
            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            } else if (categoryName != null && !categoryName.trim().isBlank() && !"ALL".equalsIgnoreCase(categoryName.trim())) {
                String cTerm = categoryName.trim().toLowerCase(Locale.ROOT);
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("category").get("name")), cTerm),
                        cb.like(cb.lower(root.get("category").get("name")), "%" + cTerm + "%"),
                        cb.like(cb.literal(cTerm), cb.concat("%", cb.concat(cb.lower(root.get("category").get("name")), "%")))
                ));
            }

            // Brand Filter
            if (brand != null && !brand.trim().isBlank() && !"ALL".equalsIgnoreCase(brand.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
            }

            // Fertilizer Type Filter
            if (fertilizerType != null && !fertilizerType.trim().isBlank() && !"ALL".equalsIgnoreCase(fertilizerType.trim())) {
                String tTerm = fertilizerType.trim().toLowerCase(Locale.ROOT);
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("type")), tTerm),
                        cb.like(cb.lower(root.get("type")), "%" + tTerm + "%"),
                        cb.like(cb.literal(tTerm), cb.concat("%", cb.concat(cb.lower(root.get("type")), "%")))
                ));
            }

            // Price Minimum
            if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }

            // Price Maximum
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }

            // Stock Availability Filter
            if (stockStatus != null && !stockStatus.trim().isBlank() && !"ALL".equalsIgnoreCase(stockStatus.trim())) {
                String st = stockStatus.trim().toUpperCase(Locale.ROOT);
                if ("IN_STOCK".equals(st)) {
                    predicates.add(cb.greaterThan(root.get("stock"), 0));
                } else if ("OUT_OF_STOCK".equals(st)) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("stock"), 0));
                } else if ("LOW_STOCK".equals(st)) {
                    predicates.add(cb.and(
                            cb.greaterThan(root.get("stock"), 0),
                            cb.or(
                                    cb.lessThanOrEqualTo(root.get("stock"), root.get("minimumStock")),
                                    cb.lessThanOrEqualTo(root.get("stock"), 10)
                            )
                    ));
                }
            }

            // NPK Ratio Filter
            if (npk != null && !npk.trim().isBlank() && !"ALL".equalsIgnoreCase(npk.trim())) {
                // Normalize NPK string so "20-20-20" or "20:20:20" matches
                String cleanNpk = npk.trim().replace("-", ":").toLowerCase(Locale.ROOT);
                String pattern1 = "%" + cleanNpk + "%";
                String pattern2 = "%" + npk.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate p1 = cb.like(cb.lower(root.get("npkRatio")), pattern1);
                Predicate p2 = cb.like(cb.lower(root.get("name")), pattern1);
                Predicate p3 = cb.like(cb.lower(root.get("name")), pattern2);
                Predicate p4 = cb.like(cb.lower(root.get("composition")), pattern1);
                predicates.add(cb.or(p1, p2, p3, p4));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

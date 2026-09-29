package com.futureboundtech.repository;

import com.futureboundtech.entity.FaqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqItemRepository extends JpaRepository<FaqItem, Long> {

    /** Published items in display order, for the public FAQ page. */
    List<FaqItem> findByPublishedTrueOrderBySortOrderAscCreatedAtAsc();

    /** Every item in display order, for the admin console. */
    List<FaqItem> findAllByOrderBySortOrderAscCreatedAtAsc();
}

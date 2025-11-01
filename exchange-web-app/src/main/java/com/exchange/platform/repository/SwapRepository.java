package com.exchange.platform.repository;

import com.exchange.platform.entity.Swap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwapRepository extends JpaRepository<Swap, Long> {
    // Explicit JPQL to avoid Spring Data property parsing issues with leading single-letter camel-case fields
    @Query("select s from Swap s where s.aUserId = :userA or s.bUserId = :userB")
    Page<Swap> findByAUserIdOrBUserId(@Param("userA") Long aUserId, @Param("userB") Long bUserId, Pageable pageable);
}

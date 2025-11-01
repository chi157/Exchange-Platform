package com.exchange.platform.repository;

import com.exchange.platform.entity.Swap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwapRepository extends JpaRepository<Swap, Long> {

    Optional<Swap> findByProposalId(Long proposalId);

    List<Swap> findByStatus(Swap.SwapStatus status);

    @Query("SELECT s FROM Swap s WHERE s.userA.id = :userId OR s.userB.id = :userId")
    List<Swap> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Swap s WHERE (s.userA.id = :userId OR s.userB.id = :userId) AND s.status = :status")
    List<Swap> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Swap.SwapStatus status);
}
package com.exchange.platform.repository;

import com.exchange.platform.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findBySwapId(Long swapId);

    List<Dispute> findByStatus(Dispute.DisputeStatus status);

    List<Dispute> findByComplainantId(Long complainantId);

    @Query("SELECT d FROM Dispute d WHERE d.admin.id = :adminId")
    List<Dispute> findByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT d FROM Dispute d WHERE d.status IN ('OPEN', 'IN_REVIEW')")
    List<Dispute> findOpenDisputes();
}
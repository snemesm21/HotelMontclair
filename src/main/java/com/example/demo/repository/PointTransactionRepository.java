package com.example.demo.repository;

import com.example.demo.entities.PointTransaction;
import com.example.demo.entities.PointTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<PointTransaction> findByClientIdAndTypeOrderByCreatedAtDesc(Long clientId, PointTransactionType type);

    boolean existsByClientIdAndReferenceTypeAndReferenceId(Long clientId, String referenceType, Long referenceId);
}

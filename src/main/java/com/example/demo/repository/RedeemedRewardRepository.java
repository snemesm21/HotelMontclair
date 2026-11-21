package com.example.demo.repository;

import com.example.demo.entities.RedeemedReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedeemedRewardRepository extends JpaRepository<RedeemedReward, Long> {

    List<RedeemedReward> findByClientIdOrderByRedeemedAtDesc(Long clientId);

    List<RedeemedReward> findAllByOrderByRedeemedAtDesc();

    Optional<RedeemedReward> findByRedemptionCodeIgnoreCase(String redemptionCode);

    long countByClientId(Long clientId);
}

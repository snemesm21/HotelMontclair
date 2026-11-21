package com.example.demo.repository;

import com.example.demo.entities.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByActiveTrueOrderByPointsCostAsc();

    List<Reward> findAllByOrderByPointsCostAsc();

    List<Reward> findByActiveTrueAndCategoryIgnoreCaseOrderByPointsCostAsc(String category);
}

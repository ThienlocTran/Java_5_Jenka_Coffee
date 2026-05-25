package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.FeedbackStatus;
import com.springboot.jenka_coffee.entity.StoreFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreFeedbackRepository extends JpaRepository<StoreFeedback, Long> {

    @Query("""
            select f from StoreFeedback f
            where (:branch is null or f.branch = :branch)
              and (:status is null or f.status = :status)
            order by f.createdAt desc
            """)
    Page<StoreFeedback> search(
            @Param("branch") String branch,
            @Param("status") FeedbackStatus status,
            Pageable pageable
    );

    @Query("""
            select f from StoreFeedback f
            where f.status = :status
            order by coalesce(f.approvedAt, f.createdAt) desc, f.createdAt desc
            """)
    List<StoreFeedback> findPublicByStatus(
            @Param("status") FeedbackStatus status,
            Pageable pageable
    );
}

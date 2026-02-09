package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Integer> {

    /**
     * Find all available news (for site display)
     */
    List<News> findByAvailableTrueOrderByCreateDateDesc();

    /**
     * Find all available news with pagination
     */
    Page<News> findByAvailableTrueOrderByCreateDateDesc(Pageable pageable);

    /**
     * Find all news with pagination (for admin)
     */
    Page<News> findAllByOrderByCreateDateDesc(Pageable pageable);
}

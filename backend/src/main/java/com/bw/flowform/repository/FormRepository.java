package com.bw.flowform.repository;

import com.bw.flowform.entity.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {
    Page<Form> findAllByOrderByIdDesc(Pageable pageable);

    List<Form> findAllByOrderByIdDesc();

    @Query("SELECT f FROM Form f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY f.id DESC")
    Page<Form> findByNameContaining(@Param("name") String name, Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    java.util.Optional<Form> findByName(String name);
}

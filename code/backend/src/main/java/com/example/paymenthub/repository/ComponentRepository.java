package com.example.paymenthub.repository;

import com.example.paymenthub.entity.ProcessingComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentRepository extends JpaRepository<ProcessingComponent, String>, JpaSpecificationExecutor<ProcessingComponent> {

    List<ProcessingComponent> findAllByIsActiveOrderByComponentCodeAsc(int isActive);

    List<ProcessingComponent> findAllByIsActiveAndStatusOrderByComponentCodeAsc(int isActive, int status);

    boolean existsByComponentCode(String componentCode);
}

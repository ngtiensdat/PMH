package com.example.paymenthub.repository;

import com.example.paymenthub.entity.ProcessingComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentRepository extends JpaRepository<ProcessingComponent, String>, JpaSpecificationExecutor<ProcessingComponent> {

    List<ProcessingComponent> findAllByIsActiveOrderByComponentNameAsc(int isActive);

    List<ProcessingComponent> findAllByIsActiveAndStatusOrderByComponentNameAsc(int isActive, int status);

    boolean existsByComponentCode(String componentCode);
}

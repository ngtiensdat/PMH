package com.example.paymenthub.repository;

import com.example.paymenthub.entity.ProcessingComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComponentRepository extends JpaRepository<ProcessingComponent, String>, JpaSpecificationExecutor<ProcessingComponent> {

    List<ProcessingComponent> findAllByIsActiveOrderByComponentCodeAsc(int isActive);

    List<ProcessingComponent> findAllByIsActiveAndStatusOrderByComponentCodeAsc(int isActive, int status);

    boolean existsByComponentCode(String componentCode);

    /**
     * Lấy danh sách component đang hoạt động và còn trong thời gian hiệu lực.
     * Đẩy filter ngày xuống DB thay vì lọc trong Java memory.
     */
    @Query("SELECT c FROM ProcessingComponent c WHERE c.isActive = :isActive " +
           "AND c.effectiveDate IS NOT NULL AND c.effectiveDate <= :now " +
           "AND (c.endEffectiveDate IS NULL OR c.endEffectiveDate >= :now) " +
           "ORDER BY c.componentCode ASC")
    List<ProcessingComponent> findActiveInRange(@Param("isActive") int isActive,
                                               @Param("now") LocalDateTime now);

    /**
     * Lấy danh sách component đang hoạt động, lọc thêm theo status, còn trong thời gian hiệu lực.
     */
    @Query("SELECT c FROM ProcessingComponent c WHERE c.isActive = :isActive AND c.status = :status " +
           "AND c.effectiveDate IS NOT NULL AND c.effectiveDate <= :now " +
           "AND (c.endEffectiveDate IS NULL OR c.endEffectiveDate >= :now) " +
           "ORDER BY c.componentCode ASC")
    List<ProcessingComponent> findActiveWithStatusInRange(@Param("isActive") int isActive,
                                                         @Param("status") int status,
                                                         @Param("now") LocalDateTime now);
}

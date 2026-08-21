package com.example.paymenthub.repository;

import com.example.paymenthub.entity.GroupCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCategoryRepository
        extends JpaRepository<GroupCategory, Long>, JpaSpecificationExecutor<GroupCategory> {

    boolean existsByParamNameAndParamValueAndParamType(String paramName, String paramValue, String paramType);

    boolean existsByParamNameAndParamValueAndParamTypeAndIdNot(String paramName, String paramValue, String paramType,
            Long id);

    java.util.List<GroupCategory> findByIsActiveOrderByParamTypeAscParamNameAsc(Integer isActive);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(g) > 0 FROM GroupCategory g WHERE g.paramName = :paramName "
            +
            "AND g.paramType = :paramType " +
            "AND (:id IS NULL OR g.id <> :id) " +
            "AND ( " +
            "  (g.endEffectiveDate IS NULL AND :endEffectiveDate IS NULL) OR " +
            "  (g.endEffectiveDate IS NULL AND :endEffectiveDate IS NOT NULL AND g.effectiveDate <= :endEffectiveDate) OR "
            +
            "  (:endEffectiveDate IS NULL AND g.endEffectiveDate IS NOT NULL AND :effectiveDate <= g.endEffectiveDate) OR "
            +
            "  (g.endEffectiveDate IS NOT NULL AND :endEffectiveDate IS NOT NULL AND g.effectiveDate <= :endEffectiveDate AND :effectiveDate <= g.endEffectiveDate) "
            +
            ")")
    boolean existsOverlapping(
            @Param("paramName") String paramName,
            @Param("paramType") String paramType,
            @Param("effectiveDate") java.time.LocalDateTime effectiveDate,
            @Param("endEffectiveDate") java.time.LocalDateTime endEffectiveDate,
            @Param("id") Long id);

    // Gọi Procedure bằng annotation @Procedure (đại diện cho dạng 3)
    @Procedure(name = "PROC_APPROVE_GROUP_CATEGORY")
    void approveGroupCategory(
            @Param("p_id") Long id,
            @Param("p_user") String user,
            @Param("p_status") Long[] status,
            @Param("p_message") String[] message);
}

package com.example.paymenthub.repository;

import com.example.paymenthub.entity.GroupCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupCategoryRepository
        extends JpaRepository<GroupCategory, Long>, JpaSpecificationExecutor<GroupCategory> {

    List<GroupCategory> findByIsActiveOrderByParamTypeAscParamNameAsc(Integer isActive);

    /**
     * Kiểm tra xem có tồn tại bản ghi khác (khác id) có cùng Tên + Nhóm bị chồng lấn thời gian hiệu lực không.
     * Loại bỏ 2 derived-query methods cũ (existsByParamNameAndParamValueAndParamType...) vì không được dùng.
     */
    @Query("SELECT COUNT(g) > 0 FROM GroupCategory g WHERE g.paramName = :paramName "
            + "AND g.paramType = :paramType "
            + "AND (:id IS NULL OR g.id <> :id) "
            + "AND ( "
            + "  (g.endEffectiveDate IS NULL AND :endEffectiveDate IS NULL) OR "
            + "  (g.endEffectiveDate IS NULL AND :endEffectiveDate IS NOT NULL AND g.effectiveDate <= :endEffectiveDate) OR "
            + "  (:endEffectiveDate IS NULL AND g.endEffectiveDate IS NOT NULL AND :effectiveDate <= g.endEffectiveDate) OR "
            + "  (g.endEffectiveDate IS NOT NULL AND :endEffectiveDate IS NOT NULL AND g.effectiveDate <= :endEffectiveDate AND :effectiveDate <= g.endEffectiveDate) "
            + ")")
    boolean existsOverlapping(
            @Param("paramName") String paramName,
            @Param("paramType") String paramType,
            @Param("effectiveDate") LocalDateTime effectiveDate,
            @Param("endEffectiveDate") LocalDateTime endEffectiveDate,
            @Param("id") Long id);
}

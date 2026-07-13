package com.example.paymenthub.repository;

import com.example.paymenthub.entity.GroupCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCategoryRepository extends JpaRepository<GroupCategory, Long>, JpaSpecificationExecutor<GroupCategory> {

    boolean existsByParamNameAndParamValueAndParamType(String paramName, String paramValue, String paramType);

    boolean existsByParamNameAndParamValueAndParamTypeAndIdNot(String paramName, String paramValue, String paramType, Long id);

    // Gọi Procedure bằng annotation @Procedure (đại diện cho dạng 3)
    @Procedure(name = "PROC_APPROVE_GROUP_CATEGORY")
    void approveGroupCategory(
        @Param("p_id") Long id,
        @Param("p_user") String user,
        @Param("p_status") Long[] status,
        @Param("p_message") String[] message
    );
}

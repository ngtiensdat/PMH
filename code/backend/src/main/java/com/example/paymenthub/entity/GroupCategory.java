package com.example.paymenthub.entity;

import com.example.paymenthub.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "PMH_GROUP_CATEGORY", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"PARAM_NAME", "PARAM_VALUE", "PARAM_TYPE"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GroupCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PARAM_NAME", nullable = false, length = 255)
    private String paramName;

    @Column(name = "PARAM_VALUE", nullable = false, length = 255)
    private String paramValue;

    @Column(name = "PARAM_TYPE", nullable = false, length = 255)
    private String paramType;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "COMPONENT_CODE", nullable = false, length = 255)
    private String componentCode;
}

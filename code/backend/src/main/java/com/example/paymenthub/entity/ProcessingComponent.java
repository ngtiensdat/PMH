package com.example.paymenthub.entity;

import com.example.paymenthub.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "PMH_COMPONENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProcessingComponent extends BaseEntity {

    @Id
    @Column(name = "COMPONENT_CODE", nullable = false, length = 50)
    private String componentCode;

    @Column(name = "COMPONENT_NAME", nullable = false, length = 255)
    private String componentName;

    @Column(name = "MESSAGE_TYPE", length = 100)
    private String messageType;

    @Column(name = "CONNECTION_METHOD", length = 100)
    private String connectionMethod;

    @Column(name = "CHECK_TOKEN", length = 1)
    private String checkToken; // Y: Có kiểm tra token, N: Không

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (checkToken == null) checkToken = "N";
    }
}

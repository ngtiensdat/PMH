package com.example.paymenthub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PMH_PERMISSIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PERMISSION_CODE", nullable = false, unique = true, length = 100)
    private String permissionCode;

    @Column(name = "PERMISSION_NAME", nullable = false, length = 255)
    private String permissionName;

    @Column(name = "MODULE", nullable = false, length = 50)
    private String module;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;
}

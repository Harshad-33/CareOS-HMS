package com.careos.hms.permission.entity;

import com.careos.hms.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "module_name", nullable = false, length = 64)
    private String moduleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PermissionScope scope;

    @Column(name = "description", length = 255)
    private String description;

    protected Permission() {
    }

    public Permission(String code, String name, String moduleName, PermissionScope scope, String description) {
        this.code = code;
        this.name = name;
        this.moduleName = moduleName;
        this.scope = scope;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public PermissionScope getScope() {
        return scope;
    }

    public String getDescription() {
        return description;
    }
}

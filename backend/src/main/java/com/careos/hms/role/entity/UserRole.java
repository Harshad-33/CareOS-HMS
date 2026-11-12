package com.careos.hms.role.entity;

import com.careos.hms.common.audit.BaseEntity;
import com.careos.hms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role_id"})
)
public class UserRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    protected UserRole() {
    }

    public UserRole(User user, Role role, String assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy != null && !assignedBy.isBlank() ? assignedBy : "SYSTEM";
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public String getAssignedBy() {
        return assignedBy;
    }
}

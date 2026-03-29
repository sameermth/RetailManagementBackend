package com.retailmanagement.modules.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "default_branch_id")
    private Long defaultBranchId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "employee_code")
    private String employeeCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id", insertable = false, updatable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumns({
            @JoinColumn(name = "organization_id", referencedColumnName = "organization_id", insertable = false, updatable = false),
            @JoinColumn(name = "person_id", referencedColumnName = "person_id", insertable = false, updatable = false)
    })
    private OrganizationPersonProfile organizationPersonProfile;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<UserBranchAccess> branchAccesses;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = this.createdAt == null ? now : this.createdAt;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getUsername() {
        return account != null ? account.getLoginIdentifier() : null;
    }

    public void setUsername(String username) {
        if (account != null) {
            account.setLoginIdentifier(username);
        }
        this.employeeCode = username;
    }

    public String getFirstName() {
        String[] parts = splitFullName(resolveFullName());
        return parts[0];
    }

    public void setFirstName(String firstName) {
        updateDisplayName(firstName, getLastName());
    }

    public String getLastName() {
        String[] parts = splitFullName(resolveFullName());
        return parts[1];
    }

    public void setLastName(String lastName) {
        updateDisplayName(getFirstName(), lastName);
    }

    public Set<Role> getRoles() {
        if (role == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(Set.of(role));
    }

    public void setRoles(Set<Role> roles) {
        this.role = (roles == null || roles.isEmpty()) ? null : roles.iterator().next();
    }

    public String getPassword() {
        return account != null ? account.getPasswordHash() : null;
    }

    public String getEmail() {
        if (organizationPersonProfile != null && organizationPersonProfile.getEmailForOrg() != null
                && !organizationPersonProfile.getEmailForOrg().isBlank()) {
            return organizationPersonProfile.getEmailForOrg();
        }
        return person != null ? person.getPrimaryEmail() : null;
    }

    public String getPhone() {
        if (organizationPersonProfile != null && organizationPersonProfile.getPhoneForOrg() != null
                && !organizationPersonProfile.getPhoneForOrg().isBlank()) {
            return organizationPersonProfile.getPhoneForOrg();
        }
        return person != null ? person.getPrimaryPhone() : null;
    }

    public String getDisplayName() {
        return resolveFullName();
    }

    private String[] splitFullName(String nameValue) {
        if (nameValue == null || nameValue.isBlank()) {
            return new String[]{"", ""};
        }
        String trimmed = nameValue.trim();
        int idx = trimmed.indexOf(' ');
        if (idx < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1).trim()};
    }

    private String mergeName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String merged = (first + " " + last).trim();
        return merged.isEmpty() ? null : merged;
    }

    private void updateDisplayName(String firstName, String lastName) {
        String merged = mergeName(firstName, lastName);
        if (organizationPersonProfile != null) {
            organizationPersonProfile.setDisplayName(merged);
        }
        if (person != null) {
            person.setLegalName(merged);
        }
    }

    private String resolveFullName() {
        if (organizationPersonProfile != null && organizationPersonProfile.getDisplayName() != null
                && !organizationPersonProfile.getDisplayName().isBlank()) {
            return organizationPersonProfile.getDisplayName();
        }
        if (person != null && person.getLegalName() != null && !person.getLegalName().isBlank()) {
            return person.getLegalName();
        }
        return null;
    }
}

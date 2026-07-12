package project.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // НОВОЕ ПОЛЕ: Полное имя пользователя
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // НОВОЕ ПОЛЕ: Email пользователя
    @Column(unique = true)
    private String email;

    // НОВОЕ ПОЛЕ: Дата создания пользователя
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // НОВОЕ ПОЛЕ: Дата последнего обновления
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles;

    @Column(nullable = false)
    private boolean enabled = true;

    // НОВОЕ ПОЛЕ: Причина деактивации (если применимо)
    @Column(name = "deactivation_reason")
    private String deactivationReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* UserDetails */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // НОВЫЕ МЕТОДЫ: Для удобства
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public String getDisplayName() {
        return fullName != null && !fullName.trim().isEmpty() ? fullName : username;
    }
}
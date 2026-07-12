package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.model.Role;
import project.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    long countByEnabled(boolean enabled);

    @Query("SELECT u FROM User u WHERE u.enabled = true ORDER BY u.fullName")
    List<User> findActiveUsers();

    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:name% OR u.username LIKE %:name% OR u.email LIKE %:name%")
    List<User> searchUsers(@Param("name") String name);

    @Query("SELECT COUNT(u) FROM User u WHERE :role MEMBER OF u.roles")
    long countByRole(@Param("role") Role role);
}
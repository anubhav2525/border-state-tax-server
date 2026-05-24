package com.bst.server.modules.authentication.repositories;

import com.bst.server.modules.authentication.data.entities.Roles;
import com.bst.server.modules.authentication.data.entities.Users;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * JPA Specifications for dynamic User filtering.
 *
 * Usage in service layer:
 * <pre>
 *   Specification<User> spec = UserSpecification.isActive()
 *       .and(UserSpecification.hasRole("AGENT"))
 *       .and(UserSpecification.phoneContains("98"));
 *
 *   Page<User> page = userRepository.findAll(spec, pageable);
 * </pre>
 *
 * UserRepository must extend JpaSpecificationExecutor<User>.
 */
public class UserSpecification {

    private UserSpecification() {}

    public static Specification<Users> isActive() {
        return (root, query, cb) ->
                cb.isTrue(root.get("isActive"));
    }

    public static Specification<Users> isInactive() {
        return (root, query, cb) ->
                cb.isFalse(root.get("isActive"));
    }

    public static Specification<Users> isPhoneVerified() {
        return (root, query, cb) ->
                cb.isTrue(root.get("isPhoneVerified"));
    }

    public static Specification<Users> isEmailVerified() {
        return (root, query, cb) ->
                cb.isTrue(root.get("isEmailVerified"));
    }

    public static Specification<Users> isLocked() {
        return (root, query, cb) ->
                cb.greaterThan(root.get("lockedUntil"), LocalDateTime.now());
    }

    /**
     * Case-insensitive partial match on full_name, phone, or email.
     */
    public static Specification<Users> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("phone")),    pattern),
                    cb.like(cb.lower(root.get("email")),    pattern)
            );
        };
    }

    public static Specification<Users> phoneContains(String partial) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(partial)) return cb.conjunction();
            return cb.like(root.get("phone"), "%" + partial + "%");
        };
    }

    public static Specification<Users> emailContains(String partial) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(partial)) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")),
                    "%" + partial.toLowerCase() + "%");
        };
    }

    /**
     * Filters users who have at least one role with the given name.
     * Uses a subquery to avoid duplicating rows on the outer result.
     */
    public static Specification<Users> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(roleName)) return cb.conjunction();
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Users> subUser = sub.correlate(root);
            Join<Users, Roles> roleJoin = subUser.join("roles");
            sub.select(cb.literal(1L))
                    .where(cb.equal(roleJoin.get("name"), roleName));
            return cb.exists(sub);
        };
    }

    public static Specification<Users> createdAfter(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Users> createdBefore(LocalDateTime to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Users> lastLoginAfter(LocalDateTime since) {
        return (root, query, cb) ->
                since == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("lastLoginAt"), since);
    }

    public static Specification<Users> neverLoggedIn() {
        return (root, query, cb) ->
                cb.isNull(root.get("lastLoginAt"));
    }
}

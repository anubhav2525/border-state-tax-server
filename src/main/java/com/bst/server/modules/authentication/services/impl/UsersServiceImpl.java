package com.bst.server.modules.authentication.services.impl;

import com.bst.server.common.exceptions.sub.*;
import com.bst.server.common.utils.*;
import com.bst.server.modules.authentication.data.dtos.*;
import com.bst.server.modules.authentication.data.entities.Roles;
import com.bst.server.modules.authentication.data.entities.Users;
import com.bst.server.modules.authentication.repositories.RoleRepository;
import com.bst.server.modules.authentication.repositories.UserRepository;
import com.bst.server.modules.authentication.security.JwtService;
import com.bst.server.modules.authentication.services.UsersService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {
    private static final String RESOURCE = "User";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("fullName", "email", "phone", "createdAt", "updatedAt", "enabled");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsersResponse usersResponse;
    private final UserRolesService userRolesService;
    private final JwtService jwtService;
    private final CreateResponseEntity createResponseEntity;
    private final BuildPageable buildPageable;
    private final StringOperation stringOperation;

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<UsersResponse.Auth>> login(
            UsersRequest.Login request, WebRequest webRequest) {
        Users user = userRepository.findSecurityUserByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotExistsException(RESOURCE + " not found"));

        if (!Boolean.TRUE.equals(user.getEnabled()) || Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResourceOperationNotAllowed("User account is disabled");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ResourceOperationNotAllowed("User account is locked until " + user.getLockedUntil());
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            userRepository.incrementFailedLoginAttempts(user.getId(), LocalDateTime.now());
            int attempts = user.getFailedLoginAttempts() == null ? 1 : user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                userRepository.lockAccount(user.getId(), LocalDateTime.now().plusMinutes(LOCK_MINUTES), LocalDateTime.now());
            }
            throw new ResourceValidationException("Invalid email or password");
        }

        userRepository.unlockAndRecordLogin(user.getId(), LocalDateTime.now());
        Users freshUser = userRepository.findByIdWithRolesAndPermissions(user.getId()).orElse(user);
        UsersResponse.Auth auth = buildAuth(freshUser);
        saveRefreshToken(freshUser, auth.getRefreshToken());

        return createResponseEntity.buildResponse("Login successful", auth, HttpStatus.OK, webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<UsersResponse.Auth>> refresh(
            UsersRequest.RefreshToken request, WebRequest webRequest) {
        String tokenHash = jwtService.hashToken(request.getRefreshToken());
        Users user = userRepository.findByValidRefreshToken(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new ResourceValidationException("Invalid refresh token"));

        if (!jwtService.isTokenValid(request.getRefreshToken(), user.getEmail())) {
            throw new ResourceValidationException("Invalid refresh token");
        }

        Users freshUser = userRepository.findByIdWithRolesAndPermissions(user.getId()).orElse(user);
        UsersResponse.Auth auth = buildAuth(freshUser);
        saveRefreshToken(freshUser, auth.getRefreshToken());
        return createResponseEntity.buildResponse("Token refreshed successfully", auth, HttpStatus.OK, webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> create(
            UsersRequest.Create request, WebRequest webRequest) {
        Users user = createUserEntity(request, request.getRoleIds().isEmpty() ? null : request.getRoleIds());

        userRepository.save(user);
        return createResponseEntity.buildResponse(
                RESOURCE + " created successfully",
                usersResponse.toDetail(user),
                HttpStatus.CREATED,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> getById(UUID id, WebRequest webRequest) {
        Users users = getUser(id);

        return createResponseEntity.buildResponse(
                RESOURCE + " fetched successfully",
                usersResponse.toDetail(users),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> update(UUID id, UsersRequest.Update request, WebRequest webRequest) {
        Users user = getUser(id);

        String fullName = stringOperation.trimOrNull(request.getFullName());

        if (request.getEmail() != null) {
            String email = stringOperation.trimOrNull(request.getEmail());
            if (email != null) {
                userRepository.findByEmailAndDeletedFalse(email)
                        .filter(existing -> !existing.getId().equals(id))
                        .ifPresent(existing -> {
                            throw new ResourceAlreadyExistsException(RESOURCE + " already exists with email: " + email);
                        });
            }
            user.setEmail(email);
        }
        if (request.getPhone() != null) {
            String phone = stringOperation.trimOrNull(request.getPhone());
            userRepository.findByPhoneAndDeletedFalse(phone)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResourceAlreadyExistsException(RESOURCE + " already exists with phone: " + phone);
                    });
            user.setPhone(phone);
        }
        if (request.getFullName() != null)
            user.setFullName(fullName);
        if (request.getRoleIds() != null)
            user.setRoles(userRolesService.resolveRoles(request.getRoleIds()));

        userRepository.save(user);

        return createResponseEntity.buildResponse(
                RESOURCE + " updated successfully",
                usersResponse.toDetail(user),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<PagedResponse<UsersResponse.Summary>>> search(UsersRequest.Search request, WebRequest webRequest) {
        Pageable pageable = buildPageable.build(
                request.getPage(), request.getSize(),
                request.getSortBy(), request.getSortDir(),
                ALLOWED_SORT_FIELDS, "createdAt");
        Page<Users> page = userRepository.findAll(buildSearchSpec(request), pageable);
        return createResponseEntity.buildResponse(
                RESOURCE + " list fetched successfully",
                PagedResponse.of(page.map(usersResponse::toSummary)),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> enable(UUID id, WebRequest webRequest) {
        Users user = getUser(id);
        if (Boolean.TRUE.equals(user.getEnabled()))
            throw new ResourceAlreadyEnabledException(RESOURCE + " is already enabled");

        user.setEnabled(true);

        return createResponseEntity.buildResponse(
                RESOURCE + " enabled successfully",
                usersResponse.toDetail(userRepository.save(user)),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> disable(UUID id, WebRequest webRequest) {
        Users user = getUser(id);
        if (Boolean.FALSE.equals(user.getEnabled()))
            throw new ResourceAlreadyDisabledException(RESOURCE + " is already disabled");

        user.setEnabled(false);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);

        return createResponseEntity.buildResponse(
                RESOURCE + " disabled successfully",
                usersResponse.toDetail(userRepository.save(user)),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<Void>> softDelete(UUID id, WebRequest webRequest) {
        Users user = getUser(id);

        user.setDeleted(true);
        user.setEnabled(false);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);

        userRepository.save(user);
        return createResponseEntity.buildResponse(
                RESOURCE + " deleted successfully",
                HttpStatus.OK,
                webRequest);
    }

    private Users getUser(UUID id) {
        return userRepository.findByIdWithRolesAndPermissions(id)
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> new ResourceNotExistsException(RESOURCE + " not found with id: " + id));
    }

    private UsersResponse.Auth buildAuth(Users user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return UsersResponse.Auth.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(usersResponse.toDetail(user))
                .build();
    }

    private void saveRefreshToken(Users user, String refreshToken) {
        userRepository.saveRefreshToken(
                user.getId(),
                jwtService.hashToken(refreshToken),
                LocalDateTime.ofInstant(jwtService.refreshTokenExpiry(), ZoneId.systemDefault()),
                LocalDateTime.now()
        );
    }

    private Set<Roles> resolveRolesOrDefault(Set<UUID> roleIds) {
        if (roleIds != null && !roleIds.isEmpty())
            return userRolesService.resolveRoles(roleIds);
        return roleRepository.findByNameAndDeletedFalse("USER")
                .map(role -> new HashSet<>(Set.of(role)))
                .orElseGet(HashSet::new);
    }

    private Specification<Users> buildSearchSpec(UsersRequest.Search request) {
        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (request.getEnabled() != null) predicates.add(cb.equal(root.get("enabled"), request.getEnabled()));
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(root.get("phone"), "%" + request.getKeyword().trim() + "%")
                ));
            }
            if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
                Join<Users, Roles> roleJoin = root.join("roles");
                predicates.add(cb.equal(roleJoin.get("name"), request.getRoleName().trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Users createUserEntity(UsersRequest.Create request, Set<UUID> roleIds) {
        String email = stringOperation.trimOrNull(request.getEmail());
        String phone = stringOperation.trimOrNull(request.getPhone());
        String fullName = stringOperation.trimOrNull(request.getFullName());

        if (email != null && userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new ResourceAlreadyExistsException(RESOURCE + " already exists with email: " + email);
        }

        return Users.builder()
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .deleted(false)
                .roles(resolveRolesOrDefault(roleIds))
                .build();
    }

}

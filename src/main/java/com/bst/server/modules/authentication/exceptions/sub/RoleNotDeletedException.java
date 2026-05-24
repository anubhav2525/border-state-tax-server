package com.bst.server.modules.authentication.exceptions.sub;

import lombok.Getter;

import java.util.UUID;

/**
 * Thrown when restore() is invoked on a Role that is NOT in a soft-deleted
 * state — i.e., the role is still active. There is nothing to restore.
 */
@Getter
public class RoleNotDeletedException extends RuntimeException {

    private final UUID roleId;

    public RoleNotDeletedException(UUID roleId) {
        super(String.format(
                "Role with ID '%s' is not in a deleted state and cannot be restored.", roleId));
        this.roleId = roleId;
    }

}
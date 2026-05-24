package com.bst.server.modules.authentication.exceptions.sub;

import lombok.Getter;

import java.util.Set;
import java.util.UUID;

// Thrown when deleting a role that still has active users assigned
@Getter
public class RoleInUseException extends RuntimeException {

  private final Set<UUID> roleIds;
  private final long userCount;

  /** Single-role variant — carries exact user count for a clearer error message */
  public RoleInUseException(UUID roleId, long userCount) {
    super(String.format(
            "Role with ID '%s' is assigned to %d user(s) and cannot be deleted. "
                    + "Revoke the role from all users before deleting.", roleId, userCount));
    this.roleIds = Set.of(roleId);
    this.userCount = userCount;
  }

  /** Bulk variant — lists all offending role IDs */
  public RoleInUseException(Set<UUID> roleIds) {
    super(String.format(
            "The following roles are still assigned to users and cannot be deleted: %s. "
                    + "Revoke each role from all users before deleting.", roleIds));
    this.roleIds = Set.copyOf(roleIds);
    this.userCount = -1; // indeterminate in bulk context
  }

}

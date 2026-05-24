package com.bst.server.common.exceptions.sub;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ─────────────────────────────────────────────────────────────────────────────
// . RESTORE ERROR — 400
//    Thrown when:
//      - Trying to restore a record that is NOT soft-deleted
//      - Restore would violate uniqueness constraint
// ───────────────────────────────────────
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResourceNotRestoreException extends RuntimeException {
    public ResourceNotRestoreException(String message) {
        super(message);
    }
}

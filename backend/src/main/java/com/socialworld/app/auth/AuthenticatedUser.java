package com.socialworld.app.auth;

import java.util.UUID;

/** Lightweight principal stored in the security context; controllers read the user id from here. */
public record AuthenticatedUser(UUID id, String username) {
}

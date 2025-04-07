package com.petconnect.backend.user.application.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents the authentication response data.
 * Contains the username, a response message, a JWT token, and a status flag.
 *
 * @param username the username of the authenticated user
 * @param message the response message regarding the authentication process
 * @param jwt the JSON Web Token issued upon successful authentication
 * @param status the status indicating the success (true) or failure (false) of the authentication
 *
 * author: ibosquet
 */
@JsonPropertyOrder({"username", "message", "jwt", "status"})
public record AuthResponseDto(
       String username,
       String message,
       String jwt,
       boolean status
) {
}

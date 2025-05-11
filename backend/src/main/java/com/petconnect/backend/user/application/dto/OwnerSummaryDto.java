package com.petconnect.backend.user.application.dto;

/**
 * A summary DTO for representing basic information about a Pet Owner.
 * Typically used in scenarios requiring quick user references, such as lists or overviews.
 *
 * @param id The unique identifier of the owner.
 * @param username The owner's username.
 * @param email The owner's email address.
 * @param phone The owner's contact phone number.
 *
 * @author ibosquet
 *
 */
public record OwnerSummaryDto(Long id, String username, String email, String phone) {
}

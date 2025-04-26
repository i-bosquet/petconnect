package com.petconnect.backend.certificate.port.in.web;

import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API interface defining endpoints for managing digital Certificates.
 * Documented using OpenAPI 3 annotations.
 * Requires appropriate authentication (Vet for generation, Owner/Staff for retrieval).
 *
 * @author ibosquet
 */
@Tag(name = "\uD83D\uDCDC Certificate Management", description = "Endpoints for generating and retrieving digital pet certificates.")
@RequestMapping("/api/certificates")
@SecurityRequirement(name = "bearerAuth")
public interface CertificateControllerApi {
    /**
     * Generates a new digital certificate based on a specified medical record.
     * Requires the requesting user to be an authenticated Veterinarian authorized
     * for the source record and associated clinic.
     *
     * @param requestDto DTO containing the source record ID and the official certificate number.
     * @return ResponseEntity with the created CertificateViewDto and status 201 (Created).
     */
    @Operation(summary = "Generate New Certificate (Vet)",
            description = "Allows an authorized Vet to generate a digital certificate based on a signed medical record (typically VACCINE type).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Certificate generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CertificateViewDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Input Data (e.g., missing fields, invalid certificate number format, record unsuitable)",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a Vet or not authorized for the record/clinic)",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Source Record or related entity (Pet, Clinic) not found",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (Certificate number already exists, or certificate already exists for this record)",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error (e.g., cryptographic operation failed)",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("")
    ResponseEntity<CertificateViewDto> generateCertificate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details needed for certificate generation (Pet ID and official Cert Number).", required = true,
                    content = @Content(schema = @Schema(implementation = CertificateGenerationRequestDto.class)))
            @Valid @RequestBody CertificateGenerationRequestDto requestDto);

    /**
     * Retrieves all certificates issued for a specific pet.
     * Requires the requester to be the owner of the pet or authorized clinic staff.
     *
     * @param petId The ID of the pet whose certificates are requested.
     * @return ResponseEntity with a List of CertificateViewDto objects and status 200 (OK).
     */
    @Operation(summary = "List Certificates by Pet ID",
            description = "Retrieves all certificates issued for a specific pet. Requires Owner or associated Staff authorization.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificates retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CertificateViewDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized for this pet)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("")
    ResponseEntity<List<CertificateViewDto>> findCertificatesByPet(
            @Parameter(description = "ID of the pet whose certificates to retrieve", required = true)
            @RequestParam Long petId);

    /**
     * Retrieves the details of a specific certificate by its ID.
     * Requires the requester to be the owner of the associated pet or authorized clinic staff.
     *
     * @param certificateId The ID of the certificate to retrieve.
     * @return ResponseEntity with the CertificateViewDto and status 200 (OK).
     */
    @Operation(summary = "Get Certificate by ID",
            description = "Retrieves details of a specific certificate. Requires Owner or associated Staff authorization for the related pet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificate retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CertificateViewDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized for the associated pet)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{certificateId}")
    ResponseEntity<CertificateViewDto> findCertificateById(
            @Parameter(description = "ID of the certificate to retrieve", required = true)
            @PathVariable Long certificateId);

    /**
     * Retrieves the data string encoded in Base45 (after CBOR, COSE, ZLib processing)
     * for a specific certificate, ready to be embedded in a QR code.
     * Requires the requester to be the owner of the associated pet or authorized clinic staff.
     *
     * @param certificateId The ID of the certificate for which to generate QR data.
     * @return ResponseEntity with the Base45 encoded String and status 200 (OK).
     *         The response content type is 'text/plain'.
     */
    @Operation(summary = "Get QR Code Data (Base45)",
            description = "Retrieves the final Base45 encoded string for a certificate, suitable for QR code generation. Requires Owner or associated Staff authorization.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR data string retrieved successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "HC1:6BFOXN*TS0BI$ZD4EX/.."))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized for the associated pet)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error (e.g., failed to generate QR data)", content = @Content(schema = @Schema(implementation = Map.class))) // Puede haber errores en QrCodeService
    })
    @GetMapping(value = "/{certificateId}/qr-data", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> getCertificateQrData(
            @Parameter(description = "ID of the certificate", required = true)
            @PathVariable Long certificateId);
}

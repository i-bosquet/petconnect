package com.petconnect.backend.certificate.port.in.web;

import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.service.CertificateService;
import com.petconnect.backend.common.helper.UserHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * Implementation of {@link CertificateControllerApi}.
 * Handles incoming HTTP requests for certificate management and delegates to {@link CertificateService}.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@Slf4j
public class CertificateController implements CertificateControllerApi {

    private final CertificateService certificateService;
    private final UserHelper userHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("")
    public ResponseEntity<CertificateViewDto> generateCertificate(
            @Valid @RequestBody CertificateGenerationRequestDto requestDto) {
        Long generatingVetId = userHelper.getAuthenticatedUserId();
        log.info("Received request to generate certificate for Pet ID {} from Vet ID: {}", requestDto.petId(), generatingVetId);
        CertificateViewDto createdCertificate = certificateService.generateCertificate(requestDto, generatingVetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCertificate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("")
    public ResponseEntity<List<CertificateViewDto>> findCertificatesByPet(
            @RequestParam Long petId) {
        Long requesterUserId = userHelper.getAuthenticatedUserId();
        log.debug("Received request to find certificates for Pet ID: {} by User ID: {}", petId, requesterUserId);
        List<CertificateViewDto> certificates = certificateService.findCertificatesByPet(petId, requesterUserId);
        return ResponseEntity.ok(certificates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{certificateId}")
    public ResponseEntity<CertificateViewDto> findCertificateById(
            @PathVariable Long certificateId) {
        Long requesterUserId = userHelper.getAuthenticatedUserId();
        log.debug("Received request to find certificate ID: {} by User ID: {}", certificateId, requesterUserId);
        CertificateViewDto certificate = certificateService.findCertificateById(certificateId, requesterUserId);
        return ResponseEntity.ok(certificate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping(value = "/{certificateId}/qr-data", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getCertificateQrData(@PathVariable Long certificateId) {
        Long requesterUserId = userHelper.getAuthenticatedUserId();
        log.debug("Received request for QR data string for certificate ID: {} by User ID: {}", certificateId, requesterUserId);
        String qrData = certificateService.getQrDataForCertificate(certificateId, requesterUserId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(qrData);
    }
}

package com.petconnect.backend.notification.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.util.IntegrationTestUtils;
import com.petconnect.backend.pet.application.dto.PetActivationDto;
import com.petconnect.backend.pet.application.dto.PetRegistrationDto;
import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.VaccineCreateDto;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.petconnect.backend.util.IntegrationTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying that domain events are correctly published to Kafka
 * when relevant service methods are invoked via API calls. Uses an embedded Kafka broker.
 *
 * @author ibosquet
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = { "listeners=PLAINTEXT://localhost:9093", "port=9093" },
        topics = {
                "${kafka.topic.pet-activation-requests}",
                "${kafka.topic.pet-activated}",
                "${kafka.topic.certificate-requests}",
                "${kafka.topic.certificate-generated}"
        }
)
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaEventPublicationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private EntityManager entityManager;

    private String ownerToken;
    private String vetToken;
    private Long ownerId;
    private Long vetId;
    private Long petIdPending;
    private Long petIdActive;
    private final String testCertNumber = "KAFKA-TEST-CERT-" + System.currentTimeMillis();

    private Consumer<String, Object> consumer;

    @Value("${kafka.topic.pet-activation-requests}") private String petActivationRequestsTopicName;
    @Value("${kafka.topic.pet-activated}") private String petActivatedTopicName;
    @Value("${kafka.topic.certificate-requests}") private String certificateRequestsTopicName;
    @Value("${kafka.topic.certificate-generated}") private String certificateGeneratedTopicName;


    @BeforeEach
    void setUp() throws Exception {
        Long clinicId = 1L;
        String ownerUsername = "kafka_pub_owner_" + System.currentTimeMillis();
        String vetUsername = "kafka_pub_vet_" + System.currentTimeMillis();
        userRepository.findByUsername(ownerUsername).ifPresent(userRepository::delete);
        userRepository.findByUsername(vetUsername).ifPresent(userRepository::delete);
        entityManager.flush();

        String adminToken = IntegrationTestUtils.obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_london", "password123"));

        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(ownerUsername, ownerUsername+"@test.com", "password123", "kfkowner");
        MvcResult ownerRegResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isCreated()).andReturn();
        OwnerProfileDto ownerDto = objectMapper.readValue(ownerRegResult.getResponse().getContentAsString(), OwnerProfileDto.class);
        ownerId = ownerDto.id();
        ownerToken = IntegrationTestUtils.obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        ClinicStaffCreationDto vetReg = new ClinicStaffCreationDto(vetUsername, vetUsername+"@test.com", "password123", "Kafka", "VetPub", RoleEnum.VET, "KFKPVET"+System.currentTimeMillis(), "KFKVKEY"+System.currentTimeMillis());
        MvcResult vetRegResult = mockMvc.perform(post("/api/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vetReg)))
                .andExpect(status().isCreated()).andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id();
        vetToken = IntegrationTestUtils.obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(vetReg.username(), vetReg.password()));

        PetRegistrationDto pendingPetReg = new PetRegistrationDto("KafkaPending", Specie.DOG, LocalDate.now().minusMonths(1), null,null,null,null,null);
        MvcResult pendingPetRes = mockMvc.perform(post("/api/pets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pendingPetReg)))
                .andExpect(status().isCreated()).andReturn();
        petIdPending = IntegrationTestUtils.extractPetIdFromResult(objectMapper, pendingPetRes);

        PetRegistrationDto activePetReg = new PetRegistrationDto("KafkaActive", Specie.CAT, LocalDate.now().minusYears(1), null,null,null,null,"KFKACTIVE"+System.currentTimeMillis());
        MvcResult activePetRes = mockMvc.perform(post("/api/pets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activePetReg)))
                .andExpect(status().isCreated()).andReturn();
        petIdActive = IntegrationTestUtils.extractPetIdFromResult(objectMapper, activePetRes);

        mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdActive, clinicId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();

        Pet petToActivate = petRepository.findById(petIdActive).orElseThrow();
        PetActivationDto activationDto = new PetActivationDto(petToActivate.getName(), "Gray", Gender.FEMALE, petToActivate.getBirthDate(), petToActivate.getMicrochip(), petToActivate.getBreed().getId(), petToActivate.getImage());
        mockMvc.perform(put("/api/pets/{petId}/activate", petIdActive)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activationDto)))
                .andExpect(status().isOk());

        VaccineCreateDto rabiesVacDto = new VaccineCreateDto("RabiesKfk", 1, "RabLab", "BatchRKFK", true);
        RecordCreateDto rabiesRecDto = new RecordCreateDto(petIdActive, RecordType.VACCINE, "Rabies vaccine kafka test", rabiesVacDto);
        MvcResult rabiesRes = mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rabiesRecDto)))
                .andExpect(status().isCreated()).andReturn();
        IntegrationTestUtils.extractRecordIdFromResult(objectMapper, rabiesRes);

        RecordCreateDto annualRecDto = new RecordCreateDto(petIdActive, RecordType.ANNUAL_CHECK, "Annual checkup kafka test", null);
        mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(annualRecDto)))
                .andExpect(status().isCreated());

        entityManager.flush();

        String embeddedKafkaBrokers = System.getProperty(EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS);
        assertThat(embeddedKafkaBrokers).as("Embedded Kafka broker addresses should be set").isNotNull();
        String groupId = "testGroupPub" + System.currentTimeMillis();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBrokers);

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES,"com.petconnect.backend.pet.application.event,com.petconnect.backend.certificate.application.event");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokers);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        DefaultKafkaConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();

        consumer.subscribe(List.of(
                petActivationRequestsTopicName,
                petActivatedTopicName,
                certificateRequestsTopicName,
                certificateGeneratedTopicName
        ));
        Thread.sleep(500);
        int maxMessagesToConsume = 100;
        KafkaTestUtils.getRecords(consumer, Duration.ofMillis(100), maxMessagesToConsume);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("should publish PetActivationRequestedEvent when associatePetToClinic is called via API")
    void testPublishPetActivationRequestedEvent() throws Exception {
        Long clinicId = 1L;
        // Act
        mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdPending, clinicId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Assert
        ConsumerRecord<String, Object> recordItem = KafkaTestUtils.getSingleRecord(consumer, petActivationRequestsTopicName, Duration.ofSeconds(10));

        assertThat(recordItem).isNotNull();
        assertThat(recordItem.key()).isEqualTo(petIdPending.toString());
        assertThat(recordItem.value()).isInstanceOf(PetActivationRequestedEvent.class);

        PetActivationRequestedEvent event = (PetActivationRequestedEvent) recordItem.value();
        assertThat(event.petId()).isEqualTo(petIdPending);
        assertThat(event.ownerId()).isEqualTo(ownerId);
        assertThat(event.targetClinicId()).isEqualTo(clinicId);
    }

    @Test
    @DisplayName("should publish PetActivatedEvent when activatePet is called via API")
    void testPublishPetActivatedEvent() throws Exception {
        Long clinicId = 1L;
        // Arrange
        PetRegistrationDto pendingReg = new PetRegistrationDto("KafkaPendingActivate", Specie.RABBIT, LocalDate.now().minusMonths(3), null,null,null,null,null);
        MvcResult pendingPetRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pendingReg))).andExpect(status().isCreated()).andReturn();
        Long petToActivateId = extractPetIdFromResult(objectMapper, pendingPetRes);
        mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petToActivateId, clinicId).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();
        Pet petEntity = petRepository.findById(petToActivateId).orElseThrow();
        PetActivationDto activationDto = new PetActivationDto(petEntity.getName(), "White", Gender.MALE, petEntity.getBirthDate(), "ACTIVATECHIP"+System.currentTimeMillis(), petEntity.getBreed().getId(), petEntity.getImage());

        // Act
        mockMvc.perform(put("/api/pets/{petId}/activate", petToActivateId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activationDto)))
                .andExpect(status().isOk());

        // Assert
        ConsumerRecord<String, Object> recordItem = KafkaTestUtils.getSingleRecord(consumer, "test-pet-activated", Duration.ofSeconds(10));
        assertThat(recordItem).isNotNull();
        assertThat(recordItem.key()).isEqualTo(petToActivateId.toString());
        assertThat(recordItem.value()).isInstanceOf(PetActivatedEvent.class);

        PetActivatedEvent event = (PetActivatedEvent) recordItem.value();
        assertThat(event.petId()).isEqualTo(petToActivateId);
        assertThat(event.ownerId()).isEqualTo(ownerId);
        assertThat(event.activatingStaffId()).isEqualTo(vetId);
    }


    @Test
    @DisplayName("should publish CertificateRequestedEvent when requestCertificateGeneration is called via API")
    void testPublishCertificateRequestedEvent() throws Exception {
        // Act: Llamar al endpoint
        mockMvc.perform(post("/api/pets/{petId}/request-certificate/{vetId}", petIdActive, vetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Assert
        ConsumerRecord<String, Object> recordItem = KafkaTestUtils.getSingleRecord(consumer, "test-certificate-requests", Duration.ofSeconds(10));
        assertThat(recordItem).isNotNull();
        assertThat(recordItem.key()).isEqualTo(petIdActive.toString());
        assertThat(recordItem.value()).isInstanceOf(CertificateRequestedEvent.class);

        CertificateRequestedEvent event = (CertificateRequestedEvent) recordItem.value();
        assertThat(event.petId()).isEqualTo(petIdActive);
        assertThat(event.ownerId()).isEqualTo(ownerId);
        assertThat(event.targetVetId()).isEqualTo(vetId);
    }

    @Test
    @DisplayName("should publish CertificateGeneratedEvent when generateCertificate is called via API")
    void testPublishCertificateGeneratedEvent() throws Exception {
        // Arrange
        CertificateGenerationRequestDto requestDto = new CertificateGenerationRequestDto(petIdActive, testCertNumber);

        // Act
        MvcResult result = mockMvc.perform(post("/api/certificates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();
        CertificateViewDto generatedCert = objectMapper.readValue(result.getResponse().getContentAsString(), CertificateViewDto.class);
        Long generatedCertId = generatedCert.id();


        // Assert
        ConsumerRecord<String, Object> recordItem = KafkaTestUtils.getSingleRecord(consumer, "test-certificate-generated", Duration.ofSeconds(10));
        assertThat(recordItem).isNotNull();
        assertThat(recordItem.key()).isEqualTo(generatedCertId.toString());
        assertThat(recordItem.value()).isInstanceOf(CertificateGeneratedEvent.class);

        CertificateGeneratedEvent event = (CertificateGeneratedEvent) recordItem.value();
        assertThat(event.certificateId()).isEqualTo(generatedCertId);
        assertThat(event.petId()).isEqualTo(petIdActive);
        assertThat(event.ownerId()).isEqualTo(ownerId);
        assertThat(event.generatingVetId()).isEqualTo(vetId);
        assertThat(event.certificateNumber()).isEqualTo(testCertNumber);
    }
}
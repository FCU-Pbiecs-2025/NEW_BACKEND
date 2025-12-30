package Group4.Childcare.service;

import Group4.Childcare.DTO.*;
import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Model.Applications;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import Group4.Childcare.Service.ApplicationsService;
import Group4.Childcare.Service.EmailService;
import Group4.Childcare.Service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApplicationsService 單元測試
 * 
 * 測試範圍：
 * 1. create() - 創建申請
 * 2. generateCaseNumber() - 生成案件編號
 * 3. getById() - 根據ID查詢
 * 4. getAll() - 查詢所有
 * 5. update() - 更新申請
 * 6. apply() - 申請流程（各種分支）
 * 7. updateStatusAndSendEmail() - 狀態更新和郵件發送（各種分支）
 * 8. getCaseByChildrenNationalId() - 根據身分證查詢（各種分支）
 * 9. getCaseByParticipantId() - 根據參與者ID查詢（各種分支）
 */
@ExtendWith(MockitoExtension.class)
class ApplicationsServiceTest {

    @Mock
    private ApplicationsJdbcRepository applicationsJdbcRepository;

    @Mock
    private ApplicationParticipantsJdbcRepository applicationParticipantsRepository;

    @Mock
    private FileService fileService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationsService applicationsService;

    private UUID testApplicationId;
    private UUID testClassId;
    private Applications testApplication;

    @BeforeEach
    void setUp() {
        testApplicationId = UUID.randomUUID();
        testClassId = UUID.randomUUID();

        testApplication = new Applications();
        testApplication.setApplicationID(testApplicationId);
        testApplication.setApplicationDate(LocalDate.now());
        testApplication.setIdentityType((byte) 1);
    }

    // ========== create() 測試 ==========

    @Test
    void testCreate_Success() {
        when(applicationsJdbcRepository.save(any(Applications.class))).thenReturn(testApplication);

        Applications result = applicationsService.create(testApplication);

        assertNotNull(result);
        assertEquals(testApplicationId, result.getApplicationID());
        verify(applicationsJdbcRepository).save(testApplication);
    }

    // ========== generateCaseNumber() 測試 ==========

    @Test
    void testGenerateCaseNumber_Success() {
        when(applicationsJdbcRepository.countCaseNumberWithDateFormat()).thenReturn(5L);

        Long caseNumber = applicationsService.generateCaseNumber();

        assertNotNull(caseNumber);
        // 案件編號 = YYYYMMDD * 10000 + 6
        assertTrue(caseNumber > 0);
        verify(applicationsJdbcRepository).countCaseNumberWithDateFormat();
    }

    @Test
    void testGenerateCaseNumber_FirstCase() {
        when(applicationsJdbcRepository.countCaseNumberWithDateFormat()).thenReturn(0L);

        Long caseNumber = applicationsService.generateCaseNumber();

        assertNotNull(caseNumber);
        // 第一筆案件流水號應為 1
        assertTrue(caseNumber % 10000 == 1);
    }

    // ========== getById() 測試 ==========

    @Test
    void testGetById_Found() {
        when(applicationsJdbcRepository.findById(testApplicationId)).thenReturn(Optional.of(testApplication));

        Optional<Applications> result = applicationsService.getById(testApplicationId);

        assertTrue(result.isPresent());
        assertEquals(testApplicationId, result.get().getApplicationID());
    }

    @Test
    void testGetById_NotFound() {
        when(applicationsJdbcRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        Optional<Applications> result = applicationsService.getById(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    // ========== getAll() 測試 ==========

    @Test
    void testGetAll_Success() {
        List<Applications> applicationList = Arrays.asList(testApplication, new Applications());
        when(applicationsJdbcRepository.findAll()).thenReturn(applicationList);

        List<Applications> result = applicationsService.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void testGetAll_Empty() {
        when(applicationsJdbcRepository.findAll()).thenReturn(Collections.emptyList());

        List<Applications> result = applicationsService.getAll();

        assertTrue(result.isEmpty());
    }

    // ========== update() 測試 ==========

    @Test
    void testUpdate_Success() {
        when(applicationsJdbcRepository.save(any(Applications.class))).thenReturn(testApplication);

        Applications result = applicationsService.update(testApplicationId, testApplication);

        assertNotNull(result);
        assertEquals(testApplicationId, testApplication.getApplicationID());
        verify(applicationsJdbcRepository).save(testApplication);
    }

    // ========== apply() 測試 ==========

    @Test
    void testApply_WithLowIncomeIdentity() {
        ApplicationApplyDTO dto = createApplyDTO("低收入戶", null, null);

        applicationsService.apply(dto);

        verify(applicationsJdbcRepository).save(argThat(app -> app.getIdentityType() == 1));
    }

    @Test
    void testApply_WithMidLowIncomeIdentity() {
        ApplicationApplyDTO dto = createApplyDTO("中低收入戶", null, null);

        applicationsService.apply(dto);

        verify(applicationsJdbcRepository).save(argThat(app -> app.getIdentityType() == 2));
    }

    @Test
    void testApply_WithOtherIdentity() {
        ApplicationApplyDTO dto = createApplyDTO("一般", null, null);

        applicationsService.apply(dto);

        verify(applicationsJdbcRepository).save(argThat(app -> app.getIdentityType() == 0));
    }

    @Test
    void testApply_WithAttachments() {
        List<String> attachments = Arrays.asList("file1.pdf", "file2.pdf");
        ApplicationApplyDTO dto = createApplyDTO("一般", attachments, null);

        applicationsService.apply(dto);

        verify(applicationsJdbcRepository)
                .save(argThat(app -> app.getAttachmentPath() != null && app.getAttachmentPath().contains("file1.pdf")));
    }

    @Test
    void testApply_WithEmptyAttachments() {
        ApplicationApplyDTO dto = createApplyDTO("一般", new ArrayList<>(), null);

        applicationsService.apply(dto);

        verify(applicationsJdbcRepository).save(any(Applications.class));
    }

    @Test
    void testApply_WithParentParticipant() {
        ApplicationParticipantDTO participant = createParticipantDTO("家長", "A123456789", "Test Parent", "男",
                "1980-01-01");
        ApplicationApplyDTO dto = createApplyDTO("一般", null, Collections.singletonList(participant));

        applicationsService.apply(dto);

        verify(applicationParticipantsRepository).save(argThat(p -> p.getParticipantType() == true));
    }

    @Test
    void testApply_WithChildParticipant() {
        ApplicationParticipantDTO participant = createParticipantDTO("幼兒", "B234567890", "Test Child", "女",
                "2020-01-01");
        ApplicationApplyDTO dto = createApplyDTO("一般", null, Collections.singletonList(participant));

        applicationsService.apply(dto);

        verify(applicationParticipantsRepository).save(argThat(p -> p.getParticipantType() == false));
    }

    @Test
    void testApply_WithParticipantType1() {
        ApplicationParticipantDTO participant = createParticipantDTO("1", "A123456789", "Test", "男", null);
        ApplicationApplyDTO dto = createApplyDTO("一般", null, Collections.singletonList(participant));

        applicationsService.apply(dto);

        verify(applicationParticipantsRepository).save(argThat(p -> p.getParticipantType() == true));
    }

    @Test
    void testApply_WithNullBirthDate() {
        ApplicationParticipantDTO participant = createParticipantDTO("幼兒", "B234567890", "Test", "男", null);
        ApplicationApplyDTO dto = createApplyDTO("一般", null, Collections.singletonList(participant));

        applicationsService.apply(dto);

        verify(applicationParticipantsRepository).save(argThat(p -> p.getBirthDate() == null));
    }

    @Test
    void testApply_WithEmptyBirthDate() {
        ApplicationParticipantDTO participant = createParticipantDTO("幼兒", "B234567890", "Test", "男", "");
        ApplicationApplyDTO dto = createApplyDTO("一般", null, Collections.singletonList(participant));

        applicationsService.apply(dto);

        verify(applicationParticipantsRepository).save(argThat(p -> p.getBirthDate() == null));
    }

    @Test
    void testApply_WithSuspendEnd() {
        ApplicationParticipantDTO participant = createParticipantDTO("幼兒", "B234567890", "Test", "女", "2020-01-01");
        participant.suspendEnd = "2025-12-31";
        participant.classID = testClassId.toString();
        ApplicationApplyDTO dto = createApplyDTO("一般", null, Collections.singletonList(participant));

        applicationsService.apply(dto);

        verify(applicationParticipantsRepository).save(any(ApplicationParticipants.class));
    }

    @Test
    void testApply_WithNullParticipants() {
        ApplicationApplyDTO dto = createApplyDTO("一般", null, null);

        applicationsService.apply(dto);

        verify(applicationsJdbcRepository).save(any(Applications.class));
        verify(applicationParticipantsRepository, never()).save(any());
    }

    // ========== updateStatusAndSendEmail() 測試 ==========

    @Test
    void testUpdateStatusAndSendEmail_EmailNotFound() throws Exception {
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId)).thenReturn(Optional.empty());

        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "已錄取", "reason",
                LocalDateTime.now());

        verify(emailService, never()).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testUpdateStatusAndSendEmail_EmailEmpty() throws Exception {
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId)).thenReturn(Optional.of(""));

        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "已錄取", "reason",
                LocalDateTime.now());

        verify(emailService, never()).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testUpdateStatusAndSendEmail_CaseNotFound() throws Exception {
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of("test@example.com"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), isNull()))
                .thenReturn(Optional.empty());

        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "已錄取", "reason",
                LocalDateTime.now());

        verify(emailService, never()).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testUpdateStatusAndSendEmail_Success_WithEmailService() throws Exception {
        ApplicationCaseDTO caseDto = createApplicationCaseDTO();
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of("test@example.com"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), isNull()))
                .thenReturn(Optional.of(caseDto));
        doNothing().when(emailService).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), any(), any());

        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "已錄取", "reason",
                LocalDateTime.now());

        verify(emailService).sendApplicationStatusChangeEmail(eq("test@example.com"), anyString(), anyString(),
                anyString(), any(), anyString(), eq("已錄取"), any(), eq("reason"));
    }

    @Test
    void testUpdateStatusAndSendEmail_EmailServiceThrowsException() throws Exception {
        ApplicationCaseDTO caseDto = createApplicationCaseDTO();
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of("test@example.com"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), isNull()))
                .thenReturn(Optional.of(caseDto));
        doThrow(new RuntimeException("Email error")).when(emailService).sendApplicationStatusChangeEmail(anyString(),
                anyString(), anyString(), anyString(), any(), anyString(), anyString(), any(), any());

        // Should not throw exception
        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "已錄取", "reason",
                LocalDateTime.now());

        verify(emailService).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(), anyString(), any(),
                anyString(), anyString(), any(), any());
    }

    @Test
    void testUpdateStatusAndSendEmail_NoParents() throws Exception {
        ApplicationCaseDTO caseDto = createApplicationCaseDTO();
        caseDto.parents = null;
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of("test@example.com"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), isNull()))
                .thenReturn(Optional.of(caseDto));
        doNothing().when(emailService).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), any(), any());

        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "已錄取", null, LocalDateTime.now());

        verify(emailService).sendApplicationStatusChangeEmail(anyString(), eq(""), anyString(), anyString(), any(),
                anyString(), anyString(), any(), any());
    }

    @Test
    void testUpdateStatusAndSendEmail_EmptyChildren() throws Exception {
        ApplicationCaseDTO caseDto = createApplicationCaseDTO();
        caseDto.children = new ArrayList<>();
        when(applicationsJdbcRepository.getUserEmailByApplicationId(testApplicationId))
                .thenReturn(Optional.of("test@example.com"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), isNull()))
                .thenReturn(Optional.of(caseDto));
        doNothing().when(emailService).sendApplicationStatusChangeEmail(anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), any(), any());

        applicationsService.updateStatusAndSendEmail(testApplicationId, "A123456789", "候補中", null, LocalDateTime.now());

        verify(emailService).sendApplicationStatusChangeEmail(anyString(), anyString(), eq(""), anyString(), any(),
                anyString(), anyString(), isNull(), any());
    }

    // ========== getCaseByChildrenNationalId() 測試 ==========

    @Test
    void testGetCaseByChildrenNationalId_NotFound() {
        when(applicationsJdbcRepository.findByNationalID(anyString())).thenReturn(Collections.emptyList());

        Optional<CaseEditUpdateDTO> result = applicationsService.getCaseByChildrenNationalId("A123456789");

        assertFalse(result.isPresent());
    }

    @Test
    void testGetCaseByChildrenNationalId_Found_NoFiles() {
        CaseEditUpdateDTO dto = new CaseEditUpdateDTO();
        dto.setApplicationID(testApplicationId);
        when(applicationsJdbcRepository.findByNationalID("A123456789")).thenReturn(Collections.singletonList(dto));
        when(fileService.getFilesByApplicationId(testApplicationId)).thenReturn(Collections.emptyList());
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), any()))
                .thenReturn(Optional.empty());

        Optional<CaseEditUpdateDTO> result = applicationsService.getCaseByChildrenNationalId("A123456789");

        assertTrue(result.isPresent());
        assertNull(result.get().getAttachmentPath());
    }

    @Test
    void testGetCaseByChildrenNationalId_Found_WithMultipleFiles() {
        CaseEditUpdateDTO dto = new CaseEditUpdateDTO();
        dto.setApplicationID(testApplicationId);
        when(applicationsJdbcRepository.findByNationalID("A123456789")).thenReturn(Collections.singletonList(dto));
        when(fileService.getFilesByApplicationId(testApplicationId))
                .thenReturn(Arrays.asList("file1.pdf", "file2.pdf", "file3.pdf", "file4.pdf"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), any()))
                .thenReturn(Optional.empty());

        Optional<CaseEditUpdateDTO> result = applicationsService.getCaseByChildrenNationalId("A123456789");

        assertTrue(result.isPresent());
        assertEquals("file1.pdf", result.get().getAttachmentPath());
        assertEquals("file2.pdf", result.get().getAttachmentPath1());
        assertEquals("file3.pdf", result.get().getAttachmentPath2());
        assertEquals("file4.pdf", result.get().getAttachmentPath3());
    }

    @Test
    void testGetCaseByChildrenNationalId_WithCaseDetails() {
        CaseEditUpdateDTO dto = new CaseEditUpdateDTO();
        dto.setApplicationID(testApplicationId);
        ApplicationCaseDTO caseDto = createApplicationCaseDTO();

        when(applicationsJdbcRepository.findByNationalID("A123456789")).thenReturn(Collections.singletonList(dto));
        when(fileService.getFilesByApplicationId(testApplicationId)).thenReturn(Collections.singletonList("file1.pdf"));
        when(applicationsJdbcRepository.findApplicationCaseById(any(), anyString(), any()))
                .thenReturn(Optional.of(caseDto));

        Optional<CaseEditUpdateDTO> result = applicationsService.getCaseByChildrenNationalId("A123456789");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getParents());
        assertNotNull(result.get().getChildren());
    }

    // ========== getCaseByParticipantId() 測試 ==========

    @Test
    void testGetCaseByParticipantId_NotFound() {
        UUID participantId = UUID.randomUUID();
        when(applicationsJdbcRepository.findCaseByParticipantId(participantId)).thenReturn(Optional.empty());

        Optional<CaseEditUpdateDTO> result = applicationsService.getCaseByParticipantId(participantId);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetCaseByParticipantId_Found_WithFiles() {
        UUID participantId = UUID.randomUUID();
        CaseEditUpdateDTO dto = new CaseEditUpdateDTO();
        dto.setApplicationID(testApplicationId);

        when(applicationsJdbcRepository.findCaseByParticipantId(participantId)).thenReturn(Optional.of(dto));
        when(fileService.getFilesByApplicationId(testApplicationId))
                .thenReturn(Arrays.asList("file1.pdf", "file2.pdf"));
        when(applicationsJdbcRepository.findApplicationCaseByParticipantId(participantId)).thenReturn(Optional.empty());

        Optional<CaseEditUpdateDTO> result = applicationsService.getCaseByParticipantId(participantId);

        assertTrue(result.isPresent());
        assertEquals("file1.pdf", result.get().getAttachmentPath());
        assertEquals("file2.pdf", result.get().getAttachmentPath1());
    }

    // ========== 其他方法測試 ==========

    @Test
    void testGetSummaryByUserID_Success() {
        UUID userId = UUID.randomUUID();
        List<ApplicationSummaryDTO> summaries = Collections.singletonList(new ApplicationSummaryDTO());
        when(applicationsJdbcRepository.findSummaryByUserID(userId)).thenReturn(summaries);

        List<ApplicationSummaryDTO> result = applicationsService.getSummaryByUserID(userId);

        assertEquals(1, result.size());
    }

    @Test
    void testGetTotalApplicationsCount_Success() {
        when(applicationsJdbcRepository.count()).thenReturn(100L);

        long result = applicationsService.getTotalApplicationsCount();

        assertEquals(100L, result);
    }

    @Test
    void testCountAcceptedApplicationsByChildNationalID_Success() {
        when(applicationsJdbcRepository.countAcceptedApplicationsByChildNationalID("A123456789")).thenReturn(3);

        int result = applicationsService.countAcceptedApplicationsByChildNationalID("A123456789");

        assertEquals(3, result);
    }

    @Test
    void testCountPendingApplicationsByChildNationalID_Success() {
        when(applicationsJdbcRepository.countPendingApplicationsByChildNationalID("A123456789")).thenReturn(2);

        int result = applicationsService.countPendingApplicationsByChildNationalID("A123456789");

        assertEquals(2, result);
    }

    @Test
    void testCountActiveApplicationsByChildAndInstitution_Success() {
        UUID institutionId = UUID.randomUUID();
        when(applicationsJdbcRepository.countActiveApplicationsByChildAndInstitution("A123456789", institutionId))
                .thenReturn(1);

        int result = applicationsService.countActiveApplicationsByChildAndInstitution("A123456789", institutionId);

        assertEquals(1, result);
    }

    // ========== 輔助方法 ==========

    private ApplicationApplyDTO createApplyDTO(String identityType, List<String> attachments,
            List<ApplicationParticipantDTO> participants) {
        ApplicationApplyDTO dto = new ApplicationApplyDTO();
        dto.identityType = identityType;
        dto.attachmentFiles = attachments;
        dto.participants = participants;
        return dto;
    }

    private ApplicationParticipantDTO createParticipantDTO(String participantType, String nationalID, String name,
            String gender, String birthDate) {
        ApplicationParticipantDTO dto = new ApplicationParticipantDTO();
        dto.participantType = participantType;
        dto.nationalID = nationalID;
        dto.name = name;
        dto.gender = gender;
        dto.birthDate = birthDate;
        return dto;
    }

    private ApplicationCaseDTO createApplicationCaseDTO() {
        ApplicationCaseDTO dto = new ApplicationCaseDTO();
        dto.applicationId = testApplicationId;
        dto.caseNumber = 202412280001L;
        dto.institutionName = "Test Institution";
        dto.applicationDate = LocalDate.now();

        ApplicationParticipantDTO parent = new ApplicationParticipantDTO();
        parent.name = "Parent Name";
        dto.parents = Collections.singletonList(parent);

        ApplicationParticipantDTO child = new ApplicationParticipantDTO();
        child.name = "Child Name";
        child.currentOrder = 1;
        dto.children = Collections.singletonList(child);

        return dto;
    }
}

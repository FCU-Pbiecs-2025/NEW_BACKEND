package Group4.Childcare.repository;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.FamilyInfoJdbcRepository;
import Group4.Childcare.Repository.UserJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private FamilyInfoJdbcRepository familyInfoRepository;

    @InjectMocks
    private UserJdbcRepository repository;

    private UUID testId = UUID.randomUUID();

    @Test
    void testUsersRowMapper_AllBranches() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("UserID")).thenReturn(testId.toString());

        // Null branches
        when(rs.getDate("BirthDate")).thenReturn(null);
        when(rs.getString("FamilyInfoID")).thenReturn(null);
        when(rs.getString("InstitutionID")).thenReturn(null);

        ArgumentCaptor<RowMapper<Users>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        repository.findAll();
        verify(jdbcTemplate, atLeastOnce()).query(anyString(), mapperCaptor.capture());
        RowMapper<Users> mapper = mapperCaptor.getValue();

        Users resultNull = mapper.mapRow(rs, 1);
        assertNull(resultNull.getBirthDate());
        assertNull(resultNull.getFamilyInfoID());
        assertNull(resultNull.getInstitutionID());

        // Non-null branches
        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
        when(rs.getDate("BirthDate")).thenReturn(now);
        when(rs.getString("FamilyInfoID")).thenReturn(testId.toString());
        when(rs.getString("InstitutionID")).thenReturn(testId.toString());

        Users resultFull = mapper.mapRow(rs, 1);
        assertNotNull(resultFull.getBirthDate());
        assertEquals(testId, resultFull.getFamilyInfoID());
        assertEquals(testId, resultFull.getInstitutionID());
    }

    @Test
    void testInsertAndUpdate_NonNullTernaries() {
        Users user = new Users();
        user.setAccount("new");
        user.setPassword("pass");
        user.setFamilyInfoID(testId);
        user.setAccountStatus((byte) 2);
        user.setPermissionType((byte) 3);
        user.setGender(true);
        user.setBirthDate(LocalDate.now());
        user.setInstitutionID(testId);
        user.setNationalID("A123");

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(anyString(), (Object[]) any())).thenReturn(1);

        // Test Insert
        repository.save(user);
        verify(jdbcTemplate).update(startsWith("INSERT"),
                eq(user.getUserID().toString()), eq("new"), eq("pass"), eq((byte) 2), eq((byte) 3),
                any(), eq(true), any(), any(), any(), any(), eq(testId.toString()), eq(testId.toString()), eq("A123"));

        // Test Update
        user.setUserID(testId);
        repository.save(user);
        verify(jdbcTemplate).update(startsWith("UPDATE"),
                eq("new"), eq("pass"), eq((byte) 2), eq((byte) 3), any(), eq(true),
                any(), any(), any(), any(), eq(testId.toString()), eq(testId.toString()), eq("A123"),
                eq(testId.toString()));
    }

    @Test
    void testExistsMethods_NullAndCatch() {
        assertFalse(repository.existsByAccount(null));
        assertFalse(repository.existsByAccount(""));

        assertFalse(repository.existsByEmail(null));
        assertFalse(repository.existsByEmail(""));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(repository.existsByAccount("abc"));
        assertFalse(repository.existsByEmail("abc@test.com"));
    }

    @Test
    void testSave_Validation() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));

        Users user = new Users();
        assertThrows(IllegalArgumentException.class, () -> repository.save(user)); // Missing account

        user.setAccount("test");
        assertThrows(IllegalArgumentException.class, () -> repository.save(user)); // Missing password

        user.setPassword("pass");

        FamilyInfo fi = new FamilyInfo();
        fi.setFamilyInfoID(UUID.randomUUID());
        when(familyInfoRepository.save(any())).thenReturn(fi);
        when(jdbcTemplate.update(anyString(), (Object[]) any())).thenReturn(1);

        repository.save(user);
        assertEquals(fi.getFamilyInfoID(), user.getFamilyInfoID());
    }

    @Test
    void testInsert_DuplicateChecksAndTernaries() {
        Users user = new Users();
        user.setAccount("new");
        user.setPassword("pass");
        user.setFamilyInfoID(UUID.randomUUID());
        user.setAccountStatus(null);
        user.setPermissionType(null);
        user.setGender(null);
        user.setBirthDate(null);
        user.setInstitutionID(null);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(startsWith("INSERT"), (Object[]) any())).thenReturn(1);

        repository.save(user);

        // Verify insert ternaries (null branches)
        verify(jdbcTemplate).update(startsWith("INSERT"),
                any(), any(), any(), eq((byte) 1), eq((byte) 1), any(), eq(false), any(), any(), any(), isNull(), any(),
                isNull(), any());
    }

    @Test
    void testUpdate_Ternaries() {
        Users user = new Users();
        user.setUserID(testId);
        user.setAccount("acc");
        user.setPassword("pass");
        user.setFamilyInfoID(UUID.randomUUID());
        user.setAccountStatus(null);
        user.setBirthDate(null);

        when(jdbcTemplate.update(startsWith("UPDATE"), (Object[]) any())).thenReturn(1);

        repository.save(user);

        // Verify update ternaries
        verify(jdbcTemplate).update(startsWith("UPDATE"),
                any(), any(), eq((byte) 1), any(), any(), any(), any(), any(), any(), isNull(), any(), isNull(), any(),
                eq(testId.toString()));
    }

    @Test
    void testInsert_DuplicateEmail() {
        Users user = new Users();
        user.setAccount("unique_acc");
        user.setPassword("pass");
        user.setEmail("dup@test.com");
        user.setFamilyInfoID(testId);

        // Literal SQL strings to avoid ambiguity
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE Account = ?"), eq(Integer.class),
                eq("unique_acc")))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE Email = ?"), eq(Integer.class),
                eq("dup@test.com")))
                .thenReturn(1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> repository.save(user));
        assertTrue(ex.getMessage().contains("Email"), "Actual message: " + ex.getMessage());
    }

    @Test
    void testExists_EdgeCases() {
        assertFalse(repository.existsByAccount("   "));
        assertFalse(repository.existsByEmail("   "));
    }

    @Test
    void testUpdateProfile_MoreCombinations() {
        // No fields
        assertEquals(0, repository.updateProfile(testId, null, null, null, null));

        // One field (first)
        repository.updateProfile(testId, "name", null, null, null);
        verify(jdbcTemplate).update(matches("UPDATE users SET Name = \\? WHERE UserID = \\?"), any(Object[].class));

        // Middle fields (not first)
        repository.updateProfile(testId, "name", "email", null, "addr");
        verify(jdbcTemplate).update(
                matches("UPDATE users SET Name = \\?, Email = \\?, MailingAddress = \\? WHERE UserID = \\?"),
                any(Object[].class));

        // Only email (first = true at this point)
        repository.updateProfile(testId, null, "email@test.com", null, null);
        verify(jdbcTemplate).update(matches("UPDATE users SET Email = \\? WHERE UserID = \\?"), any(Object[].class));

        // Only phone
        repository.updateProfile(testId, null, null, "123", null);
        verify(jdbcTemplate).update(matches("UPDATE users SET PhoneNumber = \\? WHERE UserID = \\?"),
                any(Object[].class));

        // Only address
        repository.updateProfile(testId, null, null, null, "addr");
        verify(jdbcTemplate).update(matches("UPDATE users SET MailingAddress = \\? WHERE UserID = \\?"),
                any(Object[].class));
    }

    @Test
    void testUpdate_NotFound() {
        Users user = new Users();
        user.setUserID(testId);
        user.setAccount("acc");
        user.setPassword("pass");

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0); // Simulate
                                                                                                      // account not
                                                                                                      // found for
                                                                                                      // update check
        when(jdbcTemplate.update(startsWith("UPDATE"), (Object[]) any())).thenReturn(0); // Simulate no rows affected

        assertThrows(RuntimeException.class, () -> repository.save(user));
    }

    @Test
    void testUpdateAccountStatus_SuccessAndCatch() {
        when(jdbcTemplate.update(anyString(), anyInt(), anyString())).thenReturn(1);
        assertEquals(1, repository.updateAccountStatus(testId, 2));

        when(jdbcTemplate.update(anyString(), anyInt(), anyString())).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> repository.updateAccountStatus(testId, 2));
    }

    @Test
    void testSearchMethods_NullResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString(),
                anyString()))
                .thenReturn(null);
        assertEquals(0, repository.countSearchUsers("abc"));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
                .thenReturn(null);
        assertEquals(0, repository.countSearchUsersByAccount("abc"));
        assertEquals(0, repository.countSearchCitizenUsersByAccount("abc"));
    }

    @Test
    void testSearchAndCountMethods() {
        // Mock query for UserSummaryDTO
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenReturn(java.util.Collections.emptyList());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.Collections.emptyList());

        repository.findWithOffsetAndInstitutionName(0, 10);
        repository.searchUsersWithOffset("abc", 0, 10);
        repository.searchUsersByAccountWithOffset("abc", 0, 10);
        repository.searchCitizenUsersByAccountWithOffset("abc", 0, 10);

        // Mock queryForObject for counts
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any()))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(5L);

        assertEquals(5, repository.countTotal());
        assertEquals(5, repository.countSearchUsers("abc"));
        assertEquals(5, repository.countSearchUsersByAccount("abc"));
        assertEquals(5, repository.countSearchCitizenUsersByAccount("abc"));
    }

    @Test
    void testDeleteAndExistsById() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        assertTrue(repository.existsById(testId));

        Users user = new Users();
        user.setUserID(testId);
        repository.delete(user);
        verify(jdbcTemplate).update(contains("DELETE"), eq(testId.toString()));
    }

    @Test
    void testFindMethods_CatchBlocks() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new RuntimeException("error"));

        assertTrue(repository.findById(testId).isEmpty());
        assertTrue(repository.findByAccount("abc").isEmpty());
        assertTrue(repository.findByEmail("abc").isEmpty());
    }
}

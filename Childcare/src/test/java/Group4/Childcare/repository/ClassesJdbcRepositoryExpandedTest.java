package Group4.Childcare.repository;

import Group4.Childcare.Model.Classes;
import Group4.Childcare.DTO.ClassSummaryDTO;
import Group4.Childcare.DTO.ClassNameDTO;
import Group4.Childcare.Repository.ClassesJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class ClassesJdbcRepositoryExpandedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ClassesJdbcRepository repository;

    private UUID testClassId;
    private UUID testInstitutionId;

    @BeforeEach
    void setUp() {
        testClassId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();
    }

    @Test
    void testRowMapper_AllBranches() throws SQLException {
        // Capture the static RowMapper
        ArgumentCaptor<RowMapper<?>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        
        // Test CLASSES_ROW_MAPPER (lambda$static$0)
        when(jdbcTemplate.query(anyString(), (RowMapper<Classes>) any())).thenReturn(Collections.emptyList());
        repository.findAll();
        verify(jdbcTemplate).query(anyString(), (RowMapper<Classes>) mapperCaptor.capture());
        RowMapper<Classes> mapper = (RowMapper<Classes>) mapperCaptor.getValue();

        ResultSet rs = mock(ResultSet.class);
        
        // Branch 1: All fields present
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Test Class");
        when(rs.getObject("Capacity")).thenReturn(20);
        when(rs.getObject("CurrentStudents")).thenReturn(10);
        when(rs.getObject("MinAgeDescription")).thenReturn(3);
        when(rs.getObject("MaxAgeDescription")).thenReturn(6);
        when(rs.getString("AdditionalInfo")).thenReturn("Info");
        when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());

        Classes result = mapper.mapRow(rs, 1);
        assertNotNull(result);
        assertEquals(testClassId, result.getClassID());
        assertEquals(20, result.getCapacity());
        assertEquals(10, result.getCurrentStudents());
        assertEquals(3, result.getMinAgeDescription());
        assertEquals(6, result.getMaxAgeDescription());
        assertEquals(testInstitutionId, result.getInstitutionID());

        // Branch 2: All nullable fields are null
        when(rs.getObject("Capacity")).thenReturn(null);
        when(rs.getObject("CurrentStudents")).thenReturn(null);
        when(rs.getObject("MinAgeDescription")).thenReturn(null);
        when(rs.getObject("MaxAgeDescription")).thenReturn(null);
        when(rs.getString("InstitutionID")).thenReturn(null);

        result = mapper.mapRow(rs, 2);
        assertNull(result.getCapacity());
        assertNull(result.getCurrentStudents());
        assertNull(result.getMinAgeDescription());
        assertNull(result.getMaxAgeDescription());
        assertNull(result.getInstitutionID());

        // Test CLASS_NAME_ROW_MAPPER (lambda$static$1)
        reset(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), (RowMapper<ClassNameDTO>) any(), any())).thenReturn(Collections.emptyList());
        repository.findClassNamesByInstitutionId(testInstitutionId);
        verify(jdbcTemplate).query(anyString(), (RowMapper<ClassNameDTO>) mapperCaptor.capture(), any());
        RowMapper<ClassNameDTO> nameMapper = (RowMapper<ClassNameDTO>) mapperCaptor.getValue();
        
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getString("ClassName")).thenReturn("Name");
        ClassNameDTO nameDto = nameMapper.mapRow(rs, 1);
        assertEquals(testClassId, nameDto.getClassID());
        assertEquals("Name", nameDto.getClassName());

        when(rs.getString("ClassID")).thenReturn(null);
        nameDto = nameMapper.mapRow(rs, 2);
        assertNull(nameDto.getClassID());

        // Test CLASS_SUMMARY_ROW_MAPPER (lambda$static$2)
        reset(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), (RowMapper<ClassSummaryDTO>) any())).thenReturn(Collections.emptyList());
        repository.findAllWithInstitutionName();
        verify(jdbcTemplate).query(anyString(), (RowMapper<ClassSummaryDTO>) mapperCaptor.capture());
        RowMapper<ClassSummaryDTO> summaryMapper = (RowMapper<ClassSummaryDTO>) mapperCaptor.getValue();

        // Branch 1: All fields present
        when(rs.getString("ClassID")).thenReturn(testClassId.toString());
        when(rs.getObject("Capacity")).thenReturn(20);
        when(rs.getObject("MinAgeDescription")).thenReturn(3);
        when(rs.getObject("MaxAgeDescription")).thenReturn(6);
        when(rs.getString("InstitutionName")).thenReturn("Inst Name");
        
        ClassSummaryDTO summaryDto = summaryMapper.mapRow(rs, 1);
        assertEquals("Inst Name", summaryDto.getInstitutionName());
        assertEquals(20, summaryDto.getCapacity());
        assertEquals("3", summaryDto.getMinAgeDescription());

        // Branch 2: All nullable fields are null
        when(rs.getString("ClassID")).thenReturn(null);
        when(rs.getObject("Capacity")).thenReturn(null);
        when(rs.getObject("MinAgeDescription")).thenReturn(null);
        when(rs.getObject("MaxAgeDescription")).thenReturn(null);
        when(rs.getString("InstitutionName")).thenReturn(null);
        
        summaryDto = summaryMapper.mapRow(rs, 2);
        assertNull(summaryDto.getClassID());
        assertNull(summaryDto.getCapacity());
        assertNull(summaryDto.getMinAgeDescription());
        assertNull(summaryDto.getMaxAgeDescription());
        assertNull(summaryDto.getInstitutionName());
    }

    @Test
    void testSave_Branches() {
        Classes classes = new Classes();
        classes.setClassName("Test");

        // Branch: classID is null
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        repository.save(classes);
        assertNotNull(classes.getClassID());

        // Branch: classID not null, but !existsById
        UUID id = UUID.randomUUID();
        classes.setClassID(id);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        repository.save(classes);
        verify(jdbcTemplate, atLeastOnce()).update(argThat(s -> s.contains("INSERT")), any(), any(), any(), any(), any(), any(), any(), any());

        // Branch: classID not null, and existsById
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        repository.save(classes);
        verify(jdbcTemplate, atLeastOnce()).update(argThat(s -> s.contains("UPDATE")), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testInsert_Branches() {
        Classes classes = new Classes();
        classes.setClassID(testClassId);
        classes.setClassName("Test");
        
        // Branch: InstitutionID is null
        classes.setInstitutionID(null);
        repository.save(classes); // This will call insert because we didn't mock existsById yet (default 0)
        
        // Branch: InstitutionID is not null
        classes.setInstitutionID(testInstitutionId);
        repository.save(classes);
    }

    @Test
    void testUpdate_Branches() {
        Classes classes = new Classes();
        classes.setClassID(testClassId);
        classes.setClassName("Test");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);

        // Branch: InstitutionID is null
        classes.setInstitutionID(null);
        repository.save(classes);
        
        // Branch: InstitutionID is not null
        classes.setInstitutionID(testInstitutionId);
        repository.save(classes);
    }

    @Test
    void testCount_Branches() {
        // Branch: count returns null
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
        assertEquals(0, repository.count());

        // Branch: count returns value
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);
        assertEquals(5, repository.count());
    }

    @Test
    void testCountByInstitutionID_Branches() {
        // Branch: count returns null
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(null);
        assertEquals(0, repository.countByInstitutionID(testInstitutionId));

        // Branch: count returns value
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(10L);
        assertEquals(10, repository.countByInstitutionID(testInstitutionId));
    }

    @Test
    void testExistsById_Branches() {
        // Branch: count returns null
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);
        assertFalse(repository.existsById(testClassId));

        // Branch: count returns 0
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertFalse(repository.existsById(testClassId));

        // Branch: count returns 1
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        assertTrue(repository.existsById(testClassId));
    }

    @Test
    void testIsClassFull_Branches() {
        // Branch: isFull returns true
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(true);
        assertTrue(repository.isClassFull(testClassId));

        // Branch: isFull returns false
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(false);
        assertFalse(repository.isClassFull(testClassId));

        // Branch: isFull returns null
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(null);
        assertFalse(repository.isClassFull(testClassId));

        // Branch: catch (Exception e)
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyString())).thenThrow(new RuntimeException("DB Error"));
        assertTrue(repository.isClassFull(testClassId));
    }
}

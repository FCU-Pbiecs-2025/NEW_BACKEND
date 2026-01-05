package Group4.Childcare.security;

import Group4.Childcare.Model.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secretKey = "mySecretKeyForJWTTokenGenerationThatIsLongEnough1234567890";
    private final long expirationTime = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", expirationTime);
    }

    @Test
    void testGenerateToken_WithSuperAdmin() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("superadmin");
        user.setPermissionType((byte) 1);

        // When
        String token = jwtUtil.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token claims
        Claims claims = extractClaimsForTest(token);
        assertEquals("superadmin", claims.getSubject());
        assertEquals("ROLE_SUPER_ADMIN", claims.get("roles"));
        assertEquals(user.getUserID().toString(), claims.get("userId"));
    }

    @Test
    void testGenerateToken_WithAdmin() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("admin");
        user.setPermissionType((byte) 2);

        // When
        String token = jwtUtil.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token claims
        Claims claims = extractClaimsForTest(token);
        assertEquals("admin", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("roles"));
        assertEquals(user.getUserID().toString(), claims.get("userId"));
    }

    @Test
    void testGenerateToken_WithRegularUser() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("user");
        user.setPermissionType((byte) 0);

        // When
        String token = jwtUtil.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token claims
        Claims claims = extractClaimsForTest(token);
        assertEquals("user", claims.getSubject());
        assertEquals("ROLE_USER", claims.get("roles"));
        assertEquals(user.getUserID().toString(), claims.get("userId"));
    }

    @Test
    void testGenerateToken_WithOtherPermissionType() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("otheruser");
        user.setPermissionType((byte) 99);

        // When
        String token = jwtUtil.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token claims
        Claims claims = extractClaimsForTest(token);
        assertEquals("otheruser", claims.getSubject());
        assertEquals("ROLE_USER", claims.get("roles"));
        assertEquals(user.getUserID().toString(), claims.get("userId"));
    }

    @Test
    void testExtractAccount() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("testuser");
        user.setPermissionType((byte) 1);
        String token = jwtUtil.generateToken(user);

        // When
        String account = jwtUtil.extractAccount(token);

        // Then
        assertEquals("testuser", account);
    }

    @Test
    void testExtractRole() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("testadmin");
        user.setPermissionType((byte) 2);
        String token = jwtUtil.generateToken(user);

        // When
        String role = jwtUtil.extractRole(token);

        // Then
        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    void testValidateToken_WithValidToken() {
        // Given
        Users user = new Users();
        user.setUserID(UUID.randomUUID());
        user.setAccount("testuser");
        user.setPermissionType((byte) 1);
        String token = jwtUtil.generateToken(user);

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithMalformedToken() {
        // Given
        String malformedToken = "malformed";

        // When
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WithEmptyToken() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtUtil.validateToken(emptyToken);

        // Then
        assertFalse(isValid);
    }

    // Helper method to extract claims for testing
    private Claims extractClaimsForTest(String token) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}


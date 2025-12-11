package Group4.Childcare.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret:}")
    private String secretKey;

    @Value("${recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    @Value("${recaptcha.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verify(String token) {
        if (!enabled) return true; // skip verification when disabled
        if (token == null || token.isBlank()) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String body = "secret=" + secretKey + "&response=" + token;
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(verifyUrl, entity, Map.class);
            Map<String, Object> map = resp.getBody();
            if (map == null) return false;
            Object success = map.get("success");
            if (success instanceof Boolean b && b) {
                Object scoreObj = map.get("score");
                if (scoreObj instanceof Number n) {
                    return n.doubleValue() >= 0.5;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            // on error, fail closed
            return false;
        }
    }
}

package Group4.Childcare.Service;

import Group4.Childcare.DTO.RecaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.url}")
    private String recaptchaUrl;

    @Value("${google.recaptcha.v3.threshold}")
    private float recaptchaThreshold;

    private final RestTemplate restTemplate;

    public RecaptchaService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean verify(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", recaptchaSecret);
            map.add("response", token);

            RecaptchaResponse response = restTemplate.postForObject(recaptchaUrl, map, RecaptchaResponse.class);

            if (response != null && response.isSuccess()) {
                return response.getScore() >= recaptchaThreshold;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
}


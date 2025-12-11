package Group4.Childcare.DTO;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String token;
    private String newPassword;
    private String recaptchaToken;
}


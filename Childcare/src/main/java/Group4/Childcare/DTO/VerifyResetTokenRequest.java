package Group4.Childcare.DTO;

import lombok.Data;

@Data
public class VerifyResetTokenRequest {
    private String email;
    private String token;
}


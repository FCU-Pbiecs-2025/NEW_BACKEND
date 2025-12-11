package Group4.Childcare.Model;

import lombok.Data;

@Data
public class EmailRequest {
    private String to;
    private String subject;
    private String text;
    private String htmlContent;
    private String username;
    private String resetToken;
    private String applicantName;
    private String status;
    private String details;
}

package Group4.Childcare.DTO;

import lombok.Data;
import java.util.UUID;

@Data
public class UserSummaryDTO {
    private UUID userID;
    private String account;
    private String institutionName;
    private Byte permissionType;
    private Byte accountStatus;
}

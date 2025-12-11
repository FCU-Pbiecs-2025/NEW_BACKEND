package Group4.Childcare.DTO;

import lombok.Data;
import java.util.UUID;

@Data
public class ManualAdmissionRequest {
    private UUID applicationId;
    private String nationalId;
    private UUID classId;
}


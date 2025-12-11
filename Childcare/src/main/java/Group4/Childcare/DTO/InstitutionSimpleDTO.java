package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionSimpleDTO {
  //個案管理機構名稱查詢條件
    private UUID institutionID;
    private String institutionName;
}


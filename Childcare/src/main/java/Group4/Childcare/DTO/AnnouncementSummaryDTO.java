package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementSummaryDTO {
    private UUID announcementID;
    private String title;
    private String content;
    private LocalDate startDate;
    private String attachmentPath;
    private Byte type;

    // Constructor for backward compatibility (without attachmentPath, type)
    public AnnouncementSummaryDTO(UUID announcementID, String title, String content, LocalDate startDate) {
        this.announcementID = announcementID;
        this.title = title;
        this.content = content;
        this.startDate = startDate;
    }

    // Constructor for backward compatibility (without type)
    public AnnouncementSummaryDTO(UUID announcementID, String title, String content, LocalDate startDate, String attachmentPath) {
        this.announcementID = announcementID;
        this.title = title;
        this.content = content;
        this.startDate = startDate;
        this.attachmentPath = attachmentPath;
    }
}

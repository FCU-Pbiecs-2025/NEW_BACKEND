package Group4.Childcare.DTO;

import java.util.List;

public class RevokeDetailResponse {
    public RevokeApplicationDTO revokeInfo; // summary of cancellation
    public List<ApplicationParticipantDTO> parents; // participantType == 2
    public ApplicationParticipantDTO child; // application + participants info (child data)

    public RevokeDetailResponse() {}

    public RevokeDetailResponse(RevokeApplicationDTO revokeInfo, List<ApplicationParticipantDTO> parents, ApplicationParticipantDTO child) {
        this.revokeInfo = revokeInfo;
        this.parents = parents;
        this.child = child;
    }

    public RevokeApplicationDTO getRevokeInfo() { return revokeInfo; }
    public List<ApplicationParticipantDTO> getParents() { return parents; }
    public ApplicationParticipantDTO getChild() { return child; }

    public void setRevokeInfo(RevokeApplicationDTO revokeInfo) { this.revokeInfo = revokeInfo; }
    public void setParents(List<ApplicationParticipantDTO> parents) { this.parents = parents; }
    public void setChild(ApplicationParticipantDTO child) { this.child = child; }
}

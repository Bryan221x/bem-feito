package com.bemfeito.api.requests.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.bemfeito.api.requests.entity.RequestStatus;
import com.bemfeito.api.user.entity.UserStatus;

public final class RequestOutput {
    private RequestOutput() {}
    public record Submitted(Long id, RequestStatus status, LocalDateTime createdAt) {}
    public record Summary(Long id,String type,String institutionName,String responsibleName,LocalDateTime createdAt,RequestStatus status) {}
    public record Reference(Long id,String name) {}
    public record Location(Long id,String city,String state) {}
    public record SearchResult(Long id,String displayName,String category,List<Location> locations) {}
    public record UnitDetail(Long id,RequestInput.MainUnit data,String city,String state) {}
    public record Detail(Summary summary,String categoryName,RequestInput.Registration registration,List<UnitDetail> units,
            RequestInput.Access access,Long requesterUserId,UserStatus requesterUserStatus,
            Long reviewedByUserId,LocalDateTime reviewedAt,String reviewNotes) {}
    public record Invitation(Long userId,String deliveryMode,String activationUrl,LocalDateTime expiresAt) {
        @Override public String toString() { return "Invitation[redacted]"; }
    }
    public record Reviewed(Long id,RequestStatus status,Long institutionId,Long userId,Invitation invitation) {}
    public record Inspection(String state,String name,String maskedEmail,LocalDateTime expiresAt) {}
    public record Dashboard(long pendingRequests,List<Summary> requests) {}
}

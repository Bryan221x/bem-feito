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
    public static final class Invitation {
        private final Long userId;
        private String deliveryMode;
        private String activationUrl;
        private final LocalDateTime expiresAt;
        private final String purpose;
        public Invitation(Long userId,String mode,String url,LocalDateTime expiresAt) { this(userId,mode,url,expiresAt,"ACTIVATION"); }
        public Invitation(Long userId,String mode,String url,LocalDateTime expiresAt,String purpose) {
            this.userId=userId;this.deliveryMode=mode;this.activationUrl=url;this.expiresAt=expiresAt;this.purpose=purpose;
        }
        @com.fasterxml.jackson.annotation.JsonProperty public Long userId() { return userId; }
        @com.fasterxml.jackson.annotation.JsonProperty public String deliveryMode() { return deliveryMode; }
        @com.fasterxml.jackson.annotation.JsonProperty public String activationUrl() { return activationUrl; }
        @com.fasterxml.jackson.annotation.JsonProperty public LocalDateTime expiresAt() { return expiresAt; }
        @com.fasterxml.jackson.annotation.JsonProperty public String purpose() { return purpose; }
        public void delivered(String status) { deliveryMode=status;if(status.equals("EMAIL_SENT")) activationUrl=null; }
        @Override public String toString() { return "Invitation[redacted]"; }
    }
    public record Reviewed(Long id,RequestStatus status,Long institutionId,Long userId,Invitation invitation) {}
    public record Inspection(String state,String name,String maskedEmail,LocalDateTime expiresAt) {}
    public record Dashboard(long pendingRequests,List<Summary> requests) {}
}

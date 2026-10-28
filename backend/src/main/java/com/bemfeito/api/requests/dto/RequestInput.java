package com.bemfeito.api.requests.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CNPJ;
import com.bemfeito.api.common.domain.ContactType;
import com.bemfeito.api.common.validation.*;
import com.bemfeito.api.institution.membership.entity.MembershipRole;
import java.util.*;

/** Public contracts intentionally contain no reviewer/user IDs or authorization roles. */
public final class RequestInput {
    private RequestInput() {}
    public static String text(String s) { return s==null || s.isBlank()?null:s.trim(); }
    public static String email(String s) { return s==null?null:s.trim().toLowerCase(Locale.ROOT); }
    public static String digits(String s) { return text(s)==null?null:s.replaceAll("[^0-9]", ""); }
    public static String phone(String s) { return text(s)==null?null:(s.trim().startsWith("+")?"+":"")+digits(s); }

    public record Responsible(@NotBlank @Size(max=150) String name, @NotBlank @Email @Size(max=255) String email,
            @NotBlank @Pattern(regexp="\\+?[0-9]{8,15}") String phone, @NotBlank @Size(max=100) String role) {
        public Responsible { name=text(name);email=RequestInput.email(email);phone=RequestInput.phone(phone);role=text(role); }
    }
    public record Contact(@NotNull ContactType type, @NotBlank @Size(max=255) String value,
            @Size(max=100) String label, @NotNull Boolean isPrimary) {
        public Contact { value=type==ContactType.EMAIL?email(value):phone(value);label=text(label); }
    }
    public record MainUnit(@NotBlank @Size(max=150) String name,
            @Pattern(regexp="[0-9]{14}") @CNPJ String document, @Utf8Text String description,
            @NotNull @Positive Long cityId, @NotBlank @Pattern(regexp="[0-9]{8}") String postalCode,
            @NotBlank @Size(max=180) String street, @NotBlank @Size(max=30) String number,
            @Size(max=120) String complement, @NotBlank @Size(max=120) String neighborhood,
            @NotNull @Size(max=30) List<@NotNull @Valid Contact> contacts) {
        public MainUnit { name=text(name);document=digits(document);description=text(description);postalCode=digits(postalCode);
            street=text(street);number=text(number);complement=text(complement);neighborhood=text(neighborhood); }
    }
    public record Registration(@NotNull @Positive Long categoryId, @NotBlank @Size(max=150) String displayName,
            @NotBlank @Size(max=200) String legalName, @NotBlank @Pattern(regexp="[0-9]{14}") @CNPJ String cnpj,
            @Utf8Text String objective,@Utf8Text String description,@Utf8Text String history,@Size(max=255) @HttpUrl String website,
            @NotNull @Valid Responsible responsible, @NotEmpty @Size(max=30) List<@NotNull @Valid Contact> publicContacts,
            @NotNull @Valid MainUnit mainUnit) {
        public Registration { displayName=text(displayName);legalName=text(legalName);cnpj=digits(cnpj);
            objective=text(objective);description=text(description);history=text(history);website=text(website); }
    }
    public record Access(@NotNull @Positive Long institutionId,@NotBlank @Size(max=150) String requesterName,
            @NotBlank @Email @Size(max=255) String requesterEmail,
            @NotBlank @Pattern(regexp="\\+?[0-9]{8,15}") String requesterPhone,
            @NotBlank @Size(max=100) String requesterRole,@Utf8Text String justification) {
        public Access { requesterName=text(requesterName);requesterEmail=email(requesterEmail);requesterPhone=phone(requesterPhone);
            requesterRole=text(requesterRole);justification=text(justification); }
    }
    public record Decision(@Utf8Text String notes, MembershipRole membershipRole) {
        public Decision { notes=text(notes); }
    }
    public record Inspect(@NotBlank @Size(max=128) String token) {
        @Override public String toString() { return "Inspect[redacted]"; }
    }
    public record Accept(@NotBlank @Size(max=128) String token,@NotNull @Size(min=12,max=72) String password,
            @NotNull String confirmPassword) {
        @Override public String toString() { return "Accept[redacted]"; }
    }
}

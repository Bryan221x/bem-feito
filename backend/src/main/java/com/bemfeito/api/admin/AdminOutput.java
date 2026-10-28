package com.bemfeito.api.admin;
import java.time.LocalDateTime;
import java.util.List;
import com.bemfeito.api.common.domain.InstitutionStatus;
public final class AdminOutput {
    private AdminOutput() {}
    public record InstitutionRow(Long id,String displayName,String cnpj,InstitutionStatus status,LocalDateTime createdAt,long unitCount,long responsibleCount,String location) {}
    public record CityCount(String city,String state,long count) {}
    public record Summary(long institutions,long managers,long units,List<CityCount> institutionsByCity) {}
}

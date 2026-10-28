package com.bemfeito.api.common.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashSet;
import java.util.Set;

/** Registra presença separadamente do valor, inclusive null explícito. */
public abstract class PatchRequest {
    private final Set<String> present = new HashSet<>();
    protected void mark(String field) { present.add(field); }
    public boolean has(String field) { return present.contains(field); }
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Campo desconhecido: " + field);
    }
}

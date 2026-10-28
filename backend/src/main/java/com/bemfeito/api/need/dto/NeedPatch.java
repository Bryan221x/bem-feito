package com.bemfeito.api.need.dto;

import com.bemfeito.api.common.domain.NeedPriority;
import com.bemfeito.api.common.domain.NeedStatus;
import com.bemfeito.api.common.dto.PatchRequest;
import java.math.BigDecimal;

public class NeedPatch extends PatchRequest {
    private Long categoryId;
    private String title;
    private String description;
    private BigDecimal quantity;
    private String unitLabel;
    private NeedPriority priority;
    private NeedStatus status;
    @com.fasterxml.jackson.annotation.JsonSetter("categoryId")
    public void setCategoryId(Long value) { mark("categoryId"); this.categoryId=value; }
    public Long getCategoryId() { return categoryId; }
    @com.fasterxml.jackson.annotation.JsonSetter("title")
    public void setTitle(String value) { mark("title"); this.title=value; }
    public String getTitle() { return title; }
    @com.fasterxml.jackson.annotation.JsonSetter("description")
    public void setDescription(String value) { mark("description"); this.description=value; }
    public String getDescription() { return description; }
    @com.fasterxml.jackson.annotation.JsonSetter("quantity")
    public void setQuantity(BigDecimal value) { mark("quantity"); this.quantity=value; }
    public BigDecimal getQuantity() { return quantity; }
    @com.fasterxml.jackson.annotation.JsonSetter("unitLabel")
    public void setUnitLabel(String value) { mark("unitLabel"); this.unitLabel=value; }
    public String getUnitLabel() { return unitLabel; }
    @com.fasterxml.jackson.annotation.JsonSetter("priority")
    public void setPriority(NeedPriority value) { mark("priority"); this.priority=value; }
    public NeedPriority getPriority() { return priority; }
    @com.fasterxml.jackson.annotation.JsonSetter("status")
    public void setStatus(NeedStatus value) { mark("status"); this.status=value; }
    public NeedStatus getStatus() { return status; }
}

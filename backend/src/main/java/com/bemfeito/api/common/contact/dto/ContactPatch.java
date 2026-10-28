package com.bemfeito.api.common.contact.dto;

import com.bemfeito.api.common.domain.ContactType;
import com.bemfeito.api.common.dto.PatchRequest;

public class ContactPatch extends PatchRequest {
    private ContactType type;
    private String value;
    private String label;
    private Boolean isPrimary;
    private Boolean isPublic;
    private Integer displayOrder;
    @com.fasterxml.jackson.annotation.JsonSetter("type")
    public void setType(ContactType value) { mark("type"); this.type=value; }
    public ContactType getType() { return type; }
    @com.fasterxml.jackson.annotation.JsonSetter("value")
    public void setValue(String value) { mark("value"); this.value=value; }
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonSetter("label")
    public void setLabel(String value) { mark("label"); this.label=value; }
    public String getLabel() { return label; }
    @com.fasterxml.jackson.annotation.JsonSetter("isPrimary")
    public void setIsPrimary(Boolean value) { mark("isPrimary"); this.isPrimary=value; }
    public Boolean getIsPrimary() { return isPrimary; }
    @com.fasterxml.jackson.annotation.JsonSetter("isPublic")
    public void setIsPublic(Boolean value) { mark("isPublic"); this.isPublic=value; }
    public Boolean getIsPublic() { return isPublic; }
    @com.fasterxml.jackson.annotation.JsonSetter("displayOrder")
    public void setDisplayOrder(Integer value) { mark("displayOrder"); this.displayOrder=value; }
    public Integer getDisplayOrder() { return displayOrder; }
}

package com.bemfeito.api.institution.history.dto;

import com.bemfeito.api.common.dto.PatchRequest;

public class HistoryImagePatch extends PatchRequest {
    private String imageUrl;
    private String altText;
    private String caption;
    private Integer displayOrder;
    @com.fasterxml.jackson.annotation.JsonSetter("imageUrl")
    public void setImageUrl(String value) { mark("imageUrl"); this.imageUrl=value; }
    public String getImageUrl() { return imageUrl; }
    @com.fasterxml.jackson.annotation.JsonSetter("altText")
    public void setAltText(String value) { mark("altText"); this.altText=value; }
    public String getAltText() { return altText; }
    @com.fasterxml.jackson.annotation.JsonSetter("caption")
    public void setCaption(String value) { mark("caption"); this.caption=value; }
    public String getCaption() { return caption; }
    @com.fasterxml.jackson.annotation.JsonSetter("displayOrder")
    public void setDisplayOrder(Integer value) { mark("displayOrder"); this.displayOrder=value; }
    public Integer getDisplayOrder() { return displayOrder; }
}

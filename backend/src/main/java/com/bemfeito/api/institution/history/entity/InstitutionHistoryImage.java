package com.bemfeito.api.institution.history.entity;

import com.bemfeito.api.common.entity.IdentifiedEntity;
import com.bemfeito.api.institution.entity.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="institution_history_images")
public class InstitutionHistoryImage extends IdentifiedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="institution_id", nullable=false)
    private Institution institution;

    @Column(name="image_url", length=500, nullable=false)
    private String imageUrl;

    @Column(name="alt_text", length=255, nullable=false)
    private String altText;

    @Column(name="caption", length=255, nullable=true)
    private String caption;

    @Column(name="display_order",nullable=false)
    private Integer displayOrder;

    @org.hibernate.annotations.CreationTimestamp @Column(name="created_at",nullable=false,updatable=false)
    private LocalDateTime createdAt;

    public InstitutionHistoryImage() {}
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution value) { this.institution = value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { this.imageUrl = value; }
    public String getAltText() { return altText; }
    public void setAltText(String value) { this.altText = value; }
    public String getCaption() { return caption; }
    public void setCaption(String value) { this.caption = value; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer value) { this.displayOrder = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

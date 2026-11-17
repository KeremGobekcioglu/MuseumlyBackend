package com.kg.museumly.backend.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "artworks")
public class Artwork {
    @Id
    private Long id;
    private String source;
    private String sourceId;
    private String department;
    private String status;

    private String title;
    private String objectName;
    private String artistDisplayName;
    private String artistDisplayBio;
    private String objectDate;
    private Integer beginYear;
    private String classification;
    private String medium;
    private String dimensions;
    private String creditLine;
    private String culture;
    private String period;

    private String imageUrl;
    private String imageSmallUrl;
    private Integer imageWidth;

    @Column(insertable = false, updatable = false)
    private Integer eraYear;
    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getDepartment() {
        return department;
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getArtistDisplayName() {
        return artistDisplayName;
    }

    public String getArtistDisplayBio() {
        return artistDisplayBio;
    }

    public String getObjectDate() {
        return objectDate;
    }

    public Integer getBeginYear() {
        return beginYear;
    }

    public String getClassification() {
        return classification;
    }

    public String getMedium() {
        return medium;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getCreditLine() {
        return creditLine;
    }

    public String getCulture() {
        return culture;
    }

    public String getPeriod() {
        return period;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageSmallUrl() {
        return imageSmallUrl;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    private Integer imageHeight;

    protected Artwork()
    {}
}

package com.example.purestation;

public class Cloth {
    private String imageUrl;
    private String color;
    private String material;
    private String season;
    private String washing;
    private String caution;

    public Cloth() {}

    public Cloth(String imageUrl, String color, String material, String season, String washing, String caution) {
        this.imageUrl = imageUrl;
        this.color = color;
        this.material = material;
        this.season = season;
        this.washing = washing;
        this.caution = caution;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getWashing() {
        return washing;
    }

    public void setWashing(String washing) {
        this.washing = washing;
    }

    public String getCaution() {
        return caution;
    }

    public void setCaution(String caution) {
        this.caution = caution;
    }

}

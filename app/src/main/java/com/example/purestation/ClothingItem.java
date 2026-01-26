package com.example.purestation;

/*
    ClothingItem 클래스: 옷의 색상, 재질, 모양 정보를 저장
*/
public class ClothingItem {
    private String id;
    private String category;
    private String caution;
    private String color;
    private String material;
    private String season;
    private String shape;
    private String url;
    private String washing;
    private int score;

    public ClothingItem() {
    }

    public ClothingItem(String id, String category, String color, String material, String shape, String url) {
        this.id = id;
        this.category = category;
        this.color = color;
        this.material = material;
        this.shape = shape;
        this.url = url;
    }

    public ClothingItem(String id, String category, String caution, String color, String material, String season, String shape, String url, String washing, int score) {
        this.id = id;
        this.category = category;
        this.caution = caution;
        this.color = color;
        this.material = material;
        this.season = season;
        this.shape = shape;
        this.url = url;
        this.washing = washing;
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCaution() {
        return caution;
    }

    public void setCaution(String caution) {
        this.caution = caution;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWashing() {
        return washing;
    }

    public void setWashing(String washing) {
        this.washing = washing;
    }
}
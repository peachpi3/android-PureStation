package com.example.purestation;

public class Arduino {
    private int humidity;
    private int temperature;
    private int humiIsWaterFull;
    private int dehumiIsWaterFull;

    public Arduino() {
    }

    public int getHumiIsWaterFull() {
        return humiIsWaterFull;
    }

    public void setHumiIsWaterFull(int humiIsWaterFull) {
        this.humiIsWaterFull = humiIsWaterFull;
    }

    public int getDehumiIsWaterFull() {
        return dehumiIsWaterFull;
    }

    public void setDehumiIsWaterFull(int dehumiIsWaterFull) {
        this.dehumiIsWaterFull = dehumiIsWaterFull;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
}

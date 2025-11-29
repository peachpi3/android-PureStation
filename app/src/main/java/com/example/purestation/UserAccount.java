package com.example.purestation;

public class UserAccount {
    // 회원가입 사용자 정보 모델 (데이터베이스에 담기 위함)
    private String email, passwd, name, phone, arduinoId, gender;

    public UserAccount() {} // 빈 생성자

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getArduinoId() {
        return arduinoId;
    }

    public void setArduinoId(String arduinoId) {
        this.arduinoId = arduinoId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}

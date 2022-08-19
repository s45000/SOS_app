package com.allenlab.sosos;

public class Note {

    private int sensitive = 1;
    private String password = "0000";
    private String[] phonebook = {"","",""};
    private boolean tutorial = true;

    public void setSensitive(int s) {
        this.sensitive = s;
    }

    public void setPassword(String p) {
        this.password = p;
    }

    public void setFirstPhone(String phone) {
        this.phonebook[0] = phone;
    }
    public void setSecondPhone(String phone) {
        this.phonebook[1] = phone;
    }
    public void setThirdPhone(String phone) {
        this.phonebook[2] = phone;
    }

    public void setTutorial(boolean tut){
        this.tutorial = tut;
    }

    public int getSensitive() {
        return this.sensitive;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFirstPhone() {
        return this.phonebook[0];
    }
    public String getSecondPhone() {
        return this.phonebook[1];
    }
    public String getThirdPhone() {
        return this.phonebook[2];
    }

    public boolean getTutorial(){
        return this.tutorial;
    }
}

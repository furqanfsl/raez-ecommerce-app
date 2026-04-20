package com.raez.model;

public class SmtpSettings {
    public String  host        = "";
    public int     port        = 587;
    public String  username    = "";
    public String  password    = "";
    public String  fromAddress = "";
    public String  fromName    = "RAEZ";
    public boolean useTls      = true;
    public boolean isEnabled   = false;
}

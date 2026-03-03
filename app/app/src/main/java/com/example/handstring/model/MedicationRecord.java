package com.example.handstring.model;

import java.util.Date;

public class MedicationRecord {
    private String medicineName;
    private String dosage;
    private Date scheduledTime;
    private Date actualTime;
    private boolean taken;

    public MedicationRecord(String medicineName, String dosage, Date scheduledTime, Date actualTime, boolean taken) {
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.scheduledTime = scheduledTime;
        this.actualTime = actualTime;
        this.taken = taken;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Date getActualTime() {
        return actualTime;
    }

    public void setActualTime(Date actualTime) {
        this.actualTime = actualTime;
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }
} 
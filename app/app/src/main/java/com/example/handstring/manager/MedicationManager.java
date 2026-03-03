package com.example.handstring.manager;

import com.example.handstring.model.MedicationRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MedicationManager {
    private static MedicationManager instance;
    private final List<MedicationRecord> medicationRecords;
    private final List<MedicationUpdateListener> listeners;

    public interface MedicationUpdateListener {
        void onMedicationUpdated(List<MedicationRecord> records);
    }

    private MedicationManager() {
        medicationRecords = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public static synchronized MedicationManager getInstance() {
        if (instance == null) {
            instance = new MedicationManager();
        }
        return instance;
    }

    public void addListener(MedicationUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(MedicationUpdateListener listener) {
        listeners.remove(listener);
    }

    public void addMedicationRecord(MedicationRecord record) {
        medicationRecords.add(record);
        notifyListeners();
    }

    public void updateMedicationStatus(String medicineName, Date scheduledTime, boolean taken, Date actualTime) {
        for (MedicationRecord record : medicationRecords) {
            if (record.getMedicineName().equals(medicineName) && 
                record.getScheduledTime().equals(scheduledTime)) {
                record.setTaken(taken);
                record.setActualTime(actualTime);
                notifyListeners();
                break;
            }
        }
    }

    public List<MedicationRecord> getMedicationRecords() {
        return new ArrayList<>(medicationRecords);
    }

    public List<MedicationRecord> getMedicationRecordsByDate(Date date) {
        List<MedicationRecord> result = new ArrayList<>();
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date);
        
        for (MedicationRecord record : medicationRecords) {
            cal2.setTime(record.getScheduledTime());
            if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                result.add(record);
            }
        }
        return result;
    }

    private void notifyListeners() {
        for (MedicationUpdateListener listener : listeners) {
            listener.onMedicationUpdated(new ArrayList<>(medicationRecords));
        }
    }

    public void clearRecords() {
        medicationRecords.clear();
        notifyListeners();
    }
} 
package com.example.handstring.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.handstring.R;
import com.example.handstring.model.MedicationRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {
    private List<MedicationRecord> medicationRecords;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MedicationAdapter(List<MedicationRecord> medicationRecords) {
        this.medicationRecords = medicationRecords;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        MedicationRecord record = medicationRecords.get(position);
        holder.medicineName.setText(record.getMedicineName());
        holder.dosage.setText(record.getDosage());
        holder.time.setText(timeFormat.format(record.getScheduledTime()));
        holder.status.setText(record.isTaken() ? "已服用" : "未服用");
        holder.status.setTextColor(holder.itemView.getContext().getResources()
                .getColor(record.isTaken() ? R.color.green : R.color.red));
    }

    @Override
    public int getItemCount() {
        return medicationRecords.size();
    }

    public void updateData(List<MedicationRecord> newRecords) {
        this.medicationRecords = newRecords;
        notifyDataSetChanged();
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView medicineName;
        TextView dosage;
        TextView time;
        TextView status;

        MedicationViewHolder(View itemView) {
            super(itemView);
            medicineName = itemView.findViewById(R.id.medicineName);
            dosage = itemView.findViewById(R.id.dosage);
            time = itemView.findViewById(R.id.time);
            status = itemView.findViewById(R.id.status);
        }
    }
} 
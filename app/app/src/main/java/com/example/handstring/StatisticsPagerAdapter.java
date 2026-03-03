package com.example.handstring;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;


public class StatisticsPagerAdapter extends FragmentStateAdapter {

    public StatisticsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DailyStatisticsFragment();
            case 1:
                return new WeeklyStatisticsFragment();
            case 2:
                return new MonthlyStatisticsFragment();
            default:
                return new DailyStatisticsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
} 
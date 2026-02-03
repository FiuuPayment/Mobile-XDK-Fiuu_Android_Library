package com.fiuu.xdkandroid;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fiuu.xdkandroid.fragments.PaymentFragment;
import com.fiuu.xdkandroid.fragments.MerchantFragment;
import com.fiuu.xdkandroid.fragments.BillingFragment;

public class MyViewPagerAdapter  extends FragmentStateAdapter {
    public MyViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                return new PaymentFragment();
            case 1:
                return new BillingFragment();
            case 2:
                return new MerchantFragment();
            default:
                return new PaymentFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
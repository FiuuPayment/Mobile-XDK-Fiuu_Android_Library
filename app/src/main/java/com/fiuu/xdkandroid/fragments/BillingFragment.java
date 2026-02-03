package com.fiuu.xdkandroid.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fiuu.xdkandroid.R;
import com.fiuu.xdkandroid.SharedViewModel;
import com.fiuu.xdkandroid.models.Billing;
import com.fiuu.xdkandroid.models.Payment;
import com.google.android.material.textfield.TextInputEditText;

public class BillingFragment extends Fragment {
    private SharedViewModel viewModel;

    private Billing modelData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.billing_tab, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        modelData = viewModel.getBillingData().getValue();
        descInput(view);
        nameInput(view);
        emailInput(view);
        tellInput(view);
    }

    private void descInput(View v1) {

        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_billdesc);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getDescription());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setDescription(textString);
                viewModel.setBillingData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void nameInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_billname);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getPayername());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setPayername(textString);
                viewModel.setBillingData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void emailInput(View v1) {

        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_billemail);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getPayeremail());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setPayeremail(textString);
                viewModel.setBillingData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void tellInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_biltel);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getPayermobile());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setPayermobile(textString);
                viewModel.setBillingData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
}

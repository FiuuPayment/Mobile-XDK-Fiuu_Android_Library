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
import com.fiuu.xdkandroid.models.Merchant;
import com.fiuu.xdkandroid.models.Payment;
import com.google.android.material.textfield.TextInputEditText;

public class MerchantFragment extends Fragment {
    private SharedViewModel viewModel;
    private Merchant modelData;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.merchant_tab, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        modelData = viewModel.getMerchantData().getValue();
        usernameInput(view);
        passwordInput(view);
        appnameInput(view);
        merchantidInput(view);
        vkeyInput(view);
    }

    private void usernameInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_username);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getUsername());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setUsername(textString);
                viewModel.setMerchantData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void passwordInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_password);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getPassword());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setPassword(textString);
                viewModel.setMerchantData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void appnameInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_app_name);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getAppname());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setAppname(textString);
                viewModel.setMerchantData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void merchantidInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_merchant_ID);
        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getMerchantid());
        }
        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setMerchantid(textString);
                viewModel.setMerchantData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
    private void vkeyInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_verification_key);

        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getVerificationKey());
        }

        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setVerificationKey(textString);
                viewModel.setMerchantData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }
}

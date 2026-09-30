package com.fiuu.xdkandroid.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fiuu.xdkandroid.R;
import com.fiuu.xdkandroid.SharedViewModel;
import com.fiuu.xdkandroid.models.Merchant;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

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
        coreEnvDropDown(view);
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

    private void coreEnvDropDown(View v1) {
        MaterialAutoCompleteTextView edtMpCoreEnv = v1.findViewById(R.id.edt_mp_core_env);

        List<CoreEnvItem> envList = Arrays.asList(
                new CoreEnvItem("2", "2 - Production - V2"),
                new CoreEnvItem("4", "4 - Sandbox - V2"),
                new CoreEnvItem("3", "3 - UAT - V2"),
                new CoreEnvItem("1", "1 - Production - V1")
        );

        ArrayAdapter<CoreEnvItem> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                envList
        );
        edtMpCoreEnv.setAdapter(adapter);

        String currentEnv = modelData != null ? modelData.getCoreEnv() : "2";
        if (currentEnv == null || currentEnv.isEmpty()) {
            currentEnv = "2";
        }
        for (CoreEnvItem item : envList) {
            if (item.code.equals(currentEnv)) {
                edtMpCoreEnv.setText(item.label, false);
                break;
            }
        }

        edtMpCoreEnv.setOnItemClickListener((parent, view, position, id) -> {
            CoreEnvItem selected = (CoreEnvItem) parent.getItemAtPosition(position);
            if (selected != null && modelData != null) {
                modelData.setCoreEnv(selected.code);
                viewModel.setMerchantData(modelData);
            }
        });
    }

    private static class CoreEnvItem {
        final String code;
        final String label;

        CoreEnvItem(String code, String label) {
            this.code = code;
            this.label = label;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}

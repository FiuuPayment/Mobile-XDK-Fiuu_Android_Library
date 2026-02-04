package com.fiuu.xdkandroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fiuu.xdkandroid.SharedViewModel;
import com.fiuu.xdkandroid.models.Country;
import com.fiuu.xdkandroid.R;
import com.fiuu.xdkandroid.models.Payment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.molpay.molpayxdk.MOLPayActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class PaymentFragment extends Fragment {
    private SharedViewModel viewModel;
    private Payment modelData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.payment_tab, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        modelData = viewModel.getPaymentData().getValue();

        countryDropDown(view);
        channelDropDown(view);
        expressModeToggle(view);
        textInput(view);
        transactionResult(view);
    }

    private void countryDropDown(View v1) {
        String defaultCountry = "MY";
        String defaultCurrency = "MYR";
        MaterialAutoCompleteTextView edtMpCountry  = v1.findViewById(R.id.edt_mp_country);
        TextInputEditText edtMpCurrency = v1.findViewById(R.id.edt_mp_currency);

        List<Country> countries = Arrays.asList(
                new Country("MY", "MYR"),
                new Country("SG", "SGD")
        );

        ArrayAdapter<Country> adapter = new ArrayAdapter<Country>(
                requireContext(),
                android.R.layout.simple_list_item_1,  // Single TextView layout
                countries
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // Get the view from parent (handles recycling)
                View view = super.getView(position, convertView, parent);

                Country env = getItem(position);
                if (env != null) {
                    TextView textView = view.findViewById(android.R.id.text1);
                    textView.setText(env.getName()); // Show only name
                }
                return view;
            }
        };

        edtMpCountry.setAdapter(adapter);
        edtMpCountry.setText(defaultCountry, false);
        edtMpCurrency.setText(defaultCurrency);
        modelData.setCountry(defaultCountry);
        modelData.setCurrency(defaultCurrency);
        viewModel.setPaymentData(modelData);

        edtMpCountry.setOnItemClickListener((parent, v2, position, id) -> {
            Country selected = adapter.getItem(position);
            if (selected != null) {
                String mp_country = selected.getName();
                String mp_currency = selected.getCurrency();
                edtMpCurrency.setText(mp_currency);
                modelData.setCountry(mp_country);
                modelData.setCurrency(mp_currency);
                viewModel.setPaymentData(modelData);
            }
        });
    }
    private void channelDropDown(View v1){
        MaterialAutoCompleteTextView edtMpChannel  = v1.findViewById(R.id.edt_mp_channel);
        String[] channels = new String[]{"multi","TNG-EWALLET","maybank2u"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, channels);
        edtMpChannel .setAdapter(adapter);

        edtMpChannel.setText(modelData.getChannel(), false);

// Listen for selection
        edtMpChannel.setOnItemClickListener((parent, v2, position, id) -> {
            String mp_channel = adapter.getItem(position);
            modelData.setChannel(mp_channel);
            viewModel.setPaymentData(modelData);
        });

    }
    private void expressModeToggle(View v1){
        SwitchMaterial switchMaterial  = v1.findViewById(R.id.switch_material);

        switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            modelData.setIsExpressMode(isChecked);
            viewModel.setPaymentData(modelData);
        });

    }
    private void textInput(View v1) {
        TextInputEditText editTextField = v1.findViewById(R.id.edt_mp_amount);

        CharSequence current = editTextField.getText();
        if (current == null || current.length() == 0) {
            editTextField.setText(modelData.getAmount());
        }

        editTextField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textString = s.toString();
                modelData.setAmount(textString);
                viewModel.setPaymentData(modelData);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }


    private void transactionResult(View v1) {
        TextView tw = v1.findViewById(R.id.resultTV);

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), text -> {
            if (text != null)
                try {
                    tw.setText(
                            text.trim().startsWith("{")
                                    ? new JSONObject(text).toString(4)
                                    : new JSONArray(text).toString(4)
                    );
                } catch (JSONException e) {
                    tw.setText(text); // fallback if not valid JSON
                }
        });

    }


}

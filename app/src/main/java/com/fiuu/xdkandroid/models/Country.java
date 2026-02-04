package com.fiuu.xdkandroid.models;

public class Country {
    private final String name;
    private final String currency;

    public Country(String name, String id) {
        this.name = name;
        this.currency = id;
    }

    public String getName() { return name; }
    public String getCurrency() { return currency; }

    @Override
    public String toString() {
        return name; // This shows in the collapsed AutoCompleteTextView
    }
}
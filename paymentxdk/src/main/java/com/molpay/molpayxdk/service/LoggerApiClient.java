package com.molpay.molpayxdk.service;

import com.google.gson.Gson;
import com.molpay.molpayxdk.models.LogMessage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoggerApiClient {

    private static final String BASE_URL = "https://vtapi.merchant.razer.com";
    private static final String ENDPOINT = "/api/mobile/vt/logs";

    public static void sendLog(LogMessage logMessage) {
        HttpURLConnection urlConnection = null;

        try {
            // Create the URL
            URL url = new URL(BASE_URL + ENDPOINT);

            // Open connection
            urlConnection = (HttpURLConnection) url.openConnection();

            // Set request method to POST
            urlConnection.setRequestMethod("POST");

            // Set headers
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");

            // Enable writing to the connection
            urlConnection.setDoOutput(true);

            // Set timeout values
            urlConnection.setConnectTimeout(60000);  // 60 seconds
            urlConnection.setReadTimeout(60000);     // 60 seconds

            // Convert LogMessage object to JSON
            Gson gson = new Gson();
            String jsonInputString = gson.toJson(logMessage);

            // Write JSON to request body
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Success: Handle the response if necessary
                System.out.println("Log sent successfully!");
            } else {
                // Error: Handle different response codes
                System.out.println("Failed to send log. Response Code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                // Disconnect the connection
                urlConnection.disconnect();
            }
        }
    }
}

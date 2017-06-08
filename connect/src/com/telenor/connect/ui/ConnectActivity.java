package com.telenor.connect.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.telenor.connect.ConnectSdk;
import com.telenor.connect.id.ConnectTokens;

import java.util.Map;

public class ConnectActivity extends AbstractConnectSdkActivity {

    private final Gson gson = new Gson();

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, ConnectWebFragment.class);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        singleFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onSuccess(Object successData) {
        if (ConnectSdk.isConfidentialClient()) {
            Map<String, String> authCodeData = (Map<String, String>) successData;
            Intent intent = new Intent();
            for (Map.Entry<String, String> entry : authCodeData.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else {
            ConnectTokens connectTokens = (ConnectTokens) successData;
            Intent data = new Intent();
            String json = gson.toJson(connectTokens);
            data.putExtra(ConnectSdk.EXTRA_CONNECT_TOKENS, json);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        ConnectSdk.getSdkProfile().onFinishAuthorization(true);
    }

    @Override
    public void onError(Object errorData) {
        Intent intent = new Intent();
        Map<String, String> authCodeData = (Map<String, String>) errorData;
        for (Map.Entry<String, String> entry : authCodeData.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
        ConnectSdk.getSdkProfile().onFinishAuthorization(false);
    }
}

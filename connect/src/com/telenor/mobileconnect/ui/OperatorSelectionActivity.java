package com.telenor.mobileconnect.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.telenor.connect.ConnectSdk;
import com.telenor.connect.SdkProfile;
import com.telenor.connect.ui.AbstractConnectSdkActivity;
import com.telenor.connect.ui.ConnectActivity;
import com.telenor.connect.utils.ConnectUtils;
import com.telenor.mobileconnect.MobileConnectSdkProfile;

import java.util.List;
import java.util.Map;

public class OperatorSelectionActivity extends AbstractConnectSdkActivity {

    static final String MCC_MNC_PARAM_NAME = "mcc_mnc";
    static final String MCC_MNC_PARAM_SEP = "_";
    static final String SUBSCRIBER_ID_QUERY_PARAM_NAME = "subscriber_id";

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, OperatorSelectionWebFragment.class);
    }

    @Override
    public void onSuccess(Object successData) {

        Uri uri = (Uri) successData;
        String[] mccAndMnc = uri.getQueryParameter(MCC_MNC_PARAM_NAME).split(MCC_MNC_PARAM_SEP);

        @SuppressWarnings("unchecked")
        Map<String, String> parameters = (Map<String, String>) this
                .getIntent()
                .getExtras()
                .get(ConnectUtils.PARAM_SNAPHOT_EXTRA);
        @SuppressWarnings("unchecked")
        List<String> uiLocales = (List<String>) this
                .getIntent()
                .getExtras()
                .get(ConnectUtils.UI_LOCALES_SNAPHOT_EXTRA);
        final int requestCode = this
                .getIntent()
                .getExtras()
                .getInt(ConnectUtils.REQUEST_CODE_EXTRA);
        final int customLoadingLayout = this
                .getIntent()
                .getExtras()
                .getInt(ConnectUtils.CUSTOM_LOADING_SCREEN_EXTRA, ConnectSdk.NO_CUSTOM_LAYOUT);
        MobileConnectSdkProfile sdkProfile = (MobileConnectSdkProfile) ConnectSdk.getSdkProfile();
        sdkProfile.onStartAuthorization(
                parameters,
                uiLocales,
                mccAndMnc[0],
                mccAndMnc[1],
                uri.getQueryParameter(SUBSCRIBER_ID_QUERY_PARAM_NAME),
                new SdkProfile.OnStartAuthorizationCallback() {
                    @Override
                    public void onSuccess(Intent nextIntent) {
                        if (!isConnectActivity(nextIntent)) {
                            throw new RuntimeException("Wrong intent.");
                        }
                        Activity activity = OperatorSelectionActivity.this;
                        if (customLoadingLayout != ConnectSdk.NO_CUSTOM_LAYOUT) {
                            nextIntent.putExtra(
                                    ConnectUtils.CUSTOM_LOADING_SCREEN_EXTRA,
                                    customLoadingLayout);
                        }
                        activity.startActivityForResult(nextIntent, requestCode);
                    }

                    @Override
                    public void onError() {
                        cancelAndShowErrorMessage(OperatorSelectionActivity.this);
                    }
                });
    }



    @Override
    public void onError(Object errorData) {
        cancelAndShowErrorMessage(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void cancelAndShowErrorMessage(Activity activity) {
        ConnectSdk.showAuthCancelMessage(activity);
        activity.setResult(Activity.RESULT_CANCELED);
        activity.finish();
    }

    private static boolean isConnectActivity(Intent intent) {
        return ConnectActivity.class.getName()
                .equals(intent.getComponent().getClassName());
    }
}

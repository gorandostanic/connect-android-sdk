package com.telenor.connect;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.telenor.connect.id.ConnectIdService;
import com.telenor.connect.ui.ConnectActivity;
import com.telenor.connect.utils.ConnectUtils;
import com.telenor.connect.utils.RestHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.telenor.connect.utils.ConnectUtils.PREFERENCES_FILE;

public abstract class AbstractSdkProfile implements SdkProfile {

    private ConnectIdService connectIdService;
    private WellKnownAPI.WellKnownConfig wellKnownConfig;

    protected Context context;
    protected boolean confidentialClient;
    private volatile boolean isInitialized = false;
    private final WellKnownConfigStore lastSeenStore = new WellKnownConfigStore();

    public AbstractSdkProfile(
            Context context,
            boolean confidentialClient) {
        this.context = context;
        this.confidentialClient = confidentialClient;

        wellKnownConfig = lastSeenStore.get();
    }

    public abstract String getWellKnownEndpoint();

    @Override
    public Context getContext() {
        return context;
    }

    public WellKnownAPI.WellKnownConfig getWellKnownConfig() {
        return wellKnownConfig;
    }

    @Override
    public boolean isConfidentialClient() {
        return confidentialClient;
    }

    @Override
    public ConnectIdService getConnectIdService() {
        return connectIdService;
    }

    @Override
    public void onFinishAuthorization(boolean success) {
        if (success) {
            lastSeenStore.set(wellKnownConfig);
        }
    }

    public void setConnectIdService(ConnectIdService connectIdService) {
        this.connectIdService = connectIdService;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    protected void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    protected void deInitialize() {
        isInitialized = false;
        wellKnownConfig = null;
    }

    protected void initializeAndContinueAuthorizationFlow(
            final Map<String, String> parameters,
            final List<String> uiLocales,
            final OnStartAuthorizationCallback callback) {
        if (isInitialized) {
            callback.onSuccess(getAuthIntent(parameters, uiLocales));
            return;
        }
        RestHelper.
                getWellKnownApi(getWellKnownEndpoint()).getWellKnownConfig(
                new Callback<WellKnownAPI.WellKnownConfig>() {
                    @Override
                    public void success(WellKnownAPI.WellKnownConfig config, Response response) {
                        wellKnownConfig = config;
                        isInitialized = true;
                        callback.onSuccess(getAuthIntent(parameters, uiLocales));
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        wellKnownConfig = null;
                        isInitialized = true;
                        callback.onSuccess(getAuthIntent(parameters, uiLocales));
                    }
                });
    }

    private class WellKnownConfigStore {

        private static final String PREFERENCE_KEY_WELL_KNOWN_CONFIG = "WELL_KNOWN_CONFIG";
        private final Gson preferencesGson = new Gson();

        private void set(WellKnownAPI.WellKnownConfig wellKnownConfig) {
            String jsonWellKnownConfig = preferencesGson.toJson(wellKnownConfig);
            context
                    .getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREFERENCE_KEY_WELL_KNOWN_CONFIG, jsonWellKnownConfig)
                    .apply();
        }

        private WellKnownAPI.WellKnownConfig get() {
            String wellKnownConfigJson = context
                    .getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                    .getString(PREFERENCE_KEY_WELL_KNOWN_CONFIG, null);

            return preferencesGson.fromJson(
                    wellKnownConfigJson,
                    WellKnownAPI.WellKnownConfig.class);
        }
    }

    protected Intent getAuthIntent(
            Map<String, String> parameters,
            List<String> uiLocales) {
        final Intent intent = new Intent();
        intent.setClass(getContext(), ConnectActivity.class);
        intent.setAction(ConnectUtils.LOGIN_ACTION);
        final String url = getAuthorizationStartUri(parameters, uiLocales).toString();
        intent.putExtra(ConnectUtils.LOGIN_AUTH_URI, url);
        intent.putExtra(ConnectUtils.WELL_KNOWN_CONFIG_EXTRA, getWellKnownConfig());
        return intent;
    }

    private Uri getAuthorizationStartUri(
            Map<String, String> parameters,
            List<String> uiLocales) {
        if (getClientId() == null) {
            throw new ConnectException("Client ID not specified in application manifest.");
        }
        if (getRedirectUri() == null) {
            throw new ConnectException("Redirect URI not specified in application manifest.");
        }

        if (parameters.get("scope") == null || parameters.get("scope").isEmpty()) {
            throw new IllegalStateException("Cannot log in without scope tokens.");
        }

        if (TextUtils.isEmpty(parameters.get("state"))) {
            parameters.put("state", UUID.randomUUID().toString());
        }

        return getAuthorizeUri(parameters, uiLocales);
    }
}

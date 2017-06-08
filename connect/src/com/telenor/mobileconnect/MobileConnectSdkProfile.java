package com.telenor.mobileconnect;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.squareup.okhttp.HttpUrl;
import com.telenor.connect.AbstractSdkProfile;
import com.telenor.connect.id.ConnectAPI;
import com.telenor.connect.id.ConnectIdService;
import com.telenor.connect.id.ConnectTokensTO;
import com.telenor.connect.id.TokenStore;
import com.telenor.connect.id.UserInfo;
import com.telenor.connect.utils.ConnectUrlHelper;
import com.telenor.connect.utils.ConnectUtils;
import com.telenor.connect.utils.RestHelper;
import com.telenor.mobileconnect.id.MobileConnectAPI;
import com.telenor.mobileconnect.operatordiscovery.OperatorDiscoveryAPI;
import com.telenor.mobileconnect.operatordiscovery.OperatorDiscoveryConfig;
import com.telenor.mobileconnect.ui.ConnectActivityWithOperatorSelectionDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.ResponseCallback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.telenor.connect.utils.ConnectUtils.PREFERENCES_FILE;

public class MobileConnectSdkProfile extends AbstractSdkProfile {

    private OperatorDiscoveryConfig operatorDiscoveryConfig;
    private volatile OperatorDiscoveryAPI.OperatorDiscoveryResult operatorDiscoveryResult;
    private final OperatorDiscoveryConfigStore lastSeenStore = new OperatorDiscoveryConfigStore();

    public MobileConnectSdkProfile(
            Context context,
            final OperatorDiscoveryConfig operatorDiscoveryConfig,
            boolean confidentialClient) {
        super(context, confidentialClient);
        this.operatorDiscoveryConfig = operatorDiscoveryConfig;

        OperatorDiscoveryAPI.OperatorDiscoveryResult lastSeen = lastSeenStore.get();
        if (lastSeen != null) {
            operatorDiscoveryResult = lastSeen;
            setConnectIdService(createConnectIdService());
        }
    }

    @Override
    public HttpUrl getApiUrl() {
        String host = operatorDiscoveryResult.getMobileConnectApiUrl().host();
        HttpUrl.Builder builder = new HttpUrl.Builder();
        builder
                .scheme(operatorDiscoveryResult.getMobileConnectApiUrl().scheme())
                .host(host);
        for (String seg : operatorDiscoveryResult.getMobileConnectApiUrl().pathSegments()) {
            builder.addPathSegment(seg);
        }
        return builder.build();
    }

    @Override
    public String getClientId() {
        return operatorDiscoveryResult.getClientId();
    }

    @Override
    public String getClientSecret() {
        return operatorDiscoveryResult.getClientSecret();
    }

    @Override
    public String getRedirectUri() {
        return operatorDiscoveryConfig.getOperatorDiscoveryRedirectUri();
    }

    @Override
    public String getExpectedIssuer() {
        if (getWellKnownConfig() != null) {
            return getWellKnownConfig().getIssuer();
        }
        return operatorDiscoveryResult.getBasePath();
    }

    @Override
    public List<String> getExpectedAudiences() {
        return new ArrayList<>(Arrays.asList(getClientId()));
    }

    @Override
    public Uri getAuthorizeUri(Map<String, String> parameters, List<String> locales) {
        Uri.Builder builder = ConnectUrlHelper.getAuthorizeUriStem(
                parameters,
                getClientId(),
                getRedirectUri(),
                locales,
                getApiUrl())
                .buildUpon();
        for (String pathSeg : getApiUrl().pathSegments()) {
            builder.appendPath(pathSeg);
        }
        return builder.build();
    }

    @Override
    public void onStartAuthorization(
            final Map<String, String> parameters,
            final List<String> uiLocales,
            final OnStartAuthorizationCallback callback) {

        if (isInitialized()) {
            callback.onSuccess(getAuthIntent(parameters, uiLocales));
            return;
        }

        TelephonyManager phMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = phMgr.getNetworkOperator();

        if (TextUtils.isEmpty(networkOperator)) {
            handleOperatorDiscoveryFailure(parameters, uiLocales, callback);
            return;
        }

        final String mcc = networkOperator.substring(0, 3);
        final String mnc = networkOperator.substring(3);
        getOperatorDiscoveryApi().getOperatorDiscoveryResult_ForMccMnc(
                getOperatorDiscoveryAuthHeader(),
                operatorDiscoveryConfig.getOperatorDiscoveryRedirectUri(),
                mcc,
                mnc,
                new Callback<OperatorDiscoveryAPI.OperatorDiscoveryResult>() {
                    @Override
                    public void success(OperatorDiscoveryAPI.OperatorDiscoveryResult operatorDiscoveryResult, Response response) {
                        initAndContinue(operatorDiscoveryResult, parameters, uiLocales, callback);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        handleOperatorDiscoveryFailure(parameters, uiLocales, callback);
                    }
                });
    }

    private void handleOperatorDiscoveryFailure(
            final Map<String, String> parameters,
            final List<String> uiLocales,
            final OnStartAuthorizationCallback callback) {
        getOperatorDiscoveryApi().getOperatorSelectionResult(
                getOperatorDiscoveryAuthHeader(),
                operatorDiscoveryConfig.getOperatorDiscoveryRedirectUri(),
                new Callback<OperatorDiscoveryAPI.OperatorSelectionResult>() {
                    @Override
                    public void success(
                            OperatorDiscoveryAPI.OperatorSelectionResult operatorSelectionResult,
                            Response response) {
                        String endpoint = operatorSelectionResult.getEndpoint();
                        if (endpoint == null) {
                            callback.onError();
                            return;
                        }
                        final Intent intent = new Intent(
                                getContext(),
                                ConnectActivityWithOperatorSelectionDelegate.class
                        );
                        intent.setAction(ConnectUtils.OPERATOR_SELECTION_ACTION);
                        intent.putExtra(ConnectUtils.OPERATOR_SELECTION_URI, endpoint);
                        intent.putExtra(
                                ConnectUtils.PARAM_SNAPHOT_EXTRA,
                                new HashMap<>(parameters));
                        intent.putExtra(
                                ConnectUtils.UI_LOCALES_SNAPHOT_EXTRA,
                                new ArrayList<>(uiLocales));
                        callback.onSuccess(intent);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        callback.onError();
                    }
                });
    }

    public synchronized void onStartAuthorization(
            final Map<String, String> parameters,
            final List<String> uiLocales,
            final String mcc,
            final String mnc,
            final String subscriberId,
            final OnStartAuthorizationCallback callback) {

        if (subscriberId != null) {
            parameters.put(
                    "login_hint",
                    String.format("ENCR_MSISDN:%s", subscriberId));
        }

        getOperatorDiscoveryApi().getOperatorDiscoveryResult_ForMccMnc(
                getOperatorDiscoveryAuthHeader(),
                operatorDiscoveryConfig.getOperatorDiscoveryRedirectUri(),
                mcc,
                mnc,
                new Callback<OperatorDiscoveryAPI.OperatorDiscoveryResult>() {
                    @Override
                    public void success(
                            OperatorDiscoveryAPI.OperatorDiscoveryResult operatorDiscoveryResult,
                            Response response) {
                        initAndContinue(operatorDiscoveryResult, parameters, uiLocales, callback);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        callback.onError();
                    }
                });
    }

    @Override
    public void onFinishAuthorization(boolean success) {
        super.onFinishAuthorization(success);
        if (success) {
            lastSeenStore.set(operatorDiscoveryResult);
        }
    }

    private OperatorDiscoveryAPI getOperatorDiscoveryApi() {
        return  RestHelper.getOperatorDiscoveryApi(
                    operatorDiscoveryConfig.getOperatorDiscoveryEndpoint());
    }

    private void initAndContinue(
            OperatorDiscoveryAPI.OperatorDiscoveryResult odResult,
            Map<String, String> parameters,
            List<String> uiLocales,
            OnStartAuthorizationCallback callback) {
        operatorDiscoveryResult = odResult;
        setConnectIdService(createConnectIdService());
        initializeAndContinueAuthorizationFlow(parameters, uiLocales, callback);
    }

    private ConnectIdService createConnectIdService() {
        HttpUrl url = getApiUrl();
        MobileConnectAPI mobileConnectApi =
                RestHelper.getMobileConnectApi(
                        String.format(
                                "%s://%s",
                                url.scheme(),
                                url.host()));
        return new ConnectIdService(
                new TokenStore(context),
                new MobileConnectAPIAdapter(mobileConnectApi),
                getClientId(),
                getRedirectUri());
    }

    public void deInitialize() {
        super.deInitialize();
        setInitialized(false);
    }

    @Override
    public String getWellKnownEndpoint() {
        return operatorDiscoveryResult.getWellKnownEndpoint();
    }

    private String getAuthorizationHeader() {
        return "Basic " + Base64.encodeToString(
                String.format("%s:%s",
                        getClientId(),
                        getClientSecret()).getBytes(),
                Base64.NO_WRAP);
    }

    private String getOperatorDiscoveryAuthHeader() {
        return Base64.encodeToString(
                String.format("%s:%s",
                        operatorDiscoveryConfig.getOperatorDiscoveryClientId(),
                        operatorDiscoveryConfig.getOperatorDiscoveryClientSecret()).getBytes(),
                Base64.NO_WRAP);
    }

    private class MobileConnectAPIAdapter implements ConnectAPI {

        private MobileConnectAPI mobileConnectApi;

        MobileConnectAPIAdapter(MobileConnectAPI mobileConnectApi) {
            this.mobileConnectApi = mobileConnectApi;
        }

        @Override
        public void getAccessTokens(
                String grantType,
                String code,
                String redirectUri,
                String clientId,
                Callback<ConnectTokensTO> tokens) {
            mobileConnectApi.getAccessTokens(
                    getAuthorizationHeader(),
                    operatorDiscoveryResult.getPath("token"),
                    grantType,
                    code,
                    redirectUri,
                    tokens);
        }

        @Override
        public void refreshAccessTokens(
                String grantType,
                String refreshToken,
                String clientId,
                Callback<ConnectTokensTO> tokens) {
            mobileConnectApi.refreshAccessTokens(
                    getAuthorizationHeader(),
                    operatorDiscoveryResult.getPath("token"),
                    grantType,
                    refreshToken,
                    tokens);
        }

        @Override
        public void revokeToken(
                String clientId,
                String token,
                ResponseCallback callback) {
            mobileConnectApi.revokeToken(
                    getAuthorizationHeader(),
                    operatorDiscoveryResult.getPath("tokenrevoke"),
                    token,
                    callback);
        }

        @Override
        public void getUserInfo(
                String auth,
                Callback<UserInfo> userInfoCallback) {
            mobileConnectApi.getUserInfo(
                    auth,
                    operatorDiscoveryResult.getPath("userinfo"),
                    userInfoCallback);
        }
    }

    private class OperatorDiscoveryConfigStore {

        private static final String PREFERENCE_KEY_OD_RESULT = "OD_RESULT";
        private final Gson preferencesGson = new Gson();

        private void set(
                OperatorDiscoveryAPI.OperatorDiscoveryResult odResult) {
            String jsonOdResult = preferencesGson.toJson(odResult);
            context
                    .getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREFERENCE_KEY_OD_RESULT, jsonOdResult)
                    .apply();
        }

        private OperatorDiscoveryAPI.OperatorDiscoveryResult get() {
            String wellKnownConfigJson = context
                    .getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                    .getString(PREFERENCE_KEY_OD_RESULT, null);
            return preferencesGson.fromJson(
                    wellKnownConfigJson,
                    OperatorDiscoveryAPI.OperatorDiscoveryResult.class);
        }
    }
}

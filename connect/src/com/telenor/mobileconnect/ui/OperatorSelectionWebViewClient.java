package com.telenor.mobileconnect.ui;

import android.net.Uri;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.telenor.connect.ConnectCallback;
import com.telenor.connect.ConnectSdk;

public class OperatorSelectionWebViewClient extends WebViewClient {

    private final View loadingView;
    private final View errorView;
    private final ConnectCallback operatorSelectionCallback;

    public OperatorSelectionWebViewClient(
            View loadingView,
            View errorView,
            ConnectCallback callback) {
        this.loadingView = loadingView;
        this.errorView = errorView;
        this.operatorSelectionCallback = callback;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (ConnectSdk.getRedirectUri() != null && url.startsWith(ConnectSdk.getRedirectUri())) {
            Uri uri = Uri.parse(url);
            if (validateRedirectUri(uri)) {
                operatorSelectionCallback.onSuccess(uri);
                return true;
            }
            operatorSelectionCallback.onError(null);
            return true;

        }
        return false;
    }

    private boolean validateRedirectUri(Uri uri) {
        String mcc_mnc = uri.getQueryParameter(OperatorSelectionActivity.MCC_MNC_PARAM_NAME);
        if (mcc_mnc != null) {
            String[] mccAndMnc = mcc_mnc.split(OperatorSelectionActivity.MCC_MNC_PARAM_SEP);
            return (mccAndMnc.length == 2);
        }
        return false;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        errorView.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode,
                                String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        loadingView.setVisibility(View.GONE);
    }

}

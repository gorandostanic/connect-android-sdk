package com.telenor.connect.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.telenor.connect.ConnectCallback;
import com.telenor.connect.id.ParseTokenCallback;

public class ConnectWebFragment extends AbstractConnectSdkWebFragment {
    private ConnectWebViewClient connectWebViewClient;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return onCreateView(
                inflater,
                container,
                new WebViewClientMaker() {
                    @Override
                    public WebViewClient make(
                            Activity activity,
                            WebView webView,
                            ViewStub loadingView,
                            View errorView,
                            ConnectCallback callback) {
                        connectWebViewClient = new ConnectWebViewClient(
                                getActivity(),
                                webView,
                                loadingView,
                                errorView,
                                new ParseTokenCallback(callback));
                        return connectWebViewClient;
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        connectWebViewClient.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        connectWebViewClient.onResume();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        connectWebViewClient.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }
}

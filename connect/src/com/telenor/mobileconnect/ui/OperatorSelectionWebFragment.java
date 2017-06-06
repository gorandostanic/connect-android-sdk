package com.telenor.mobileconnect.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.telenor.connect.ConnectCallback;
import com.telenor.connect.ui.AbstractConnectSdkWebFragment;

public class OperatorSelectionWebFragment extends AbstractConnectSdkWebFragment {

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
                        return new OperatorSelectionWebViewClient(loadingView, errorView, callback);
                    }
                });
    }
}

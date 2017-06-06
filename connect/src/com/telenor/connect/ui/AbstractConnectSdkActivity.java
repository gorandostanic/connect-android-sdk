package com.telenor.connect.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.telenor.connect.ConnectCallback;
import com.telenor.connect.ConnectSdk;
import com.telenor.connect.R;
import com.telenor.connect.utils.ConnectUrlHelper;
import com.telenor.connect.utils.ConnectUtils;

public abstract class AbstractConnectSdkActivity
        extends FragmentActivity
        implements ConnectCallback {

    protected Fragment singleFragment;

    protected <T extends AbstractConnectSdkWebFragment> void onCreate(
            Bundle savedInstanceState,
            Class<T> clazz) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_telenor_connect_activity_layout);

        FragmentManager manager = getSupportFragmentManager();
        final String fragmentTag = "SingleFragment";
        Fragment fragment = manager.findFragmentByTag(fragmentTag);

        Intent intent = getIntent();
        String action = intent.getAction();
        int loadingScreen = intent.getIntExtra(ConnectUtils.CUSTOM_LOADING_SCREEN_EXTRA,
                R.layout.com_telenor_connect_default_loading_view);

        if (fragment == null) {
            try {
                fragment = clazz.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("Could not instantiate fragment", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("This shoudn't happen", e);
            }
            Bundle bundle = new Bundle(intent.getExtras());
            bundle.putString(ConnectUrlHelper.ACTION_ARGUMENT, action);
            bundle.putInt(ConnectUtils.CUSTOM_LOADING_SCREEN_EXTRA, loadingScreen);
            if (action.equals(ConnectUtils.PAYMENT_ACTION)) {
                bundle.putString(ConnectUrlHelper.URL_ARGUMENT,
                        intent.getStringExtra(ConnectSdk.EXTRA_PAYMENT_LOCATION));
            }
            fragment.setArguments(bundle);
            fragment.setRetainInstance(true);
            manager.beginTransaction()
                    .add(R.id.com_telenor_connect_fragment_container, fragment, fragmentTag)
                    .commit();
        }

        singleFragment = fragment;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (singleFragment != null) {
            singleFragment.onConfigurationChanged(newConfig);
        }
    }
}

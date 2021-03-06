package com.kyleduo.rabbits.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kyleduo.rabbits.Interceptor;
import com.kyleduo.rabbits.Rabbit;
import com.kyleduo.rabbits.RabbitResult;
import com.kyleduo.rabbits.annotations.Page;
import com.kyleduo.rabbits.demo.base.BaseFragment;
import com.kyleduo.rabbits.rules.Rules;

/**
 * Created by kyle on 2016/12/12.
 */
@Page("/web")
public class WebFragment extends BaseFragment {
    public static final String KEY_URL = "url";

    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mWebView = (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.fragment_web, container, false);
        mWebView.setWebViewClient(new DefaultWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setUseWideViewPort(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        return mWebView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle extras = getArguments();
        String url = "file:///android_asset/web.html";
        if (extras != null) {
            url = extras.getString(KEY_URL, url);
        }
        mWebView.loadUrl(url);
    }

    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }

    private class DefaultWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            boolean ret = Rabbit.from(WebFragment.this)
                    .to(url)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public RabbitResult intercept(Dispatcher dispatcher) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, dispatcher.action().getUri());
                            startActivity(intent);
                            return RabbitResult.success();
                        }
                    }, Rules.scheme().is("tel"))
                    .ignoreFallback()
                    .start()
                    .isSuccess();
            return ret || super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
        }
    }

    @Override
    public boolean onBackPressedSupport() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onBackPressedSupport();
    }
}

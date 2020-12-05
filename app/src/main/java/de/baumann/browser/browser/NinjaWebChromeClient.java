package de.baumann.browser.browser;

import android.app.Activity;
import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.webkit.*;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebChromeClient extends WebChromeClient {

    private final NinjaWebView ninjaWebView;
    private NinjaWebView newWebView = null;

    public NinjaWebChromeClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
    }


    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        ninjaWebView.update(progress);
        if (view.getTitle().isEmpty()) {
            ninjaWebView.update(view.getUrl());
        } else {
            ninjaWebView.update(view.getTitle());
        }
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        ninjaWebView.getBrowserController().onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        ninjaWebView.getBrowserController().onHideCustomView();
        super.onHideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        ninjaWebView.getBrowserController().showFileChooser(filePathCallback);
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Activity activity =  (Activity) ninjaWebView.getContext();
        HelperUnit.grantPermissionsLoc(activity);
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }

    @Override

    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {

        newWebView = new NinjaWebView(view.getContext());
        view.addView(newWebView);
        WebSettings settings = newWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        //这个setWebViewClient要加上，否则window.open弹出浏览器打开。

        newWebView.setWebViewClient(new NinjaWebViewClient(newWebView));
        newWebView.setWebChromeClient(this);

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        return true;

    }

    @Override
    public void onCloseWindow(WebView view) {

        if (newWebView != null) {
            newWebView.setVisibility(View.GONE);
            view.removeView(newWebView);

        }

    }
}

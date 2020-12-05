package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Message;

import androidx.preference.PreferenceManager;

import android.util.AttributeSet;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;

import de.baumann.browser.browser.*;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

import java.util.HashMap;
import java.util.Objects;

public class NinjaWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;
    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(int scrollY, int oldScrollY);
    }

    private Context context;

    private AlbumItem album;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;
    private NinjaClickHandler clickHandler;
    private GestureDetector gestureDetector;

    private Javascript javaHosts;
    private Remote remoteHosts;
    private SharedPreferences sp;
    private WebSettings webSettings;

    private boolean foreground;

    public boolean isForeground() {
        return foreground;
    }

    private BrowserController browserController = null;

    public BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context); // Cannot create a dialog, the WebView context is not an activity

        this.context = context;
        this.foreground = false;

        this.javaHosts = new Javascript(this.context);
        this.remoteHosts = new Remote(this.context);
        this.album = new AlbumItem(this.context, this, this.browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(this.context);
        this.clickHandler = new NinjaClickHandler(this);
        this.gestureDetector = new GestureDetector(context, new NinjaGestureListener(this));

        initWebView();
        initPreferences();
        initAlbum();
    }

    @SuppressWarnings("SameReturnValue")
    @SuppressLint("ClickableViewAccessibility")
    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
        setOnTouchListener((view, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return false;
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    public synchronized void initPreferences() {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String userAgent = sp.getString("userAgent", "");
        webSettings = getSettings();

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(false);
        }
        if (!userAgent.isEmpty()) {
            webSettings.setUserAgentString(userAgent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webViewClient.enableAdBlock(sp.getBoolean(context.getString(R.string.sp_ad_block), true));
        webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));
        webSettings.setAllowFileAccessFromFileURLs(sp.getBoolean(("sp_remote"), false));
        webSettings.setAllowUniversalAccessFromFileURLs(sp.getBoolean(("sp_remote"), false));
        webSettings.setDomStorageEnabled(sp.getBoolean(("sp_remote"), true));
        webSettings.setBlockNetworkImage(!sp.getBoolean(context.getString(R.string.sp_images), true));
        webSettings.setJavaScriptEnabled(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), false));

    }

    private synchronized void initAlbum() {
        album.setAlbumTitle(context.getString(R.string.app_name));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        if (sp.getBoolean(context.getString(R.string.sp_savedata), false)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public synchronized void loadUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            NinjaToast.show(context, R.string.toast_load_error);
            return;
        }
        HelperUnit.initRendering(this);

        if (javaHosts.isWhite(url) || sp.getBoolean(context.getString(R.string.sp_javascript), true)) {
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setJavaScriptEnabled(true);
        } else {
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webSettings.setJavaScriptEnabled(false);
        }
        if (remoteHosts.isWhite(url) || sp.getBoolean("sp_remote", true)) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setDomStorageEnabled(true);
        } else {
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            webSettings.setDomStorageEnabled(false);
        }

        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim()), getRequestHeaders());
    }

    public synchronized void loadJs(String js) {
        super.loadUrl("javascript:" + js);
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    public void setAlbumTitle(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void update(int progress) {
        if (foreground) {
            browserController.updateProgress(progress);
        }
        if (isLoadFinish()) {
            browserController.updateAutoComplete();
        }
    }

    public synchronized void update(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public void onLongPress() {
        Message click = clickHandler.obtainMessage();
        click.setTarget(clickHandler);
        requestFocusNodeHref(click);
    }
}

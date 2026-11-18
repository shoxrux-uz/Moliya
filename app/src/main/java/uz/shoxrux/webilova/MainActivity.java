package uz.shoxrux.webilova;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    private static final String START_URL = BuildConfig.APP_URL;
    private static final int FILE_REQUEST = 1001;

    private WebView webView;
    private ProgressBar progress;
    private View loading;
    private View error;

    private boolean pageFailed = false;
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Splash fon faqat ishga tushishda kerak, keyin oddiy oq fon
        getWindow().setBackgroundDrawableResource(R.color.bg);

        webView = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);
        loading = findViewById(R.id.loading);
        error = findViewById(R.id.error);

        findViewById(R.id.retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retry();
            }
        });

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false); // yangi oyna ochilsa ham shu yerda ochiladi
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true); // Apps Script iframe uchun shart

        webView.setWebViewClient(new AppClient());
        webView.setWebChromeClient(new AppChrome());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) -> openExternal(url));

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            loading.setVisibility(View.GONE);
        } else {
            webView.loadUrl(START_URL);
        }
    }

    // ---------- Havolalarni boshqarish ----------

    /** Apps Script'ning o'z domenlari ilova ichida qoladi, qolganlari tashqarida ochiladi. */
    private boolean isInternal(Uri uri) {
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase();
        return host.equals("script.google.com")
                || host.endsWith(".googleusercontent.com")
                || host.equals("accounts.google.com")
                || host.endsWith(".gstatic.com");
    }

    private boolean route(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if (scheme.equals("http") || scheme.equals("https")) {
            if (isInternal(uri)) return false;   // WebView o'zi ochadi
            openExternal(url);
            return true;
        }
        if (scheme.equals("intent")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null) openExternal(fallback);
                } catch (Exception ignored) { }
            }
            return true;
        }
        // tel:, mailto:, tg:, sms: va boshqalar
        openExternal(url);
        return true;
    }

    private void openExternal(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (ActivityNotFoundException ignored) { }
    }

    // ---------- Yuklanish / xato ekranlari ----------

    private void showError() {
        pageFailed = true;
        error.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
    }

    private void retry() {
        error.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        String current = webView.getUrl();
        if (current == null || current.startsWith("about:") || current.startsWith("data:")) {
            webView.loadUrl(START_URL);
        } else {
            webView.reload();
        }
    }

    private class AppClient extends WebViewClient {

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return route(request.getUrl().toString());
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return route(url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            pageFailed = false;
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.GONE);
            if (!pageFailed) {
                error.setVisibility(View.GONE);
                if (loading.getVisibility() == View.VISIBLE) {
                    loading.animate().alpha(0f).setDuration(250).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            loading.setVisibility(View.GONE);
                            loading.setAlpha(1f);
                        }
                    });
                }
            }
            CookieManager.getInstance().flush();
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError err) {
            if (request.isForMainFrame()) showError();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) showError();
        }
    }

    private class AppChrome extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progress.setProgress(newProgress);
        }

        /** <input type="file"> ishlashi uchun (rasm/fayl tanlash) */
        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                         FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            try {
                startActivityForResult(params.createIntent(), FILE_REQUEST);
            } catch (ActivityNotFoundException e) {
                fileCallback = null;
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // ---------- Hayot sikli ----------

    @Override
    public void onBackPressed() {
        if (error.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

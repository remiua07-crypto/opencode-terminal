package ai.opencode.terminal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0A0A0A);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0A0A0A);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setUserAgentString(s.getUserAgentString() + " OpenCodeTerminal/1.0");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if (scheme.equals("oc")) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                }
                String host = u.getHost() == null ? "" : u.getHost();
                boolean local = host.equals("127.0.0.1")
                        || host.equals("localhost")
                        || host.equals(Prefs.host(MainActivity.this));
                if (!local && (scheme.equals("http") || scheme.equals("https"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, u));
                    } catch (Exception ignored) {
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectSkin();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showError(String.valueOf(error.getDescription()));
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        web.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, url, Toast.LENGTH_LONG).show();
            }
        });

        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextSize(15);
        gear.setTextColor(0xFF808080);
        gear.setAlpha(0.45f);
        gear.setGravity(Gravity.CENTER);
        gear.setContentDescription(getString(R.string.settings));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(30), dp(30));
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.topMargin = dp(2);
        lp.rightMargin = dp(6);
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(gear, lp);

        setContentView(root);
        web.loadUrl(Prefs.url(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String pending = Prefs.pendingUrl(this);
        if (pending != null) {
            Prefs.clearPendingUrl(this);
            web.loadUrl(pending);
            return;
        }
        // cấu hình URL có thể đã đổi trong Settings
        if (web.getUrl() != null && !web.getUrl().startsWith(Prefs.url(this))) {
            web.loadUrl(Prefs.url(this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] result = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        result = new Uri[n];
                        for (int i = 0; i < n; i++) {
                            result[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        result = new Uri[]{data.getData()};
                    }
                }
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** Chèn CSS + JS skin terminal vào mọi trang của OpenCode. */
    private void injectSkin() {
        if (web == null || !Prefs.skin(this)) return;
        try {
            String css = readAsset("tui.css");
            String js = readAsset("tui.js");
            String code = "(function(){try{"
                    + "var d=document;"
                    + "if(!d.getElementById('oc-tui-skin')){"
                    + "var st=d.createElement('style');st.id='oc-tui-skin';st.textContent="
                    + JSONObject.quote(css)
                    + ";(d.head||d.documentElement).appendChild(st);}"
                    + js
                    + "}catch(e){console.log('opencode-skin',e)}})();";
            web.evaluateJavascript(code, null);
        } catch (Exception ignored) {
        }
    }

    private void showError(String detail) {
        String safe = detail == null ? "" : detail.replace("<", "&lt;");
        String url = Prefs.url(this).replace("<", "&lt;");
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{background:#0a0a0a;color:#eee;font-family:ui-monospace,monospace;padding:26px;line-height:1.65;font-size:14px}"
                + "h1{color:#fab283;font-size:16px;font-weight:600;margin:0 0 14px}"
                + "code{color:#7fd88f}pre{color:#8a8a8a;white-space:pre-wrap;border-left:2px solid #333;padding-left:12px;margin:16px 0}"
                + "a{color:#fab283}</style></head><body>"
                + "<h1>Không kết nối được OpenCode server</h1>"
                + "<p>" + safe + "</p>"
                + "<p>URL: <code>" + url + "</code></p>"
                + "<pre>Termux:\n  opencode serve --hostname 127.0.0.1 --port 49374\n  opencode pair</pre>"
                + "<p><a href='oc://settings'>⚙ Mở cài đặt</a></p>"
                + "</body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private String readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

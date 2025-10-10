package org.chaitu.chatbotpro;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private static final String TARGET_URL = "https://ai.chaitu.net";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        progressBar = findViewById(R.id.web_view_progress_bar);
        webView = findViewById(R.id.ai_web_view);

        setupWebView();
        setupNetworkCallback();

        // Initial load or show error page
        if (isInternetAvailable()) {
            webView.loadUrl(TARGET_URL);
        } else {
            handleNoInternetConnection();
        }
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);

        // Expose retry() to JavaScript
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void retry() {
                runOnUiThread(() -> retryLoading());
            }
        }, "Android");

        // Progress handling
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
                super.onProgressChanged(view, newProgress);
            }
        });

        // Error handling
        webView.setWebViewClient(new WebViewClient() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    handleWebViewError(error.getErrorCode(), error.getDescription().toString());
                }
                super.onReceivedError(view, request, error);
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                handleWebViewError(errorCode, description);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }
        });
    }

    private void setupNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    String current = webView.getUrl();
                    if (current != null && current.startsWith("data:")) {
                        webView.loadUrl(TARGET_URL);
                    }
                });
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private boolean isInternetAvailable() {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void handleWebViewError(int errorCode, String description) {
        progressBar.setVisibility(View.GONE);
        if (isNetworkError(errorCode) && !isInternetAvailable()) {
            handleNoInternetConnection();
        } else if (isNetworkError(errorCode)) {
            handleServerError();
        } else {
            handleGenericError();
        }
    }

    private boolean isNetworkError(int errorCode) {
        return errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                errorCode == WebViewClient.ERROR_CONNECT ||
                errorCode == WebViewClient.ERROR_IO ||
                errorCode == WebViewClient.ERROR_TIMEOUT ||
                errorCode == WebViewClient.ERROR_FILE_NOT_FOUND;
    }

    private void handleNoInternetConnection() {
        String html = "<!DOCTYPE html><html><head><title>No Internet</title>"
                + "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;}"
                + ".retry-btn{background:#007bff;color:#fff;padding:10px 20px;border:none;border-radius:5px;"
                + "cursor:pointer;}</style></head><body>"
                + "<h2>No Internet Connection</h2>"
                + "<p>Please check your connection.</p>"
                + "<button class='retry-btn' onclick='Android.retry()'>Retry</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }

    private void handleServerError() {
        String html = "<!DOCTYPE html><html><head><title>Server Error</title>"
                + "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;}"
                + ".retry-btn{background:#007bff;color:#fff;padding:10px 20px;border:none;border-radius:5px;"
                + "cursor:pointer;}</style></head><body>"
                + "<h2>Server Unavailable</h2>"
                + "<p>Please try again later.</p>"
                + "<button class='retry-btn' onclick='Android.retry()'>Retry</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void handleGenericError() {
        String html = "<!DOCTYPE html><html><head><title>Error</title>"
                + "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;}"
                + ".retry-btn{background:#007bff;color:#fff;padding:10px 20px;border:none;border-radius:5px;"
                + "cursor:pointer;}</style></head><body>"
                + "<h2>Error Loading Page</h2>"
                + "<p>Something went wrong.</p>"
                + "<button class='retry-btn' onclick='Android.retry()'>Retry</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void retryLoading() {
        if (isInternetAvailable()) {
            String current = webView.getUrl();
            if (current == null || !current.equals(TARGET_URL)) {
                webView.loadUrl(TARGET_URL);
            }
        } else {
            handleNoInternetConnection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}

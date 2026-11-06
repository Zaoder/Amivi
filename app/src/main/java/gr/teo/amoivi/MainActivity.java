package gr.teo.amoivi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    public static final String PREFS = "amoivi";
    public static final String KEY_STATE = "state";

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0f1115"));
        getWindow().setNavigationBarColor(Color.parseColor("#0f1115"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0f1115"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(this), "AmoiviBridge");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onPause() {
        super.onPause();
        AmoiviWidget.updateAll(this);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /** Γέφυρα: η σελίδα αποθηκεύει την κατάσταση εδώ, ώστε να τη διαβάζει και το widget. */
    public static class Bridge {
        private final Context ctx;

        Bridge(Context c) { ctx = c.getApplicationContext(); }

        @JavascriptInterface
        public String load() {
            return prefs().getString(KEY_STATE, "");
        }

        @JavascriptInterface
        public void save(String json) {
            prefs().edit().putString(KEY_STATE, json).apply();
            AmoiviWidget.updateAll(ctx);
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        }

        private SharedPreferences prefs() {
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }
}

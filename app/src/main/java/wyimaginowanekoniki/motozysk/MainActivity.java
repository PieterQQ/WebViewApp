package wyimaginowanekoniki.motozysk;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    static boolean FUPLOAD = true;
    static boolean MULFILE = true;

    private static String Url ="http://www.motozysk.pl";
    private static String F_TYPE   = "*/*";
    SwipeRefreshLayout refreshLayout;
    WebView view;

    ProgressBar circle;
    private ValueCallback<Uri> file_message;
    private ValueCallback<Uri[]> file_path;
    private final static int file_req = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {

            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == file_req) {
                    if (null == file_path) {
                        return;
                    }
                    else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{ Uri.parse(dataString) };
                        } else {
                            if(MULFILE) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            file_path.onReceiveValue(results);
            file_path = null;
        } else {
            if (requestCode == file_req) {
                if (null == file_message) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                file_message.onReceiveValue(result);
                file_message = null;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("READ_PERM = ",Manifest.permission.READ_EXTERNAL_STORAGE);
        Log.w("WRITE_PERM = ",Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //Prevent the app from being started again when it is still alive in the background
        circle = (ProgressBar)findViewById(R.id.progressBar);
        setContentView(R.layout.activity_main);

        view = findViewById(R.id.msw_view);
        WebSettings webSettings = view.getSettings();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        view.setVerticalScrollBarEnabled(false);
        view.setWebViewClient(new Callback());
        view.loadUrl(Url);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.SwipeRefresh);
        refreshLayout.setOnRefreshListener(this);

        view.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {

                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(intent);
            }
        });
        view.setWebChromeClient(new WebChromeClient() {



            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){

                if(FUPLOAD) {
                    if (file_path != null) {
                        file_path.onReceiveValue(null);
                    }
                    file_path = filePathCallback;
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType(F_TYPE);
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                    startActivityForResult(Intent.createChooser(contentSelectionIntent, "Wybierz Zdjęcie"), file_req);
                }
                return true;
            }
        });
    }

    @Override
    public void onRefresh() {
        if (!DetectConnection.isInternetAvailable(MainActivity.this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.dc), Toast.LENGTH_SHORT).show();

        }
        else{
        view.reload();
        refreshLayout.setRefreshing(false);
    }}

    private class Callback extends WebViewClient {
        public void onPageFinished(WebView view, String url) {
            if(findViewById(R.id.msw_welcome).getVisibility() == View.VISIBLE) {
                if(DetectConnection.isInternetAvailable(MainActivity.this)) {
                    findViewById(R.id.msw_welcome).setVisibility(View.GONE);
                    findViewById(R.id.msw_view).setVisibility(View.VISIBLE);
                }else{
                    new Handler().postDelayed(new Runnable() {

                        /*
                         * Showing splash screen with a timer. This will be useful when you
                         * want to show case your app logo / company
                         */

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getString(R.string.dc), Toast.LENGTH_SHORT).show();
                            MainActivity.this.view.reload();
                        }
                    }, 5000);

                }
            }
        }

        //Overriding webview URLs
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!DetectConnection.isInternetAvailable(MainActivity.this)) {
                Toast.makeText(getApplicationContext(), getString(R.string.dc), Toast.LENGTH_SHORT).show();
                return  true;
            }
            return false;

        }

        //Overriding webview URLs for API 23+ [suggested by github.com/JakePou]
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (!DetectConnection.isInternetAvailable(MainActivity.this)) {
                Toast.makeText(getApplicationContext(), getString(R.string.dc), Toast.LENGTH_SHORT).show();
                return  true;
            }
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if((keyCode==KeyEvent.KEYCODE_BACK)&& view.canGoBack()) {
                view.goBack();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Czy chcesz opuścić aplikację?")
                .setCancelable(false).setPositiveButton("Tak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setNegativeButton("Nie",null).show();
    }
}

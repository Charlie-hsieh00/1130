package tw.nu.pasta.workshop;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.addJavascriptInterface(new ImageSaver(this), "AndroidImageSaver");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    public static class ImageSaver {
        private final Context context;

        ImageSaver(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void saveBase64Png(String dataUrl, String requestedName) {
            try {
                String base64 = dataUrl;
                int comma = dataUrl.indexOf(',');
                if (comma >= 0) {
                    base64 = dataUrl.substring(comma + 1);
                }
                byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);

                String safeName = (requestedName == null || requestedName.trim().isEmpty())
                        ? "我的擬真義大利麵完成圖.png"
                        : requestedName;
                if (!safeName.toLowerCase().endsWith(".png")) {
                    safeName += ".png";
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = context.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, safeName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/NU義大利麵工坊");
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);

                    android.net.Uri uri = resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) {
                        throw new IllegalStateException("Unable to create image in MediaStore");
                    }

                    try (OutputStream out = resolver.openOutputStream(uri)) {
                        if (out == null) {
                            throw new IllegalStateException("Unable to open image output stream");
                        }
                        out.write(imageBytes);
                        out.flush();
                    }

                    ContentValues done = new ContentValues();
                    done.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(uri, done, null, null);
                } else {
                    File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "NU義大利麵工坊");
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw new IllegalStateException("Unable to create pictures directory");
                    }
                    File file = new File(dir, safeName);
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        out.write(imageBytes);
                        out.flush();
                    }
                    MediaScannerConnection.scanFile(context,
                            new String[]{file.getAbsolutePath()},
                            new String[]{"image/png"}, null);
                }

                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context,
                                "已儲存到相簿：Pictures/NU義大利麵工坊",
                                Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context,
                                "圖片儲存失敗：" + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

package com.dsp.groups;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.webkit.CookieManager;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final String PRIVACY_URL = "https://www.dspgroups.com/";
    ProgressDialog progressDialog;
    WebView myview;
    RelativeLayout layout;
    SwipeRefreshLayout swipeRefreshLayout;
    Context ctx=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx=this;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//  set status text dark
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));

        layout = (RelativeLayout) findViewById(R.id.layout);
swipeRefreshLayout=(SwipeRefreshLayout) findViewById(R.id.swipeToRefresh);

        showLoader();
        myview = (WebView) findViewById(R.id.webView);
        HashMap<String, String> map = new HashMap<>();
        myview.getSettings().setDomStorageEnabled(true);
        myview.getSettings().setJavaScriptEnabled(true);
        myview.getSettings().setLoadWithOverviewMode(true);
        myview.getSettings().setUseWideViewPort(true);
//        myview.getSettings().setAppCacheEnabled(true);
        myview.getSettings().setDatabaseEnabled(true);
        startCookie();

        myview.loadUrl(PRIVACY_URL, map);
        myview.addJavascriptInterface(new MyJavaScriptInterface(this), "onCompletePayment");
        myview.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.e("LINK",request.getUrl().toString());

                //               -------------->
                String url=request.getUrl().toString();
                if (url.startsWith("intent://") ||url.startsWith("upi://")) {

                    Intent intent = null ;
                    try {
                        intent = Intent.parseUri(request.getUrl().toString(), Intent.URI_INTENT_SCHEME);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }


                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,"App not found in device",Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                        return true;

                }

//                -------------->

                if (request.getUrl().toString().startsWith("tel:")) {

                    Intent intent = new Intent(Intent.ACTION_DIAL,
                            Uri.parse(request.getUrl().toString()));
                    startActivity(intent);
                    return true;
                }

                if( String.valueOf(request.getUrl()).startsWith("whatsapp://"))
                {
                    myview.stopLoading();

                    try {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        String decoded = URLDecoder.decode(String.valueOf(request.getUrl()), "UTF-8");
                       String finalLink =decoded.replace("whatsapp://send?text="," " );
                        intent.putExtra(Intent.EXTRA_TEXT,finalLink);
                        startActivity(intent);
                    }
                    catch(Exception e){

                    }
                }

                if (request.getUrl().toString().contains("maps")) {

                    Uri gmmIntentUri = Uri.parse(request.getUrl().toString());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);

                    return true;

                }

                return false;
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
                hideLoader();
                CookieManager.getInstance().flush();

            }
        });
        setChromeClient();


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        showLoader();
                        myview.reload();

                    }
                }, 5000);
            }
        });

    }

    private void hideLoader() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public void startCookie() {
        CookieManager.setAcceptFileSchemeCookies(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
    }


    @Override
    public void onBackPressed() {
        if (myview.canGoBack()) {
            myview.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            builder.setMessage(getString(R.string.alert_msg_for_exiting));
            builder.setCancelable(false);

            builder.setPositiveButton(
                    R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            finish();
                        }
                    });

            builder.setNegativeButton(
                    R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void showLoader() {
        progressDialog = new ProgressDialog(this, R.style.progressTheme);
        try {
            progressDialog.show();
        } catch (WindowManager.BadTokenException e) {

        }
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progressbar);

        if (!isFinishing()) {
            progressDialog.show();
        }
    }


    class MyJavaScriptInterface {

        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void paymentResponse(String response) {

        }
    }


    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;

    Bitmap selectedImage;

    private void setChromeClient() {
        myview.setWebChromeClient(new WebChromeClient() {

            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // callback.invoke(String origin, boolean allow, boolean remember);
                callback.invoke(origin, true, false);
                Dexter.withContext(ctx)
                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override public void onPermissionGranted(PermissionGrantedResponse response) {
                            }
                            @Override public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(MainActivity.this,"please allow location",Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token)
                            {  token.continuePermissionRequest();    }
                        }).check();
            }
            // For Android 5.0
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePath;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;
                intentArray = new Intent[]{takePictureIntent};
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }

            // openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

                File imageStorageDir = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)
                        , "AndroidExampleFolder");
                if (!imageStorageDir.exists()) {
                    // Create AndroidExampleFolder at sdcard
                    imageStorageDir.mkdirs();
                }
                // Create camera captured image file path and name
                File file = new File(
                        imageStorageDir + File.separator + "IMG_"
                                + String.valueOf(System.currentTimeMillis())
                                + ".jpg");
                mCapturedImageURI = Uri.fromFile(file);
                // Camera capture image intent
                final Intent captureIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                        , new Parcelable[]{captureIntent});
                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            }

            // openFileChooser for Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            //openFileChooser for other Android versions
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType,
                                        String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            // Check that the response is a good one
            if (resultCode == RESULT_OK) {
                // If there is not data, then we may have taken a photo
                selectedImage =
                        (data.getExtras()) != null ?
                                (Bitmap) data.getExtras().get("data")
                                : null;

                if (selectedImage != null) {
                    if (getImageUri(selectedImage) != null) {
                        results = new Uri[]{getImageUri(selectedImage)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }

                if (results != null) {
                    mFilePathCallback.onReceiveValue(results);
                }

                mFilePathCallback = null;

            }
        }

    }

    public Uri getImageUri(Bitmap bitmap) {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File f = new File(getCacheDir(), imageFileName);
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        //write the bytes in file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.fromFile(f);

    }

}

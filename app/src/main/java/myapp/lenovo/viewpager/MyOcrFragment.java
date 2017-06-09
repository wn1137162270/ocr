package myapp.lenovo.viewpager;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class MyOcrFragment extends Fragment {

    private ProgressBar progressBar;
    private WebView ocr;
    private String url="http://115.159.205.168/ocr_php/public/";
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private OcrContent ocrContent;
    private String mCameraPhotoPath;
    private Uri mCapturedImageURI = null;
    private Uri result;

//    private static final int FILECHOOSER=1;
//    private static final int FILECHOOSERFORANDROID5=2;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_my_ocr, container, false);
        progressBar= (ProgressBar) view.findViewById(R.id.progress_bar);
        ocr= (WebView) view.findViewById(R.id.web_view);
        initWebView();
        return view;
    }

    private void initWebView(){
        ocr.getSettings().setJavaScriptEnabled(true);
        ocr.getSettings().setPluginState(WebSettings.PluginState.OFF);
        ocr.getSettings().setLoadWithOverviewMode(true);
        ocr.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        ocr.getSettings().setUseWideViewPort(true);
        ocr.getSettings().setUserAgentString("Android Mozilla/5.0 AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        ocr.getSettings().setAllowFileAccess(true);
        ocr.getSettings().setAllowFileAccess(true);
        ocr.getSettings().setAllowContentAccess(true);
        ocr.getSettings().supportZoom();
        ocr.getSettings().setJavaScriptEnabled(true);
        ocr.addJavascriptInterface(new InJavaScriptLocalObj(), "java_obj");
        ocr.setFocusable(true);
        ocr.setFocusableInTouchMode(true);
        ocr.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(ocr.canGoBack()&&keyEvent.getKeyCode()==KeyEvent.KEYCODE_BACK){
                    WebBackForwardList bfl=ocr.copyBackForwardList();
                    if(bfl.getCurrentIndex()>0){
                        ocr.goBack();
                        return true;
                    }
                }
                return false;
            }
        });
        ocr.loadUrl(url);

        ocr.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                if ( url.contains(".pdf")){
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(url), "application/pdf");
                    try{
                        view.getContext().startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        //user does not have a pdf viewer installed
                    }
                } else {
                    ocr.loadUrl(url);
                }
                return true; // then it is not handled by default action
            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                Log.e("error",description);
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {        //show progressbar here

                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //hide progressbar here
                view.loadUrl("javascript:window.java_obj.getSource(document.getElementById('wn').innerHTML);");
                super.onPageFinished(view, url);
            }

        });
        ocr.setWebChromeClient(new ChromeClient());
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    public class ChromeClient extends WebChromeClient {
        @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if(newProgress==100){
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                }
                else {
                    if(progressBar.getVisibility()==ProgressBar.INVISIBLE){
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                    progressBar.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }

        // For Android 5.0
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e("tag", "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;

        }

        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

            mUploadMessage = uploadMsg;
            // Create AndroidExampleFolder at sdcard
            // Create AndroidExampleFolder at sdcard

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
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            // Create file chooser intent
            Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

            // Set camera intent to file chooser
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                    , new Parcelable[] { captureIntent });

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
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                }
                else{
                    String dataString = data.getDataString();
                    Log.d("dataString",dataString);
                    result=Uri.parse(dataString);
                    if (dataString != null) {
                        results = new Uri[]{result};
                    }
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }
                //Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                        Log.d("newResult",result.toString());
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity().getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
        return;
    }

//    private void initWebView(){
//
//        ocr.loadUrl(url);
//        WebSettings webSettings=ocr.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        ocr.addJavascriptInterface(new InJavaScriptLocalObj(), "java_obj");
//        ocr.setFocusable(true);
//        ocr.setFocusableInTouchMode(true);
//        ocr.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View view, int i, KeyEvent keyEvent) {
//                if(ocr.canGoBack()&&keyEvent.getKeyCode()==KeyEvent.KEYCODE_BACK){
//                    WebBackForwardList bfl=ocr.copyBackForwardList();
//                    if(bfl.getCurrentIndex()>0){
//                        ocr.goBack();
//                        return true;
//                    }
//                }
//                return false;
//            }
//        });
//
//        ocr.setWebViewClient(new WebViewClient(){
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                view.loadUrl(url);
//                return true;
//            }
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                view.loadUrl("javascript:window.java_obj.getSource(document.getElementById('wn').innerHTML);");
//                super.onPageFinished(view, url);
//            }
//        });
//
//        ocr.setWebChromeClient(new WebChromeClient(){
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                if(newProgress==100){
//                    progressBar.setVisibility(ProgressBar.INVISIBLE);
//                }
//                else {
//                    if(progressBar.getVisibility()==ProgressBar.INVISIBLE){
//                        progressBar.setVisibility(ProgressBar.VISIBLE);
//                    }
//                    progressBar.setProgress(newProgress);
//                }
//                super.onProgressChanged(view, newProgress);
//            }
//
//            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
//                openFileChooserImpl(uploadMsg);
//            }
//
//            public void openFileChooser(ValueCallback<Uri> uploadMsg){
//                openFileChooserImpl(uploadMsg);
//            }
//
//            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType,String capture) {
//                openFileChooserImpl(uploadMsg);
//            }
//
//            @Override
//            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
//                openFileChooserForAndroid5(filePathCallback);
//                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
//            }
//        });
//    }
//
//    public void openFileChooserImpl(ValueCallback<Uri> uploadMsg){
//        myUploadMsg=uploadMsg;
//        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("image/*");
//        startActivityForResult(Intent.createChooser(intent,"File Chooser"),FILECHOOSER);
//    }
//
//    public void openFileChooserForAndroid5(ValueCallback<Uri[]> filePathCallback){
//        myFilePathCallback=filePathCallback;
//        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("image/*");
//
//        Intent chooseIntent=new Intent(Intent.ACTION_CHOOSER);
//        chooseIntent.putExtra(Intent.EXTRA_INTENT,intent);
//        chooseIntent.putExtra(Intent.EXTRA_TITLE,"File Chooser");
//        startActivityForResult(chooseIntent,FILECHOOSERFORANDROID5);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode==FILECHOOSER){
//            if(myUploadMsg==null)
//                return;
//            if(data==null||resultCode!=RESULT_OK){
//                result=null;
//            }
//            else{
//                result=data.getData();
//                Log.d("result", String.valueOf(result));
//            }
//            myUploadMsg.onReceiveValue(result);
//            myUploadMsg=null;
//        }
//        else if(requestCode==FILECHOOSERFORANDROID5){
//            if(myFilePathCallback==null)
//                return;
//            if(data==null||resultCode!=RESULT_OK){
//                result=null;
//            }
//            else{
//                result=data.getData();
//            }
//            if(result==null){
//                myFilePathCallback.onReceiveValue(new Uri[]{});
//            }
//            else{
//                myFilePathCallback.onReceiveValue(new Uri[]{result});
//            }
//            myFilePathCallback=null;
//        }
//    }

    @Override
    public void onAttach(Context context) {
        if(context!=null){
            ocrContent=(OcrContent)context;
        }
        super.onAttach(context);
    }

    final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void getSource(String html) {
            if(!html.equals("")&&html!=null) {
                ocrContent.getOcrContent(result,html);
            }
        }
    }

    public interface OcrContent{
        void getOcrContent(Uri uri,String html);
    }

}
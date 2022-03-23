package com.aswlinume.idcardrecognize;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] PERMISSIONS_READ_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"};

    private static final int REQUEST_CODE_SELECT_ID_CARD = 1;
    private static final int REQUEST_CODE_FOR_PERMISSION_READ_STORAGE = 2;

    //private TessBaseAPI mTessBaseApi;
    private String mLanguage = "cn";

    private ImageView mIvProcessRes;
    private Bitmap mOriginImage;
    private TextView mTvIdentifyRes;
    private ProgressDialog mProgressDialog;
    private Bitmap mExtractedImage;

    private Button mBtnLoadIDCard;
    private Button mBtnExtractIDCardNumber;
    private Button mBtnIdentifyIDCardNumber;


    private void showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("正在处理中...");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    private void dismissProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvProcessRes = (ImageView) findViewById(R.id.iv_image_process_result);
        mTvIdentifyRes = (TextView) findViewById(R.id.tv_IDCard_number_result);
        mBtnLoadIDCard = (Button) findViewById(R.id.btn_select_IDCard);
        mBtnExtractIDCardNumber = (Button) findViewById(R.id.btn_extract_IDCard_number);
        mBtnIdentifyIDCardNumber = (Button) findViewById(R.id.btn_identify_IDCard_number);

        //initTess();

    }

    /*private void initTess() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                showProgress();
            }

            @Override
            protected void onPostExecute(Boolean succeed) {
                if (succeed) {
                    dismissProgress();
                } else {
                    Toast.makeText(MainActivity.this, "识别模型训练失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                mTessBaseApi = new TessBaseAPI();
                try {
                    InputStream is = null;
                    is = getAssets().open(mLanguage);
                    File file = new File("/sdcard/tess/tessdata" + mLanguage);
                    if (!file.exists()) {
                        file.getParentFile().mkdir();
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buffer = new byte[2048];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    is.close();
                    return mTessBaseApi.init("/sdcard/tess", mLanguage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }*/

    public void loadIDCard(View view) {
        if (!checkReadStoragePermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_READ_STORAGE,
                    REQUEST_CODE_FOR_PERMISSION_READ_STORAGE);
            return;
        }
        startSelectImage();
    }

    public void startSelectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择待处理的图片"), REQUEST_CODE_SELECT_ID_CARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_ID_CARD && null != data) {
            Log.i(TAG, "onActivityResult: ");
            loadIDCardImageFromUri(data.getData());
        }
    }

    private boolean checkReadStoragePermissions() {
        return ActivityCompat.checkSelfPermission(this, PERMISSIONS_READ_STORAGE[0])
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_FOR_PERMISSION_READ_STORAGE) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSelectImage();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void extractIDCardNumber(View view) {
        mTvIdentifyRes.setText("");
        if (mExtractedImage != null) mExtractedImage.recycle();
        mExtractedImage = IDCardRecognizeUtils.getIdNumberImage(mOriginImage, Bitmap.Config.ARGB_8888);
        mOriginImage.recycle();
        mIvProcessRes.setImageBitmap(mExtractedImage);
        mBtnIdentifyIDCardNumber.setEnabled(true);
    }

    public void identifyIDCardNumber(View view) {
        //baseApi.setImage(mExtractedImage);
        //mIdentifyRes.setText(baseApi.getUTF8Text());
        //baseApi.clear();
    }

    private void loadIDCardImageFromUri(Uri uri) {
        String imagePath = null;
        if (uri != null) {
            if ("file".equals(uri.getScheme())) {
                imagePath = uri.getPath();
            } else if ("content".equals(uri.getScheme())) {
                String[] filePaths = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(uri, filePaths, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        int index = c.getColumnIndex(filePaths[0]);
                        imagePath = c.getString(index);
                    }
                    c.close();
                }
            }
        }
        if (!TextUtils.isEmpty(imagePath)) {
            if (mOriginImage != null) {
                mOriginImage.recycle();
            }
            mOriginImage = BitmapFactory.decodeFile(imagePath);
            mIvProcessRes.setImageBitmap(mOriginImage);
            mBtnExtractIDCardNumber.setEnabled(true);
        }

    }
}
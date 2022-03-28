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
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] PERMISSIONS_ACCESS_STORAGE =
            {"android.permission.READ_EXTERNAL_STORAGE",
             "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static final int REQUEST_CODE_SELECT_ID_CARD = 1;
    private static final int REQUEST_CODE_FOR_PERMISSION_READ_STORAGE = 2;
    private static final int REQUEST_CODE_FOR_PERMISSION_WRITE_STORAGE = 3;

    private TessBaseAPI mTessBaseApi;
    private String mLanguage = "cn";

    private ImageView mIvProcessRes;
    private Bitmap mOriginImage;
    private TextView mTvIdentifyRes;
    private ProgressDialog mProgressDialog;
    private Bitmap mExtractedImage;

    private Button mBtnLoadIDCard;
    private Button mBtnExtractIDCardNumber;
    private Button mBtnIdentifyIDCardNumber;

    private boolean mIsTessBaseApiInit = false;


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
        mIvProcessRes = findViewById(R.id.iv_image_process_result);
        mTvIdentifyRes = findViewById(R.id.tv_IDCard_number_result);
        mBtnLoadIDCard = findViewById(R.id.btn_select_IDCard);
        mBtnExtractIDCardNumber = findViewById(R.id.btn_extract_IDCard_number);
        mBtnIdentifyIDCardNumber = findViewById(R.id.btn_identify_IDCard_number);

        if (checkAccessStoragePermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_ACCESS_STORAGE,
                    REQUEST_CODE_FOR_PERMISSION_WRITE_STORAGE);
        } else {
            initTess();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTessBaseApi.recycle();
    }

    private void initTess() {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                mTessBaseApi = new TessBaseAPI();
                File file = new File("/sdcard/tess/tessdata/" + mLanguage + ".traineddata");
                if (!file.exists()) {
                    InputStream is = getAssets().open(mLanguage + ".traineddata");
                    File pf = file.getParentFile();
                    pf.mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    is.close();
                }
                emitter.onNext(mTessBaseApi.init("/sdcard/tess", mLanguage));
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Boolean succeed) {
                if (succeed) {
                    //dismissProgress();
                    mIsTessBaseApiInit = true;
                } else {
                    Toast.makeText(MainActivity.this, "识别模型初试化失败，原因：init调用失败",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, "识别模型初试化失败，原因：" + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void loadIDCard(View view) {
        if (checkAccessStoragePermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_ACCESS_STORAGE,
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

    private boolean checkAccessStoragePermissions() {
        return ActivityCompat.checkSelfPermission(this, PERMISSIONS_ACCESS_STORAGE[0])
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, PERMISSIONS_ACCESS_STORAGE[1])
                != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_FOR_PERMISSION_READ_STORAGE) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSelectImage();
                initTess();
            } else {
                Toast.makeText(this, "禁止存储权限将无法访问手机相册", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_FOR_PERMISSION_WRITE_STORAGE){
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initTess();
            } else {
                Toast.makeText(this, "禁止存储权限将无法完成识别功能", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void extractIDCardNumber(View view) {
        if (mExtractedImage != null) mExtractedImage.recycle();
        mExtractedImage = IDCardRecognizeUtils.getIdNumberImage(mOriginImage, Bitmap.Config.ARGB_8888);
        mOriginImage.recycle();
        mIvProcessRes.setImageBitmap(mExtractedImage);
        mBtnExtractIDCardNumber.setEnabled(false);
        mBtnIdentifyIDCardNumber.setEnabled(mIsTessBaseApiInit);
    }

    public void identifyIDCardNumber(View view) {
        mTessBaseApi.setImage(mExtractedImage);
        mTvIdentifyRes.setText(mTessBaseApi.getUTF8Text());
        mTessBaseApi.clear();
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
            mTvIdentifyRes.setText("");
            mBtnExtractIDCardNumber.setEnabled(true);
            mBtnIdentifyIDCardNumber.setEnabled(false);
        }

    }
}
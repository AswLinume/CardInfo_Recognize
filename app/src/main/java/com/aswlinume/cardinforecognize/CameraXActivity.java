package com.aswlinume.cardinforecognize;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraXActivity extends AppCompatActivity {

    public static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String TAG = CameraXActivity.class.getSimpleName();
    private ImageView mIvCapture;
    private ImageView mIvBack;
    private PreviewView mPvViewFinder;
    private Preview mPreview;
    private ImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;
    private Camera mCamera;

    private File mOutputDirectory;
    private ExecutorService mCameraExecutor;

    private Bitmap mBitmap;

    ImageView mPreView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_x_activity);
        startCamera();

        mIvCapture = findViewById(R.id.iv_camera);
        mIvBack = findViewById(R.id.iv_back);
        mPvViewFinder = findViewById(R.id.pv_view_finder);
        mPreView = findViewById(R.id.iv_perview);

        mIvCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mOutputDirectory = getOutputDirectory();

        mCameraExecutor = Executors.newSingleThreadExecutor();
    }

    private File getOutputDirectory() {
        File[] mediaDirs = getExternalMediaDirs();
        if (mediaDirs != null && mediaDirs.length > 0) {
            File mediaDir = new File(mediaDirs[0], getResources().getString(R.string.app_name));
            mediaDir.mkdir();
            if (mediaDir.exists()) return mediaDir;
        }
        return getFilesDir();
    }

    private void takePhoto() {
        if (mImageCapture == null) {
            return;
        }
        File photoFile = new File(mOutputDirectory,
                new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis()) + ".jpg");
        ImageCapture.OutputFileOptions ofo = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        mImageCapture.takePicture(ofo, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri saveUri = Uri.fromFile(photoFile);
                Toast.makeText(CameraXActivity.this, "Photo savepath: " + saveUri, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra(Constant.KEY_IMAGE_PATH, photoFile.getPath());
                setResult(Constant.CODE_RESULT_OK, intent);
                finish();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed:" + exception.getMessage());
            }
        });
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> instance = ProcessCameraProvider.getInstance(this);
        instance.addListener(new Runnable() {
            @Override
            public void run() {
                ProcessCameraProvider cameraProvider;
                try {
                    cameraProvider = instance.get();

                    mPreview = new Preview.Builder().build();

                    mImageCapture = new ImageCapture.Builder()
                            //.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build();

                    mImageAnalysis = new ImageAnalysis.Builder().build();

//                    mImageAnalysis.setAnalyzer(mCameraExecutor, new CardFinder(new ISearchResultListener() {
//                        @Override
//                        public void onFind() {
//
//                        }
//
//                        @Override
//                        public void onSearching(Image image) {
                            //Log.i(TAG, "onSearching: " + image.getFormat());
//                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                            buffer.rewind();
//                            byte[] bytes = new byte[buffer.remaining()];
//                            buffer.get(bytes);

//                            BitmapFactory.Options options = new BitmapFactory.Options();
//                            options.outWidth = image.getWidth();
//                            options.outHeight = image.getHeight();
//                            options.outConfig = image.getFormat();
//                            YuvImage yuv = new YuvImage(bytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
//                            ByteArrayOutputStream out = new ByteArrayOutputStream();
//                            yuv.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 80, out);
//                            bytes = out.toByteArray();
//                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                            Matrix matrix = new Matrix();
//                            matrix.setRotate(90);
//                            if (mBitmap != null) {
//                                mBitmap.recycle();
//                            }
//                            mBitmap = Bitmap.createBitmap(bmp, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
//                            bmp.recycle();
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Log.i(TAG, "run: " + mBitmap);
//                                    mPreView.setImageBitmap(mBitmap);
//                                }
//                            });

                            //Log.i(TAG, "analyze: " + bytes.length + "width: " + image.getWidth() + "height: " + image.getHeight());
//                        }
//                    }));

                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                    cameraProvider.unbindAll();

                    mCamera = cameraProvider.bindToLifecycle(CameraXActivity.this,
                            cameraSelector, mPreview, mImageCapture/*, mImageAnalysis*/);
                    if (mPreview != null) {
                        mPreview.setSurfaceProvider(mPvViewFinder.createSurfaceProvider(mCamera.getCameraInfo()));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private static class CardFinder implements ImageAnalysis.Analyzer{

        ISearchResultListener mSearchResultListener;

        public CardFinder(ISearchResultListener listener) {
            mSearchResultListener = listener;
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        @Override
        public void analyze(@NonNull ImageProxy image) {
            mSearchResultListener.onSearching(image.getImage());
            image.close();

        }
    }

    private interface ISearchResultListener {

        void onFind();

        void onSearching(Image image);

    }

}
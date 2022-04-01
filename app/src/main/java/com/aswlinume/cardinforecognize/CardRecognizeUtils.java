package com.aswlinume.cardinforecognize;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

public class CardRecognizeUtils {

    static {
        System.loadLibrary("IDCardReg");
    }

    public static native Bitmap getIdNumberImage(Bitmap src, Bitmap.Config config);

    @Nullable
    public static native Bitmap getCardImage(Bitmap src, Bitmap.Config config);


}

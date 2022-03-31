package com.aswlinume.idcardrecognize;

import android.graphics.Bitmap;

public class IDCardRecognizeUtils {

    static {
        System.loadLibrary("IDCardReg");
    }

    public static native Bitmap getIdNumberImage(Bitmap src, Bitmap.Config config);

    public static native Bitmap getIdCardImage(Bitmap src, Bitmap.Config config);


}

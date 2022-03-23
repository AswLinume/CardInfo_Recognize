//
// Created by Berl on 2022/3/8.
//

#include <cstdio>
#include <vector>
#include "opencv2/opencv.hpp"
#include <android/log.h>
#include <jni.h>

#define LOG_TAG "C/C++ Logcat"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define DEFAULT_CARD_WIDTH 640
#define DEFAULT_CARD_HEIGHT 400
#define FIX_IDCARD_SIZE Size(DEFAULT_CARD_WIDTH, DEFAULT_CARD_HEIGHT)
#define FIX_TEMPLATE_SIZE Size(153, 28)

using namespace cv;
using namespace std;

extern "C"  JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nBitmapToMat2
        (JNIEnv *env, jclass, jobject bitmap,jlong m_addr, jboolean needUnPremultiplyAlpha);

extern "C"  JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nMatToBitmap
        (JNIEnv *env, jclass, jlong m_addr, jobject bitmap);

jobject createBitmap(JNIEnv *env, Mat src_image, jobject config) {
    int imgWidth = src_image.cols;
    int imgHeight = src_image.rows;
    int numPix = imgWidth * imgHeight;
    jclass bmpClz = env->FindClass("android/graphics/Bitmap");
    jmethodID  createBmpMID = env->GetStaticMethodID(bmpClz, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jobject bmp = env->CallStaticObjectMethod(bmpClz, createBmpMID, imgWidth, imgHeight, config);
    Java_org_opencv_android_Utils_nMatToBitmap(env, 0, (jlong) &src_image, bmp);
    return bmp;
}


extern "C" JNIEXPORT jobject JNICALL
Java_com_aswlinume_idcardrecognize_IDCardRecognizeUtils_getIdNumberImage
        (JNIEnv *env, jclass clazz, jobject src, jobject config) {
    Mat src_img;
    Mat dst_img;
    Java_org_opencv_android_Utils_nBitmapToMat2(env, clazz, src, (jlong) &src_img, 0);
    Mat dst;

    LOGI("Utils_nBitmapToMat2");

    //无损压缩640 * 400
    resize(src_img, src_img, FIX_IDCARD_SIZE);

    //灰度化
    cvtColor(src_img, dst, COLOR_BGR2GRAY);

    // //二值化
    threshold(dst, dst, 100, 255, THRESH_BINARY);//CV_THRESH_BINARY

    //膨胀
    Mat erodeElement = getStructuringElement(MORPH_RECT, Size(20, 10));
    erode(dst, dst, erodeElement);

    // //轮廓检测
    vector<vector<Point>> contours;
    vector<Rect> rects;

    findContours(dst, contours, RETR_TREE, CHAIN_APPROX_SIMPLE, Point(0, 0));

    int size = contours.size();
    for (int i = 0; i < size; i++) {
        Rect rect = boundingRect(contours.at(i));
        rectangle(dst, rect, Scalar(0, 0, 255));
        //对符合条件的图片进行筛选
        if (rect.width > rect.height * 9) {
            rects.push_back(rect);
            rectangle(dst, rect, Scalar(0, 0, 255));//在dst图片上显示rect矩形
            dst_img = src_img(rect);
        }
    }

    //如果只找到一个矩形，那么就是目标图片。
    size = rects.size();
    if (size == 1) {
        Rect rect = rects.at(0);
        dst_img = src_img(rect);
    } else {
        int lowPoint = 0;
        Rect target;
        for (int i = 0; i < size; i++) {
            Rect rect = rects.at(i);
            Point p = rect.tl();
            if (p.y > lowPoint) {
                lowPoint = p.y;
                target = rect;
            }
        }
        rectangle(dst, target, Scalar(255, 255, 0));
        dst_img = src_img(target);
    }

    jobject bitmap = createBitmap(env, dst_img, config);

    src_img.release();
    dst_img.release();
    dst.release();

    return bitmap;
}
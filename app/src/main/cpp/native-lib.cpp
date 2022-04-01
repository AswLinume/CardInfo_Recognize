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

extern "C" JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nBitmapToMat2
        (JNIEnv *env, jclass, jobject bitmap, jlong m_addr, jboolean needUnPremultiplyAlpha);

extern "C" JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nMatToBitmap
        (JNIEnv *env, jclass, jlong m_addr, jobject bitmap);

jobject createBitmap(JNIEnv *env, Mat src_image, jobject config) {
    int imgWidth = src_image.cols;
    int imgHeight = src_image.rows;
    int numPix = imgWidth * imgHeight;
    jclass bmpClz = env->FindClass("android/graphics/Bitmap");
    jmethodID createBmpMID = env->GetStaticMethodID(bmpClz, "createBitmap",
                                                    "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jobject bmp = env->CallStaticObjectMethod(bmpClz, createBmpMID, imgWidth, imgHeight, config);
    Java_org_opencv_android_Utils_nMatToBitmap(env, 0, (jlong) &src_image, bmp);
    return bmp;
}


extern "C" JNIEXPORT jobject JNICALL
Java_com_aswlinume_cardinforecognize_CardRecognizeUtils_getIdNumberImage
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
            dst = src_img(rect);
        }
    }

    //如果只找到一个矩形，那么就是目标图片。
    size = rects.size();
    if (size == 1) {
        Rect rect = rects.at(0);
        dst = src_img(rect);
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
        dst = src_img(target);
    }

    resize(dst, dst_img, Size(), 2.0, 2.0);

    jobject bitmap = createBitmap(env, dst_img, config);

    src_img.release();
    dst_img.release();
    dst.release();

    return bitmap;
}

bool cmpByX(const Point &A, const Point &B){
    return A.x < B.x;
}

void getFourPoint(vector<Point> &points,Point &tl, Point &tr, Point &bl, Point &br) {
    sort(points.begin(), points.end(), cmpByX);
    if (points[0].y < points[1].y) {
        tl = points[0];
        bl = points[1];
    } else {
        tl = points[1];
        bl = points[0];
    }
    if (points[2].y < points[3].y) {
        tr = points[2];
        br = points[3];
    } else {
        tr = points[3];
        br = points[2];
    }
}


bool cmpByArea(const vector<Point> &A, const vector<Point> &B){
    return contourArea(A) > contourArea(B);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_aswlinume_cardinforecognize_CardRecognizeUtils_getCardImage
        (JNIEnv *env, jclass clazz,jobject src,jobject config) {
    Mat src_img;
    Java_org_opencv_android_Utils_nBitmapToMat2(env, clazz, src, (jlong) &src_img, 0);
    Mat resize_img;
    Mat process_img;
    Mat dst_img;

    resize(src_img, resize_img, Size(), 0.5, 0.5);

    //灰度化
    cvtColor(resize_img, process_img, COLOR_BGR2GRAY);

    //二值化
    threshold(process_img, process_img, 127, 255, THRESH_BINARY);

    //轮廓检测
    vector<vector<Point>> contours;
    findContours(process_img, contours, RETR_EXTERNAL, CHAIN_APPROX_NONE);

    sort(contours.begin(), contours.end(), cmpByArea);

    //int area = contourArea(contours[0]);

    vector<Point> points;
    float epi = 0.1 * arcLength(contours[0], true);
    approxPolyDP(contours[0], points, epi, true);

    Point tl, bl, tr, br;
    getFourPoint(points, tl, tr, bl, br);

    Point2f points_org[4] = {tl, tr, br, bl};

    float widthA = sqrt(pow((tr.x - tl.x), 2) + pow((tr.y - tl.y), 2));
    float widthB = sqrt(pow((br.x - bl.x), 2) + pow((br.y - bl.y), 2));
    float heightA = sqrt(pow((bl.x - tl.x), 2) + pow((bl.y - tl.y), 2));
    float heightB = sqrt(pow((br.x - tr.x), 2) + pow((br.y - tr.y), 2));
    float maxWidth = max(widthA, widthB);
    float maxHeight = max(heightA, heightB);

    float ratio = maxWidth / maxHeight;

    if (ratio < 1.5 || ratio > 1.8) return nullptr;

    int width = round(maxWidth);
    int height = round(maxHeight);

    Point2f points_new[4] = {Point2f(0, 0), Point2f(width - 1, 0), Point2f(width - 1, height - 1), Point2f(0, height - 1)};
    Mat M = getPerspectiveTransform(points_org, points_new);
    warpPerspective(resize_img, dst_img, M, Size(width, height));

    M.release();

    jobject bitmap = createBitmap(env, dst_img, config);

    src_img.release();
    dst_img.release();
    resize_img.release();
    process_img.release();

    return bitmap;
}
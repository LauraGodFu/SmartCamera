package com.zzdc.abb.smartcamera.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import com.zzdc.abb.smartcamera.R;
import com.zzdc.abb.smartcamera.controller.SurfacePreview;

public class WindowUtils {
    private static final String LOG_TAG = "WindowUtils";
//    private static View mView = null;
    private static WindowManager mWindowManager = null;
    private static Context mContext = null;
    public static Boolean isShown = false;
    public static WindowManager.LayoutParams params;
    /**
     * 显示弹出框
     *
     * @param context
     */
    public static void showPopupWindow(final Context context) {
        if (isShown) {
            Log.i(LOG_TAG, "return cause already shown");
            return;
        }
        isShown = true;
        Log.i(LOG_TAG, "LEON-showPopupWindow");
        // 获取应用的Context
        mContext = context.getApplicationContext();
        // 获取WindowManager
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        mView = setUpView(context);
        params = new WindowManager.LayoutParams();
        // 类型
        params.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        // 设置flag
        int flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
         | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL;
        // 如果设置了WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE，弹出的View收不到Back键的事件
        params.flags = flags;
        // 不设置这个弹出框的透明遮罩显示为黑色
        params.format = PixelFormat.TRANSPARENT;
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口
        // 设置 FLAG_NOT_FOCUSABLE 悬浮窗口较小时，后面的应用图标由不可长按变为可长按
        // 不设置这个flag的话，home页的划屏会有问题
        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.TOP| Gravity.RIGHT;
//        mWindowManager.addView(mView, params);

    }

    public static void addView(SurfacePreview view) {
        Log.i(LOG_TAG, "add view");
        mWindowManager.addView(view,params);
    }

    public static void removeView(SurfacePreview view) {
        mWindowManager.removeView(view);
    }
    /**
     * 隐藏弹出框
     */
//    public static void hidePopupWindow() {
//        Log.i(LOG_TAG, "hide " + isShown + ", " + mView);
//        if (isShown && null != mView) {
//            Log.i(LOG_TAG, "hidePopupWindow");
//            mWindowManager.removeView(mView);
//            isShown = false;
//        }
//    }
//    private static View setUpView(final Context context) {

        // 点击窗口外部区域可消除
        // 这点的实现主要将悬浮窗设置为全屏大小，外层有个透明背景，中间一部分视为内容区域
        // 所以点击内容区域外部视为点击悬浮窗外部
//        final View popupWindowView = view.findViewById(R.id.popup_window);// 非透明的内容区域
//        view.setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                Log.i(LOG_TAG, "onTouch");
//                int x = (int) event.getX();
//                int y = (int) event.getY();
//                Rect rect = new Rect();
//                popupWindowView.getGlobalVisibleRect(rect);
//                if (!rect.contains(x, y)) {
//                    WindowUtils.hidePopupWindow();
//                }
//                Log.i(LOG_TAG, "onTouch : " + x + ", " + y + ", rect: "
//                        + rect);
//                return false;
//            }
//        });
        // 点击back键可消除
//        view.setOnKeyListener(new OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                switch (keyCode) {
//                    case KeyEvent.KEYCODE_BACK:
//                        WindowUtils.hidePopupWindow();
//                        return true;
//                    default:
//                        return false;
//                }
//            }
//        });
//        return view;
//    }
}

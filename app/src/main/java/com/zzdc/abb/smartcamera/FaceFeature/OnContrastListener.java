package com.zzdc.abb.smartcamera.FaceFeature;

import android.graphics.Rect;
import java.util.List;

public interface OnContrastListener {
    void onContrastRectList(List<Rect> rectList);
    void onContrastSucceed(boolean focus, String identity);
    void onContrastFailed(String msg, byte[] stranger);
    void onContrastError(String message);
}

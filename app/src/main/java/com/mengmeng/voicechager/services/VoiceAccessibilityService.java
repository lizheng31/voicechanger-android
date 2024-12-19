package com.mengmeng.voicechager.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.mengmeng.voicechager.utils.LogUtils;
import java.util.List;

public class VoiceAccessibilityService extends AccessibilityService {
    private static VoiceAccessibilityService instance;
    private static String targetAudioPath;
    private boolean isButtonPressed = false;
    private static final String BTN_TEXT_PRESS = "按住 说话";
    private static final String BTN_TEXT_RELEASE = "松开 发送";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LogUtils.d("无障碍服务已创建");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null || !event.getPackageName().equals("com.tencent.mm")) {
            return;
        }

        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && 
            event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED && 
            event.getEventType() != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        List<AccessibilityNodeInfo> releaseNodes = rootNode.findAccessibilityNodeInfosByText(BTN_TEXT_RELEASE);
        if (releaseNodes != null && !releaseNodes.isEmpty()) {
            if (!isButtonPressed) {
                isButtonPressed = true;
                LogUtils.d("检测到按钮被按住");
            }
            recycleNodes(releaseNodes);
            rootNode.recycle();
            return;
        }

        List<AccessibilityNodeInfo> pressNodes = rootNode.findAccessibilityNodeInfosByText(BTN_TEXT_PRESS);
        if (pressNodes != null && !pressNodes.isEmpty()) {
            if (isButtonPressed) {
                isButtonPressed = false;
                LogUtils.d("检测到按钮被释放");
            }
            recycleNodes(pressNodes);
        }

        rootNode.recycle();
    }

    private void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            if (node != null) {
                node.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {
        LogUtils.d("无障碍服务中断");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_LONG_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_SELECTED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.packageNames = new String[]{"com.tencent.mm"};
        setServiceInfo(info);
        LogUtils.d("无障碍服务已连接");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        LogUtils.d("无障碍服务已销毁");
    }

    public static void setTargetAudioPath(String path) {
        targetAudioPath = path;
    }

    public static String getTargetAudioPath() {
        return targetAudioPath;
    }

    public static VoiceAccessibilityService getInstance() {
        return instance;
    }

    public boolean isButtonPressed() {
        return isButtonPressed;
    }
} 
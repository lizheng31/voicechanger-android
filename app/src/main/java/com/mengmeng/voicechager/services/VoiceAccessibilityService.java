package com.mengmeng.voicechager.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.mengmeng.voicechager.utils.LogUtils;
import java.util.List;

public class VoiceAccessibilityService extends AccessibilityService {
    private static VoiceAccessibilityService instance;
    private static String targetAudioPath;
    private boolean isWeChatVoiceMode = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null || !event.getPackageName().equals("com.tencent.mm")) {
            return;
        }

        LogUtils.d("收到微信的无障碍事件: " + event.getEventType());

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            LogUtils.d("无法获取根节点");
            return;
        }

        // 检测是否在微信聊天界面的语音模式
        AccessibilityNodeInfo pressToTalkButton = findPressToTalkButton(rootNode);
        boolean currentIsVoiceMode = pressToTalkButton != null;
        
        // 状态变化：进入语音模式
        if (currentIsVoiceMode && !isWeChatVoiceMode) {
            LogUtils.d("检测到进入微信语音模式");
            isWeChatVoiceMode = true;
        }
        
        // 状态变化：退出语音模式
        if (!currentIsVoiceMode && isWeChatVoiceMode) {
            LogUtils.d("检测到退出微信语音模式");
            isWeChatVoiceMode = false;
        }
        
        rootNode.recycle();
    }

    private AccessibilityNodeInfo findPressToTalkButton(AccessibilityNodeInfo root) {
        // 查找"按住说话"按钮
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText("按住 说话");
        if (list != null && !list.isEmpty()) {
            LogUtils.d("找到'按住 说话'按钮");
            return list.get(0);
        }

        // 如果没找到，尝试其他可能的文本
        list = root.findAccessibilityNodeInfosByText("按住说话");
        if (list != null && !list.isEmpty()) {
            LogUtils.d("找到'按住说话'按钮");
            return list.get(0);
        }

        LogUtils.d("未找到语音按钮");
        return null;
    }

    @Override
    public void onInterrupt() {
        LogUtils.d("无障碍服务中断");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.packageNames = new String[]{"com.tencent.mm"};
        setServiceInfo(info);
        LogUtils.d("无障碍服务已连接");
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

    public boolean isInWeChatVoiceMode() {
        return isWeChatVoiceMode;
    }
} 
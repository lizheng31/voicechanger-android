# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp 相关
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# 保留基本的网络相关类
-keep class javax.net.ssl.** { *; }
-keep class javax.net.** { *; }
-keep class java.net.** { *; }

# OkHttp 特定规则
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class okhttp3.internal.platform.Platform {
    public static okhttp3.internal.platform.Platform get();
}

# Conscrypt
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.conscrypt.KitKatPlatformOpenSSLSocketImplAdapter
-dontwarn org.conscrypt.PreKitKatPlatformOpenSSLSocketImplAdapter
-dontwarn org.conscrypt.**

# 保留 Conscrypt 相关类
-keep class org.conscrypt.** { *; }
-keepclassmembers class org.conscrypt.** { *; }

# 额外的安全相关规则
-keep class com.android.org.conscrypt.** { *; }
-keep class org.apache.harmony.xnet.provider.jsse.** { *; }
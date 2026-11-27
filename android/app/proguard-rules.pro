# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve Kotlin/Java annotations and signatures needed for JNI / reflection / JSON
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve WebView JavaScript Interface methods (AndroidYT)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve custom DotMatrix views inflated from XML layouts
-keep class com.nothing.player.DotMatrix* {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Preserve Media3 extension renderers loaded via reflection by DefaultRenderersFactory
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class androidx.media3.exoplayer.rtsp.** { *; }
-keep class androidx.media3.exoplayer.smoothstreaming.** { *; }
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.exoplayer.dash.** { *; }
-keep class androidx.media3.effect.** { *; }
-keep class androidx.media3.datasource.cache.** { *; }
-dontwarn androidx.media3.**

# Preserve LibVLC JNI bindings & callbacks
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.libvlc.**

# Preserve ML Kit Translation native models
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Unused Capacitor/Cordova bridges (stripped cleanly by R8)
-dontwarn com.getcapacitor.**
-dontwarn org.apache.cordova.**

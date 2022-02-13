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


#-keepclassmembers class com.liangke.viewpoint.behavior.GlobalBehavior$Companion {*;}

-keep class com.liangke.viewpoint.behavior.GlobalBehavior{
    public <methods>;
}

-keep class com.liangke.viewpoint.callbacks.GlobalCallbacks{
    public <methods>;
}
#保留类名及其该类的内容不会被混淆（包括变量名，方法名等）
-keep class com.liangke.viewpoint.enum.*{*;}

-keep class com.liangke.viewpoint.BuildConfig{*;}
-keep class com.liangke.viewpoint.R{*;}
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

# Razorpay ProGuard Rules
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# Keep generic Javascript interfaces (Required for Razorpay's web view)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep JSON classes (Razorpay uses them internally)
-keep class org.json.** { *; }
-keep interface org.json.** { *; }

# If you are using GSON (likely yes), keep this too just in case
-keep class com.google.gson.** { *; }
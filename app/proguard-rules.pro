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

# ==========================================
# 1. RETROFIT & COROUTINES (CRITICAL FOR <T>)
# ==========================================
# Keeps the generic signatures intact so Gson can read them
-keepattributes Signature, Exceptions, *Annotation*, EnclosingMethod, InnerClasses

# Prevents R8 from erasing Coroutines
-keep class kotlin.coroutines.Continuation

# Keep Retrofit core safe
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ==========================================
# 2. GSON REFLECTION (CRITICAL FOR <T>)
# ==========================================
# Prevent R8 from stripping generic types used by Gson's TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.Unsafe
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# ==========================================
# 3. LIVO DATA MODELS & WRAPPERS
# ==========================================
# Wildcard rule: Keeps ALL classes inside ANY folder named 'data' completely safe
-keep class com.livo.works.**.data.** { *; }

# Keep API interfaces safe
-keep interface com.livo.works.Api.** { *; }
-keep class com.livo.works.Api.** { *; }

# Keep your Utility classes safe (crucial for UiState<T>)
-keep class com.livo.works.util.** { *; }

# Keep ViewModels safe so StateFlow generics aren't mangled
-keep class com.livo.works.ViewModel.** { *; }

# ==========================================
# 4. RAZORPAY RULES
# ==========================================
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class org.json.** { *; }
-keep interface org.json.** { *; }
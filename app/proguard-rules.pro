# AI小说安卓App ProGuard配置

# ============ 通用规则 ============
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ============ 优化选项 ============
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# ============ 保留注解 ============
-keepattributes *Annotation*

# ============ 保留泛型签名 ============
-keepattributes Signature

# ============ 保留源文件名和行号（用于调试堆栈跟踪） ============
-keepattributes SourceFile,LineNumberTable

# ============ Kotlin ============
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ============ Kotlin协程 ============
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============ Kotlin序列化 ============
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.novelapp.aiagent.**$$serializer { *; }
-keepclassmembers class com.novelapp.aiagent.** {
    *** Companion;
}
-keepclasseswithmembers class com.novelapp.aiagent.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============ AndroidX Room ============
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============ Hilt ============
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep,allowobfuscation,allowshrinking class com.novelapp.aiagent.common.FragmentComponent { *; }
-keep,allowobfuscation,allowshrinking class com.novelapp.aiagent.common.ActivityComponent { *; }

# ============ Retrofit ============
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ============ OkHttp ============
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============ Moshi ============
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * {
    *;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ============ Glide ============
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind(***);
}

# ============ ffmpeg-kit ============
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# ============ 应用自有类 ============
# 保留所有数据类（用于序列化）
-keep class com.novelapp.aiagent.data.model.** { *; }

# 保留所有ViewModel
-keep class com.novelapp.aiagent.viewmodel.** { *; }

# 保留所有Repository接口
-keep interface com.novelapp.aiagent.data.repository.** { *; }

# Règles ProGuard pour DashcamApp

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.dashcam.app.models.** { *; }

# Garder les activités
-keep class com.dashcam.app.activities.** { *; }
-keep class com.dashcam.app.services.** { *; }

# ViewBinding
-keep class com.dashcam.app.databinding.** { *; }

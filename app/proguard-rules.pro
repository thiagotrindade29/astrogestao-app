# ProGuard rules for AstroGestão

# Room entities & DAOs
-keep class com.example.data.local.** { *; }

# Keep OkHttp / Kotlin Coroutines / ML Kit cleanly
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ProGuard rules for CDP Learning App Headless CMS

# Retain data models for Gson serialization
-keepclassmembers class com.cdp.learningapp.data.model.** { <fields>; }
-keep class com.cdp.learningapp.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Markwon
-dontwarn io.noties.markwon.**
-keep class io.noties.markwon.** { *; }

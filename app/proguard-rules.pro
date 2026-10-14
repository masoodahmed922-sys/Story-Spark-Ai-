# Project-specific R8 / ProGuard rules for Story Spark AI

# --- Allowed Attributes Only ---
-keepattributes Signature, *Annotation*, SourceFile, LineNumberTable

# --- Room Database Narrow Rules ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# --- Data Models (Room entities, local state models, JSON serialization) ---
-keepclassmembers class com.example.data.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.example.ai.** {
    <fields>;
    <init>(...);
}

# --- Don't warn about compile-time optional annotations ---
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.concurrent.**
-dontwarn javax.annotation.**

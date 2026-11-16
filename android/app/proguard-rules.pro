# ProGuard / R8 keep rules for Naghmeh
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void <init>();
}
-keep class com.naghmeh.app.data.** { *; }
-dontwarn androidx.media3.**

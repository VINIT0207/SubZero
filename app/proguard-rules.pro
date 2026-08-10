# ==========================================================================
# APP STARTUP ANCHORING (R8 / Proguard)
# ==========================================================================

# Explicitly keep the Application class and its lifecycle methods
-keep class com.example.subzero.SubZeroApplication {
    <init>();
    void attachBaseContext(android.content.Context);
    void onCreate();
}

# Explicitly keep the Main Activity and its constructor
-keep class com.example.subzero.MainActivity {
    <init>();
}

# Keep the AndroidX Component Factory (used for instantiation)
-keep class androidx.core.app.CoreComponentFactory { *; }

# Keep MultiDex itself just in case
-keep class androidx.multidex.** { *; }

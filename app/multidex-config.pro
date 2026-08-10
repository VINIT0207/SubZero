# ==========================================================================
# AGGRESSIVE MULTIDEX ANCHORING
# ==========================================================================

# 1. Keep ALL app source code in the primary DEX
# (This ensures MainActivity and all its internal dependencies are together)
-keep class com.example.subzero.** { *; }

# 2. Keep the AndroidX Component Factory and Base Activities
# (Required because these are the first things the system calls)
-keep class androidx.core.app.CoreComponentFactory { *; }
-keep class androidx.activity.ComponentActivity { *; }
-keep class androidx.fragment.app.FragmentActivity { *; }

# 3. Keep Lifecycle and ViewModel basics
# (MainActivity initializes these in onCreate)
-keep class androidx.lifecycle.ViewModelProvider { *; }
-keep class androidx.lifecycle.ViewModelStoreOwner { *; }
-keep class androidx.lifecycle.HasDefaultViewModelProviderFactory { *; }

# 4. Keep Biometric and Crypto initialization paths
-keep class androidx.biometric.** { *; }
-keep class com.google.crypto.tink.** { *; }

# 5. Prevent stripping of the constructor
-keepclassmembers class com.example.subzero.MainActivity {
    public <init>();
}

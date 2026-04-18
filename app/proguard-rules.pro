# ═══════════════════════════════════════════════════════════════════
#  Aether Manager — ProGuard / R8 Rules
#  @AetherDev22
# ═══════════════════════════════════════════════════════════════════

# ── Aggressive renaming ─────────────────────────────────────────────
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames

# ── Strip debug info ────────────────────────────────────────────────
-renamesourcefileattribute SourceFile
# Aktifkan ini untuk stack trace production (matikan untuk release publik):
#-keepattributes SourceFile,LineNumberTable

# ── Remove logging (semua level) ────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}

# ── Remove Kotlin null checks & intrinsics ──────────────────────────
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    ** requireNonNull(...);
}

# ── Kotlin metadata (agar reflection tetap kerja) ───────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ── Kotlin coroutines ───────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Jetpack Compose ─────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# ── Navigation Compose ──────────────────────────────────────────────
-keepnames class * extends androidx.navigation.NavArgs
-dontwarn androidx.navigation.**

# ── DataStore ───────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── ViewModel / Lifecycle ───────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}

# ── Parcelable ──────────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable

# ── Serializable ────────────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── JNI / Native ────────────────────────────────────────────────────
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── Gson ────────────────────────────────────────────────────────────
-dontwarn sun.misc.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ── Unity Ads ───────────────────────────────────────────────────────
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**

# ── Aether: StringCrypt decoder (WAJIB di-keep) ─────────────────────
# Kelas ini di-inject oleh StringEncryptTransform, jangan di-obfuscate
-keep class dev.aether.manager.security.StringCrypt { *; }

# ── Aether app entry points ─────────────────────────────────────────
-keep class dev.aether.manager.AetherApplication { *; }
-keep class dev.aether.manager.MainActivity { *; }
-keep class dev.aether.manager.SplashActivity { *; }
-keep class dev.aether.manager.SetupActivity { *; }

# ── WebView JS interface ─────────────────────────────────────────────
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#    public *;
#}

# ── Enum ────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Suppress common warnings ────────────────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

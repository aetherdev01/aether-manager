# ════════════════════════════════════════════════════════════════════════════
# AetherManager — ProGuard / R8 Rules (Full Protection)
# ════════════════════════════════════════════════════════════════════════════

# ── R8 Full Mode (aktif via gradle, bukan di sini) ───────────────────────────
# Diaktifkan lewat android.enableR8.fullMode=true di gradle.properties

# ── Keep: JNI entry points (wajib, jangan di-rename) ────────────────────────
-keep class dev.aether.manager.util.NativeExec {
    native <methods>;
    public static *;
}

# ── Keep: hanya public API yang dipanggil dari luar / reflection ─────────────
-keep class dev.aether.manager.AetherApplication { *; }

# ── Obfuscate EVERYTHING else ────────────────────────────────────────────────
# Hapus -keep class dev.aether.manager.** yang ada sebelumnya
# (terlalu lebar, mengekspos semua class)

# ── Rename / flatten package ─────────────────────────────────────────────────
-repackageclasses 'x'
-flattenpackagehierarchy 'x'
-overloadaggressively

# ── Obfuscation dictionaries ─────────────────────────────────────────────────
-obfuscationdictionary      dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# ── Aggressive optimization ──────────────────────────────────────────────────
-optimizationpasses 7
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,\
  code/removal/simple,code/removal/advanced,code/removal/exception,\
  code/removal/variable,code/merging/short,method/removal/parameter,\
  method/marking/static,method/marking/final,class/marking/final

# ── Strip semua Log call ─────────────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# ── Strip Kotlin metadata (mempersulit decompile) ────────────────────────────
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void check*(...);
    static void throw*(...);
}
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Sembunyikan source info ───────────────────────────────────────────────────
-renamesourcefileattribute ''
-keepattributes !SourceFile, !SourceDir

# Keep LineNumberTable hanya untuk crash reporting — hapus jika tidak perlu
# -keepattributes LineNumberTable

# ── Compose — hanya keep yang strictly perlu ─────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── AdMob / Google Ads ────────────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# ── AndroidX / Lifecycle ──────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.datastore.** { *; }

# ── Serialization / Parcelize ─────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ── Enum (Kotlin enum harus di-keep nama) ────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Resource adaptation ───────────────────────────────────────────────────────
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# ── Suppress warnings library pihak ketiga ───────────────────────────────────
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class dev.aether.manager.** { *; }
-keepclassmembers class dev.aether.manager.** { *; }

-keep class androidx.compose.** { *; }
-dontwarn kotlinx.**

-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

-overloadaggressively
-flattenpackagehierarchy
-repackageclasses ''

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents
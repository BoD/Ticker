# Global
-dontobfuscate
-keepattributes SourceFile, LineNumberTable
-dontoptimize
-dontnote **

# Twitter4J
-dontwarn twitter4j.**
-keep class twitter4j.conf.PropertyConfigurationFactory
-keep class twitter4j.** { *; }

# Jraf Util
-dontwarn org.jraf.android.util.**

# Klaxon
-dontwarn kotlin.reflect.**

# Libticker
-keep class org.jraf.libticker.plugin.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

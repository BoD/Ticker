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
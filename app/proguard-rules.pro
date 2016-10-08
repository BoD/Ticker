# Do not obfuscate
-dontobfuscate

# Resolves some obscure proguard/dex problem that breaks the build
# (See http://stackoverflow.com/a/7587680/15695)
-optimizations !code/allocation/variable

# Keep line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# For Twitter4J
-dontwarn twitter4j.**
-keep class twitter4j.conf.PropertyConfigurationFactory
-keep class twitter4j.** { *; }

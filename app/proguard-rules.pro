# Personal-Tracker ProGuard / R8 Rules

# SQLCipher / Zetetic JNI & Database reflection preservation
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class net.zetetic.** { *; }
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }

# Rive runtime JNI & reflection preservation
-keep class app.rive.runtime.** { *; }
-keep class com.android.tools.r8.** { *; }

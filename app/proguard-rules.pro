# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Strip debug/verbose/info logging from release builds entirely. Sentinelle
# handles call metadata and SMS content, and none of that belongs in logcat
# on a user's device. Warnings and errors are kept — they're what makes a
# real bug report readable. This relies on proguard-android-optimize.txt
# being in use (see build.gradle.kts); -assumenosideeffects is a no-op
# without R8 optimization enabled.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Gson populates these from JSON by reflection, so field names have to
# survive shrinking and renaming.
-keep class com.sentinelle.app.network.** { *; }

# WorkManager instantiates workers from their class name.
-keep class com.sentinelle.app.worker.** { *; }

# Room entities are read back through generated code, but the column
# mapping is name-based — keep the fields rather than risk a mismatch on a
# database that already exists on users' devices.
-keep class com.sentinelle.app.data.** { *; }

# Everything else (ui, util, spam, billing, arcep, config) is free to be
# shrunk and renamed. The previous blanket `-keep class com.sentinelle.app.**`
# kept every class and member, which disabled shrinking for the whole app.
# Obfuscation is beside the point here — the source is public under GPLv3 —
# but dead-code removal is not.

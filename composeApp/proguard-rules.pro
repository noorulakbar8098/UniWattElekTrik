# ─────────────────────────────────────────────────────────────────────────────
#  UniWatt ElekTrik — R8 / ProGuard rules
#
#  Anything reflectively accessed at runtime (Firebase POJOs, GitLive Firebase
#  KMP wrappers, JSON binding via reflection, JNI native methods, etc.) MUST
#  be kept here or R8 will strip / rename it and the release build will crash
#  with `NoClassDefFoundError` / `MissingFieldException`.
#
#  Default Android optimisation rules already provide a sensible baseline;
#  this file layers project-specific keeps on top.
# ─────────────────────────────────────────────────────────────────────────────

# Keep source line info so Crashlytics / `adb logcat` stack traces are useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve generic signatures (Firestore needs them to deserialise typed lists).
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Keep the Application + components — the manifest references them by name.
-keep class com.example.uniwattelektrik.platform.MainActivity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }

# ─── Kotlin / Coroutines ────────────────────────────────────────────────────
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.AgentPremain

# ─── Compose ────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Firebase ──────────────────────────────────────────────────────────────
# Auth / Firestore / Messaging / Performance all use reflection on annotated
# fields and proto generated classes.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore reflectively reads/writes data classes annotated with @PropertyName,
# and constructs POJOs via no-arg constructors — keep both.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.IgnoreExtraProperties <methods>;
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
}
# Keep our own Firestore data classes — anything that gets `.toObject(X::class)`'d.
-keep class com.example.uniwattelektrik.feature.workforce.data.remote.** { *; }
-keep class com.example.uniwattelektrik.feature.**.data.remote.** { *; }
-keepclassmembers class com.example.uniwattelektrik.** {
    public <init>();
    public <init>(...);
}

# Firebase Performance instruments classes via bytecode rewriting at build time
# — keep the generated instrumentation refs.
-keep class com.google.firebase.perf.** { *; }
-dontwarn com.google.firebase.perf.**

# ─── GitLive Firebase (KMP wrapper) ────────────────────────────────────────
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ─── Coil 3 (image loading) ────────────────────────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── OSMDroid (offline maps) ───────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ─── fastexcel-reader + StAX (xlsx import) ─────────────────────────────────
# StAX uses ServiceLoader to discover XML factory implementations; without
# these keeps R8 strips the providers and xlsx parsing throws at runtime.
-keep class org.dhatim.fastexcel.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-keep class org.codehaus.stax2.** { *; }
-keep class javax.xml.stream.** { *; }
-dontwarn org.dhatim.fastexcel.**
-dontwarn com.fasterxml.aalto.**
-dontwarn org.codehaus.stax2.**
-dontwarn javax.xml.stream.**

# Generic ServiceLoader keep — covers any META-INF/services/* providers.
-keep class * implements java.util.spi.ResourceBundleControlProvider
-keepclassmembers class * {
    public static ** valueOf(java.lang.String);
}

# ─── Play Services (location) ──────────────────────────────────────────────
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# ─── Lifecycle / ViewModel ─────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ─── Enum classes — keep `values()` / `valueOf(String)` accessible ─────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Native / JNI ──────────────────────────────────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ─── Parcelable + Serializable ─────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ─── Quiet down false-positive warnings from optional deps ─────────────────
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.invoke.**


# ===================================================================
# R8 / ProGuard Configuration for Line2Box
# ===================================================================

# Preserve source file and line numbers for de-obfuscation in crash reports (e.g. Firebase Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations and generic signatures for serialization & reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ===================================================================
# Data Models & Serialization
# ===================================================================

# Keep all models / data classes and their members for Firebase Realtime Database,
# Cloud Firestore, Gson serialization, and Kotlin reflection (asMap)
-keep class com.diu.yk_games.line2box.model.** {
    <fields>;
    <init>(...);
    *** get*();
    void set*(***);
    boolean is*();
}

-keepclassmembers,allowoptimization class com.diu.yk_games.line2box.notification.data.NotificationDatabase_Impl { <init>(); }

# Preserve Firebase Database and Firestore annotated properties across any package
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
    @com.google.firebase.database.IgnoreExtraProperties <init>(...);
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.IgnoreExtraProperties <init>(...);
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Preserve Gson @SerializedName fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===================================================================
# Kotlin Reflection
# ===================================================================

# Retain Kotlin Metadata for kotlin-reflect (used by T.asMap() via memberProperties)
-keep class kotlin.Metadata { *; }

# ===================================================================
# Custom Views & UI Components
# ===================================================================

# Preserve custom views and constructors instantiated via XML layout files
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ===================================================================
# Parcelize / parcelableCreator<T>() reflection support
# ===================================================================
# kotlinx.parcelize.parcelableCreator() does T::class.java.getField("CREATOR")
# at runtime — explicitly guard CREATOR on every Parcelable, not just the
# model package, since the reflection isn't visible to R8's call graph.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable

# Custom Parceler objects resolved by kotlin-parcelize codegen for
# PersistentList<T> fields — keep them intact so the generated
# create()/write() calls stay wired to the right INSTANCE.
-keep class com.diu.yk_games.line2box.base.*Parceler* { *; }
-keep class com.diu.yk_games.line2box.base.PersistentListParceler { *; }

# ===================================================================
# kotlinx.collections.immutable — PersistentList/PersistentVector
# ===================================================================
# Pure-JVM library shipped without embedded R8 rules; its internal
# trie/vector implementations aren't safe to obfuscate/optimize.
-keep class kotlinx.collections.immutable.** { *; }
-keep interface kotlinx.collections.immutable.** { *; }
-dontwarn kotlinx.collections.immutable.**
# Add project specific ProGuard rules here.

# Room
-keep class androidx.room.** { *; }

# Keep data/entity classes used by Room + kotlinx.serialization reflection
-keep class com.adaptiveoperator.ai.memory.db.entity.** { *; }
-keep class com.adaptiveoperator.ai.ai.tools.** { *; }

# LiteRT-LM JNI bindings
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

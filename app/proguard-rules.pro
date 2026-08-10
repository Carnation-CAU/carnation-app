# kotlinx.serialization: keep generated serializers for @Serializable classes.
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.carnation.fallalert.**$$serializer { *; }
-keepclassmembers class com.carnation.fallalert.** {
    *** Companion;
}

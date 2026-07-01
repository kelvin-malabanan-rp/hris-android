# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.rocketpartners.hris.** {
    *** Companion;
}
-keepclasseswithmembers class io.rocketpartners.hris.** {
    kotlinx.serialization.KSerializer serializer(...);
}

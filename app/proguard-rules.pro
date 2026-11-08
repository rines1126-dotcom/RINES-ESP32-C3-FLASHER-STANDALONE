# RINES ESP32-C3 FLASHER
# Keep application classes and members.
-keep class com.rines.esp32c3flasher.** { *; }

# Keep USB Serial library classes.
-keep class com.hoho.android.usbserial.** { *; }

# Preserve annotations and generic signatures.
-keepattributes *Annotation*
-keepattributes Signature

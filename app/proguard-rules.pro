# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# LiteRT-LM (on-device LLM)
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
-keepclasseswithmembernames class * { native <methods>; }

# LuaJ optional javax.script bridge
-dontwarn javax.script.**

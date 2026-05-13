#  LiteRT-LM (on-device Gemma inference — uses JNI/reflection)
-keep class com.google.ai.edge.litert.** { *; }
-keep class com.google.ai.edge.litertlm.** { *; }

#  Sherpa-ONNX (offline speech — JNI native bridge)
-keep class com.k2fsa.sherpa.onnx.** { *; }

#  Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.isharaai.isl.core.db.** { *; }

#  OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

#  Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

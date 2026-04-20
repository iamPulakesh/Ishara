#  LiteRT 
-keep class com.google.ai.edge.litert.** { *; }
-keep class com.google.ai.edge.litert.lm.** { *; }

#  Gson 
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.isharaai.isl.inference.GemmaResponse { *; }

#  Room 
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.isharaai.isl.data.db.** { *; }

#  OkHttp 
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

#  Hilt 
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

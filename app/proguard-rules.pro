# Novelo Proguard & R8 Keep Rules
# Target: Release Candidate Hardening

# Preserve Kotlin Sequences, Coroutines and internal state machines
-keep class kotlin.sequences.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.internal.** { *; }

# Room SQLite Database & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# WorkManager Workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Novelo Domain Models & Room Entities (for SAF export/import and Room type converters)
-keep class com.example.nuvilo.domain.model.** { *; }
-keep class com.example.nuvilo.data.local.entity.** { *; }

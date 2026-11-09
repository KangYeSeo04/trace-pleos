package com.tracers.trace.data.record

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** 앱 로컬 DB. 기기 밖으로 보내지 않는다. */
@Database(
    entities = [TripSummaryEntity::class, ChargeSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TraceDatabase : RoomDatabase() {
    abstract fun tripSummaryDao(): TripSummaryDao
    abstract fun chargeSessionDao(): ChargeSessionDao

    companion object {
        fun create(context: Context): TraceDatabase =
            Room.databaseBuilder(context.applicationContext, TraceDatabase::class.java, "trace.db")
                .build()
    }
}

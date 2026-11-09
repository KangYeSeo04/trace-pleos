package com.tracers.trace.data.record

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 앱 로컬 DB. 기기 밖으로 보내지 않는다. */
@Database(
    entities = [TripSummaryEntity::class, ChargeSessionEntity::class, TripInProgressEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TraceDatabase : RoomDatabase() {
    abstract fun tripSummaryDao(): TripSummaryDao
    abstract fun chargeSessionDao(): ChargeSessionDao
    abstract fun tripInProgressDao(): TripInProgressDao

    companion object {
        fun create(context: Context): TraceDatabase =
            Room.databaseBuilder(context.applicationContext, TraceDatabase::class.java, "trace.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** 1 → 2: 진행 중 주행 요약 한 줄 테이블 추가 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trip_in_progress` (" +
                        "`id` INTEGER NOT NULL, `startedAtMs` INTEGER NOT NULL, `endedAtMs` INTEGER NOT NULL, " +
                        "`distanceKm` REAL NOT NULL, `energyUsedKwh` REAL NOT NULL, " +
                        "`hardAccelCount` INTEGER NOT NULL, `regenKwh` REAL NOT NULL, `hvacOnSec` INTEGER NOT NULL, " +
                        "`startBatteryPercent` INTEGER, `endBatteryPercent` INTEGER, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}

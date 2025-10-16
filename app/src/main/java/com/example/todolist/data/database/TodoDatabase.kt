package com.example.todolist.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        // 数据库版本迁移：从版本1到版本2（添加闹钟字段）
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加闹钟相关字段
                database.execSQL("ALTER TABLE tasks ADD COLUMN alarmTime INTEGER")
                database.execSQL("ALTER TABLE tasks ADD COLUMN hasAlarm INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
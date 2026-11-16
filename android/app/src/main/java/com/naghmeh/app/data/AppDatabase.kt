package com.naghmeh.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [SongEntity::class, MediaTrackEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "naghmeh_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val dao = database.songDao()
                    val hafezCategoryId = dao.insertCategory(
                        CategoryEntity(name = "شعر حافظ", createdAt = System.currentTimeMillis())
                    )
                    val classicCategoryId = dao.insertCategory(
                        CategoryEntity(name = "اشعار کلاسیک", createdAt = System.currentTimeMillis() + 100L)
                    )

                    dao.insertSong(
                        SongEntity(
                            title = "الا یا ایها الساقی (حافظ)",
                            lyrics = """
                                الا یا ایها الساقی ادر کاساً و ناولها
                                که عشق آسان نمود اول ولی افتاد مشکل‌ها

                                به بوی نافه‌ای کاخر صبا زان طره بگشاید
                                ز تاب جعد مشکینش چه خون افتاد در دل‌ها

                                مرا در منزل جانان چه امن عیش چون هر دم
                                جرس فریاد می‌دارد که بربندید محمل‌ها
                            """.trimIndent(),
                            categoryId = hafezCategoryId,
                            fontSize = 22f,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    dao.insertSong(
                        SongEntity(
                            title = "بوی جوی مولیان (رودکی)",
                            lyrics = """
                                بوی جوی مولیان آید همی
                                یاد یار مهربان آید همی

                                ریگ آموی و درشتی راه او
                                زیر پایم پرنیان آید همی

                                آب جیحون از نشاط روی دوست
                                خنگ ما را تا میان آید همی
                            """.trimIndent(),
                            categoryId = classicCategoryId,
                            fontSize = 22f,
                            createdAt = System.currentTimeMillis() - 1000L
                        )
                    )
                }
            }
        }
    }
}

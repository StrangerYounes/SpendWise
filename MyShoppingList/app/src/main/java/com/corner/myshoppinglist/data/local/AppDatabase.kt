package com.corner.myshoppinglist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.corner.myshoppinglist.data.local.dao.*
import com.corner.myshoppinglist.data.local.entities.*

@Database(
    entities = [ShoppingList::class, ShoppingItem::class, MasterItem::class, Photo::class, AppSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun masterItemDao(): MasterItemDao
    abstract fun photoDao(): PhotoDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_list_database"
                )
                .fallbackToDestructiveMigration() // Simple for this challenge
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

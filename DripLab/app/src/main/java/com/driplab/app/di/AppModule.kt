package com.driplab.app.di

import android.content.Context
import androidx.room.Room
import com.driplab.app.core.database.DripLabDatabase
import com.driplab.app.core.database.dao.BrewNoteDao
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.data.PresetRecipeInitializer
import com.driplab.app.data.repository.BrewNoteRepositoryImpl
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.domain.repository.BrewNoteRepository
import com.driplab.app.domain.repository.RecipeRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        presetInitializer: PresetRecipeInitializer
    ): DripLabDatabase {
        val db = Room.databaseBuilder(
            context,
            DripLabDatabase::class.java,
            "driplab.db"
        ).fallbackToDestructiveMigration().build()
        presetInitializer.initialize(db)
        return db
    }

    @Provides
    fun provideRecipeDao(database: DripLabDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    fun provideBrewNoteDao(database: DripLabDatabase): BrewNoteDao {
        return database.brewNoteDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().setPrettyPrinting().create()
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideBrewNoteRepository(impl: BrewNoteRepositoryImpl): BrewNoteRepository {
        return impl
    }
}
package com.money.dividr.di // Or your appropriate DI package

import com.money.dividr.data.repository.ExpenseRepositoryImpl
import com.money.dividr.data.repository.FirestoreGroupRepositoryImpl
import com.money.dividr.domain.repository.ExpenseRepository
import com.money.dividr.domain.repository.GroupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {


    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        firestoreExpenseRepositoryImpl: ExpenseRepositoryImpl // Corrected class name
    ): ExpenseRepository

}

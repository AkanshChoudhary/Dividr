package com.money.dividr.di

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.money.dividr.data.repository.FirestoreGroupRepositoryImpl
import com.money.dividr.domain.repository.GroupRepository
import com.money.dividr.presentation.signin.GoogleAuthUiClient
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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideGroupRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): GroupRepository {
        return FirestoreGroupRepositoryImpl(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideSignInClient(@ApplicationContext context: Context): SignInClient {
        return Identity.getSignInClient(context)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthUiClient(
        @ApplicationContext context: Context,
        oneTapClient: SignInClient,
        firestore: FirebaseFirestore // Added Firestore parameter
    ): GoogleAuthUiClient {
        return GoogleAuthUiClient(context, oneTapClient, firestore) // Pass Firestore
    }


}

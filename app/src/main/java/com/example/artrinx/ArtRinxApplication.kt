package com.example.artrinx

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "artrinx_prefs")

@HiltAndroidApp
class ArtRinxApplication : Application()

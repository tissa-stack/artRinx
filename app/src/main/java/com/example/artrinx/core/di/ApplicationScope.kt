package com.example.artrinx.core.di

import javax.inject.Qualifier

/** Qualifies the process-lifetime [kotlinx.coroutines.CoroutineScope] (survives ViewModel/nav). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

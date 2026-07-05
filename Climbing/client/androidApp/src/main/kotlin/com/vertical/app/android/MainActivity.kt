package com.vertical.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vertical.app.VerticalApp
import com.vertical.app.core.storage.PlatformSessionStorage
import com.vertical.app.di.initKoin
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
        PlatformSessionStorage.initialize(applicationContext)
        initKoin()
        setContent { VerticalApp() }
    }
}

package com.roklab.btcface

import android.app.Application
import com.roklab.btcface.worker.BTCPriceSyncWorker

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Schedule periodic BTC price sync
        BTCPriceSyncWorker.schedule(this)
    }
}

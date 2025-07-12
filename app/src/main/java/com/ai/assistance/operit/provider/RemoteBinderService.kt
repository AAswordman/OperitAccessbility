package com.ai.assistance.operit.provider

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RemoteBinderService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return UIAccessibilityService.service?.binder
    }
} 

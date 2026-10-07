package com.example

import android.app.Application
import android.system.Os

class WebToApkApplication : Application() {

    companion object {
        init {
            try {
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
            } catch (ignored: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
            Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
        } catch (ignored: Throwable) {}
    }
}

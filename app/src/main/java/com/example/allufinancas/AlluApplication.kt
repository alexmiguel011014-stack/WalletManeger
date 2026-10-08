package com.example.allufinancas

import android.app.Application
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.DemoRepository

class AlluApplication : Application() {
  val demoDataSource: DemoDataSource by lazy { DemoRepository(this) }
}

package com.example.warehousescanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        // Мы убираем AppBarConfiguration, чтобы избежать любых возможных конфликтов с темой
        navView.setupWithNavController(navController)
    }
}
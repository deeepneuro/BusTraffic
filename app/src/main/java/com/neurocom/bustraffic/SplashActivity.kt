package com.neurocom.bustraffic

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neurocom.bustraffic.databinding.ActivityDirectBinding
import com.neurocom.bustraffic.databinding.ActivitySplashBinding
import java.util.Timer
import java.util.TimerTask

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var timer: Timer? = null
    private var currentIndex = 0
    private var stringDirectAdd: String? = null
    private var isAnimationFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //анимация отображения текста
        stringDirectAdd = "BusTraffic"
        startTextAnimation()



        //переход к следующей активности
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAnimationFinished) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 1100)
    }

    //функция для анимации набора текста
    private fun startTextAnimation() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    stringDirectAdd?.let {
                        if (currentIndex < it.length) {
                            binding.tvSplashActivity.text = it.substring(0, currentIndex + 1)
                            currentIndex++
                        } else {
                            timer?.cancel()
                            isAnimationFinished = true
                        }
                    }
                }
            }
        }, 100, 50)
    }
}

package com.neurocom.bustraffic

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neurocom.bustraffic.databinding.ActivityWorkWeekendDayBinding
import java.util.Timer
import java.util.TimerTask

class WorkWeekendDay : AppCompatActivity() {

    private lateinit var binding: ActivityWorkWeekendDayBinding
    private var currentIndex = 0
    private var stringDirectAdd: String? = null
    private var timer: Timer? = null
    private var isAnimationFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkWeekendDayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dataNumRoute = intent.getStringExtra("keyRoute")
        val dataDirectRoute = intent.getStringExtra("keyDirect")
        val stringTextRoute = intent.getStringExtra("stringTextRoute")
        val stringDirect = intent.getStringExtra("stringDirect")

        when (dataNumRoute) {
            "route4", "route5", "route10" -> {
                binding.frameForTvOffDay.apply {

                    isClickable = false
                    isFocusable = false  //Отключения интерактивности
                }
                binding.tvDayOff.apply {
                    setOnClickListener(null)
                    background = ContextCompat.getDrawable(this@WorkWeekendDay, R.drawable.bg_for_day_off_tv)
                    text = getString(R.string.no_weekend_routes)

                }
            }

        }

        binding.tvRouteInfo.text = stringTextRoute

        stringDirectAdd = "Направление:\n${stringDirect ?: "не указано"}"

        if (savedInstanceState == null) {
            startTextAnimation()
        } else {
            currentIndex = savedInstanceState.getInt("currentIndex", 0)
            stringDirectAdd = savedInstanceState.getString("stringDirectAdd")
            isAnimationFinished = savedInstanceState.getBoolean("isAnimationFinished", false)
            if (!isAnimationFinished) {
                startTextAnimation()
            } else {
                binding.tvDirectInfo.text = stringDirectAdd
            }
        }

        // Анимация нажатия и переход на новый экран для frameForTvWorkDay
        binding.frameForTvWorkDay.setOnClickListener {
            it.isEnabled = false // Отключение повторного нажатия
            animateAndNavigate(it, dataNumRoute, dataDirectRoute, "workDay")
        }

        // Анимация нажатия и переход на новый экран для frameForTvOffDay
        binding.frameForTvOffDay.setOnClickListener {
            it.isEnabled = false // Отключение повторного нажатия
            animateAndNavigate(it, dataNumRoute, dataDirectRoute, "offDay")
        }
    }

    private fun animateAndNavigate(view: View, dataNumRoute: String?, dataDirectRoute: String?, dayType: String) {
        view.animate().apply {
            duration = 150
            scaleXBy(0.1f)
        }.withEndAction {
            view.animate().apply {
                duration = 150
                scaleXBy(-0.1f)
            }.withEndAction {
                // Переход на новый экран после завершения анимации
                val intent = Intent(this@WorkWeekendDay, InfoActivity::class.java).apply {
                    putExtra("keyRoute", dataNumRoute)
                    putExtra("keyDirect", dataDirectRoute)
                    putExtra("keyDay", dayType)
                }
                startActivity(intent)
                view.isEnabled = true // Включение возможности нажатия после возвращения на экран
            }
        }
    }

    private fun startTextAnimation() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    stringDirectAdd?.let {
                        if (currentIndex < it.length) {
                            binding.tvDirectInfo.text = it.substring(0, currentIndex + 1)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentIndex", currentIndex)
        outState.putString("stringDirectAdd", stringDirectAdd)
        outState.putBoolean("isAnimationFinished", isAnimationFinished)
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}

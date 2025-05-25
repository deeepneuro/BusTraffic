package com.neurocom.bustraffic

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neurocom.bustraffic.databinding.ActivityDirectBinding
import java.util.Timer
import java.util.TimerTask

class DirectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDirectBinding
    private var currentIndex = 0
    private var textRoute: String? = null
    private var timer: Timer? = null
    private var isAnimationFinished = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDirectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Получение данных из предыдущей активности о номере маршрута
        val dataNumRoute = intent.getStringExtra("keyRoute")

        //Загрузка данных из JSON
        val routeSchedule = loadRouteSchedule(this)
        val route = routeSchedule.routes[dataNumRoute]

        //начальная и конечная остановка прямого направления
        val point1 = route?.stopTherePoint?.get(0)
        val point2 = route?.stopTherePoint?.last()
        //начальная и конечная остановка обратного направления
        val point3 = route?.stopBackPoint?.get(0)
        val point4 = route?.stopBackPoint?.last()

        //Функция удаления первых пяти символов
        fun removeFirstFive(input: String): String {
            return input.substring(5)
        }

        val formattedDataNumRoute = removeFirstFive(dataNumRoute.toString())
        textRoute = "Маршрут №$formattedDataNumRoute"

        if (savedInstanceState == null) {
            startTextAnimation()
        } else {
            currentIndex = savedInstanceState.getInt("currentIndex", 0)
            textRoute = savedInstanceState.getString("textRoute")
            isAnimationFinished = savedInstanceState.getBoolean("isAnimationFinished", false)
            if (!isAnimationFinished) {
                startTextAnimation()
            } else {
                binding.tvRouteInfo.text = textRoute
            }
        }

        binding.tvDirect1.text = "$point1 - $point2"
        binding.tvDirect2.text = "$point3 - $point4"

        val directThere = "$point1 - $point2"
        val directBack = "$point2 - $point1"

        //Анимация нажатия и переход на новый экран для tvDirect1
        binding.frameFortvDirect1.setOnClickListener {
            it.isEnabled = false //Отключение повторного нажатия
            animateAndNavigate(it, directThere, dataNumRoute, "there", textRoute)
        }

        //Анимация нажатия и переход на новый экран для tvDirect2
        binding.frameFortvDirect2.setOnClickListener {
            it.isEnabled = false //Отключение повторного нажатия
            animateAndNavigate(it, directBack, dataNumRoute, "back", textRoute)
        }
    }

    private fun animateAndNavigate(view: View, direct: String, dataNumRoute: String?, direction: String, textRoute: String?) {
        view.animate().apply {
            duration = 150
            scaleXBy(0.1f)
        }.withEndAction {
            view.animate().apply {
                duration = 150
                scaleXBy(-0.1f)
            }.withEndAction {
                //Переход на новый экран после завершения анимации
                val intent = Intent(this@DirectActivity, WorkWeekendDay::class.java).apply {
                    putExtra("keyRoute", dataNumRoute)
                    putExtra("keyDirect", direction)
                    putExtra("stringDirect", direct)
                    putExtra("stringTextRoute", textRoute)
                }
                startActivity(intent)
                view.isEnabled = true //Включение возможности нажатия после возвращения на экран
            }
        }
    }

    private fun startTextAnimation() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    textRoute?.let {
                        if (currentIndex < it.length) {
                            binding.tvRouteInfo.text = it.substring(0, currentIndex + 1)
                            currentIndex++
                        } else {
                            timer?.cancel()
                            isAnimationFinished = true
                        }
                    }
                }
            }
        }, 100, 100)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentIndex", currentIndex)
        outState.putString("textRoute", textRoute)
        outState.putBoolean("isAnimationFinished", isAnimationFinished)
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}

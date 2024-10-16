package com.neurocom.bustraffic

import RouteAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.neurocom.bustraffic.databinding.ActivityInfoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding
    private lateinit var bus: Bus
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var timeHandler: Handler
    private lateinit var timeRunnable: Runnable
    private var isAnimating = false
    private var isCardView = false
    private val updateJob = Job()
    private val updateScope = CoroutineScope(Dispatchers.Main + updateJob)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dataNumRoute = intent.getStringExtra("keyRoute")
        val dataDirectRoute = intent.getStringExtra("keyDirect")
        val dataDayRoute = intent.getStringExtra("keyDay")

        val routeSchedule = loadRouteSchedule(this)
        val route = routeSchedule.routes[dataNumRoute]
        val routeThere = route?.there ?: emptyList() // загрузка временных меток для будних дней из JSON. Используем emptyList() в случае отсутствия данных
        val routeBack = route?.back ?: emptyList() // загрузка временных меток для будних дней из JSON. Используем emptyList() в случае отсутствия данных
        val routeThereDayOff = route?.thereDayOff ?: emptyList() // загрузка временных меток для выходных дней из JSON. Используем emptyList() в случае отсутствия данных
        val routeBackDayOff = route?.backDayOff ?: emptyList() // загрузка временных меток для выходных дней из JSON. Используем emptyList() в случае отсутствия данных

//        val point1 = route?.point1?.get(0)
//        val point2 = route?.point2?.get(0)

        val stopTherePointCount = route?.stopTherePoint?.size
        val stopBackPointCount = route?.stopBackPoint?.size


        val directContent = when {
            dataDayRoute == "workDay" && dataDirectRoute == "there" -> routeThere
            dataDayRoute == "workDay" && dataDirectRoute == "back" -> routeBack
            dataDayRoute == "offDay" && dataDirectRoute == "there" -> routeThereDayOff
            dataDayRoute == "offDay" && dataDirectRoute == "back" -> routeBackDayOff
            else -> emptyList()
        }



        val stopCount: Int? = when (dataDirectRoute) {
            "there" -> stopTherePointCount
            "back" -> stopBackPointCount
            else -> 0
        }

        val stopNames: List<String> = if (dataDirectRoute == "there") {
            route?.stopTherePoint ?: emptyList()
        } else {
            route?.stopBackPoint ?: emptyList()
        }

        // Получаем durationTime из JSON
        val durationTime: Int = when(dataDirectRoute){
            "there" -> route?.durationTimeThere ?: 0
            "back" -> route?.durationTimeBack ?: 0
            else -> 0
        }





        // Инициализация RecyclerView
        binding.recyclerViewRoutes.layoutManager = LinearLayoutManager(this)



        // Выбор данных для отображения
        val routeTimes = when {
            dataDayRoute == "workDay" && dataDirectRoute == "there" -> routeThere
            dataDayRoute == "workDay" && dataDirectRoute == "back" -> routeBack
            dataDayRoute == "offDay" && dataDirectRoute == "there" -> routeThereDayOff
            dataDayRoute == "offDay" && dataDirectRoute == "back" -> routeBackDayOff
            else -> emptyList()
        }

        // Создание и установка адаптера для RecyclerView
        val adapter = RouteAdapter(routeTimes)
        binding.recyclerViewRoutes.adapter = adapter







    // Создаем объект Bus с параметром durationTime
        bus = Bus(directContent.map { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }, durationTime)

        val currentTime = LocalTime.now()
        val timeInTransit = bus.getTimeInTransit(currentTime)
        val timeToNextDeparture = bus.getTimeToNextDeparture(currentTime)
        val recentBuses = bus.getRecentBuses(currentTime)

        addStopPoints(stopCount ?: 0, stopNames)
        addBusIcons(recentBuses)


        // Функция удаления первых пяти символов
        fun removeFirstFive(input: String): String {
            return input.substring(5)
        }

        val formattedDataNumRoute = removeFirstFive(dataNumRoute.toString())
        val textRoute = "На маршруте\n№$formattedDataNumRoute:"


        with(binding) {
            tvTextForRace.text = textRoute
//            infoTv1.text = "Время в пути: $timeInTransit минут"
//            //infoTv2.text = "Время до следующего рейса: $timeToNextDeparture минут"
//            infoTv3.text = directContent[0]
//            infoTv4.text = stopTherePointCount.toString() + " - " + stopBackPointCount.toString()
            tvTextForBus.text = "Следующий\nрейс через:"
//            Log.d("MyTag", "количество остановок: ${infoTv4.text}")



            if (recentBuses.isEmpty()) {
                tvTimeForRace.text = "Автобусов\nнет"
            } else {
                val formattedTimes = recentBuses.joinToString("\n") { (time, _) ->
                    "Рейс: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                }
                tvTimeForRace.text = formattedTimes
            }
        }


        // Настройка RecyclerView

        binding.recyclerViewRoutes.layoutManager = LinearLayoutManager(this)

        // Настройка click listener для tvShedule
        binding.frameForTvShedule.setOnClickListener {
            if (!isAnimating) {
                isAnimating = true

                binding.frameForTvShedule.animate().apply {
                    duration = 150
                    scaleXBy(0.1f)
                }.withEndAction {
                    binding.frameForTvShedule.animate().apply {
                        duration = 150
                        scaleXBy(-0.1f)
                    }.withEndAction {
                        isAnimating = false
                    }
                }

                toggleRecyclerViewVisibility()
                performVibration()
            }
        }



        binding.ivBusLine.post {
            updateBusPositions()
        }

//        handler = Handler(Looper.getMainLooper())
//        runnable = object : Runnable {
//            override fun run() {
//                updateBusPositions()
//                handler.postDelayed(this, 1000) // Обновление каждые 5 секунд
//            }
//        }
//        handler.post(runnable)

        //Обновление позиций автобусов
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                updateBusPositions()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)




        //Обновление секундомера (время до следующего рейса)
        timeHandler = Handler(Looper.getMainLooper())
        timeRunnable = object : Runnable {
            override fun run() {
                updateTimeDisplays()
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable)
    }

    //Функция анимации CardView
    private fun toggleRecyclerViewVisibility() {
        val cardView = binding.cardView

        if (isCardView) {
            // Скрываем CardView
            val hideAnimator = ObjectAnimator.ofFloat(cardView, "alpha", 1f, 0f)
            hideAnimator.duration = 300
            hideAnimator.interpolator = AccelerateDecelerateInterpolator()
            hideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    cardView.visibility = View.GONE
                }
            })
            hideAnimator.start()
        } else {
            //RecyclerView visible
            cardView.alpha = 0f
            cardView.visibility = View.VISIBLE
            val showAnimator = ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f)
            showAnimator.duration = 300
            showAnimator.interpolator = AccelerateDecelerateInterpolator()
            showAnimator.start()
        }

        isCardView = !isCardView
    }

    private fun performVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }



    private fun updateTimeDisplays() {
        val currentTime = LocalTime.now()

        // Обновляем tvTimeForBus
        val timeToNextDeparture = bus.getTimeToNextDeparture(currentTime)
        val hours = timeToNextDeparture / 3600
        val minutes = (timeToNextDeparture % 3600 / 60).toInt()
        val seconds = timeToNextDeparture % 60
        val formattedTime = String.format(Locale.US,"%02d:%02d:%02d", hours, minutes, seconds)
        binding.tvTimeForBus.text = formattedTime

        // Обновляем tvTimeForRace
        val recentBuses = bus.getRecentBuses(currentTime)
        if (recentBuses.isEmpty()) {
            binding.tvTimeForRace.text = "Автобусов\nнет"
        } else {
            val formattedTimes = recentBuses.joinToString("\n") { (time, _) ->
                "Рейс ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }
            binding.tvTimeForRace.text = formattedTimes
        }
    }

    private fun addBusIcons(recentBuses: List<Pair<LocalTime, Double>>) {
        val busLine = binding.ivBusLine
        val busLineHeight = busLine.height.toFloat() - 40F
        Log.d("InfoActivity", "busLineHeight: $busLineHeight")

        recentBuses.forEach { ( _, timeInTransit) ->
            val busIcon = ImageView(this@InfoActivity).apply {
                setImageResource(R.drawable.group_3)
                val busIconSize = resources.getDimensionPixelSize(R.dimen.bus_geo_num_size)
                layoutParams = ConstraintLayout.LayoutParams(busIconSize, busIconSize)
                tag = "busIcon"
            }

            val progress = timeInTransit.toFloat() / bus.durationTime.toFloat()
            Log.d("InfoActivity", "duration time: ${bus.durationTime.toFloat()}")
            val startYPosition = busLineHeight * progress

            (busIcon.layoutParams as ConstraintLayout.LayoutParams).apply {
                topToTop = busLine.id
                startToStart = busLine.id
                endToEnd = busLine.id
                topMargin = startYPosition.toInt() - 40
            }

            binding.main.addView(busIcon)
            val endYPosition = startYPosition + (busLineHeight / bus.durationTime.toFloat())
            val animator = ObjectAnimator.ofFloat(busIcon, "translationY", 0f, endYPosition - startYPosition)
            animator.apply {
                duration = 71000
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun updateBusPositions() {
        val currentTime = LocalTime.now()
        val recentBuses = bus.getRecentBuses(currentTime)

        // Удаляем все существующие иконки автобусов перед добавлением новых
        removeBusIcons()

        // Добавляем обновленные иконки автобусов
        addBusIcons(recentBuses)
    }

    private fun removeBusIcons() {
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until binding.main.childCount) {
            val child = binding.main.getChildAt(i)
            if (child is ImageView && child.tag == "busIcon") {
                viewsToRemove.add(child)
            }
        }
        viewsToRemove.forEach { binding.main.removeView(it) }
    }






    //Функция добавления точек на линии маршрута и названия остановок
    private fun addStopPoints(stopCount: Int, stopNames: List<String>) {
        val busLine = binding.ivBusLine

        busLine.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                busLine.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val busLineHeight = busLine.height.toFloat() - 40F


                for (i in 0 until stopCount) {
                    val pointIcon = ImageView(this@InfoActivity)
                    pointIcon.setImageResource(R.drawable.point_line)
                    pointIcon.id = View.generateViewId()  // Генерируем уникальный ID для точки

                    val pointSize = resources.getDimensionPixelSize(R.dimen.point_size)

                    val progress = i.toFloat() / (stopCount - 1).toFloat()
                    val yPosition = busLineHeight * progress

                    val params = ConstraintLayout.LayoutParams(pointSize, pointSize)
                    params.topToTop = busLine.id
                    params.startToStart = busLine.id
                    params.endToEnd = busLine.id
                    params.topMargin = yPosition.toInt()

                    pointIcon.layoutParams = params

                    binding.main.addView(pointIcon)

                    // Добавление textViews с названиями остановок
                    val stopNameTv = TextView(this@InfoActivity)
                    stopNameTv.text = stopNames[i]
                    stopNameTv.textSize = 14f
                    stopNameTv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    stopNameTv.setTypeface(null, Typeface.BOLD)
                    stopNameTv.setTextColor(ContextCompat.getColor(this@InfoActivity, R.color.textColorPrimary))



                    // Ограничение ширины TextView
                    val maxWidth = resources.getDimensionPixelSize(R.dimen.max_stop_name_width) //Этот размер в dimens.xml
                    stopNameTv.maxWidth = maxWidth
                    // Если текст не помещается, добавляем многоточие в конце
                    stopNameTv.ellipsize = TextUtils.TruncateAt.END
                    stopNameTv.maxLines = 1


                    val tvParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    tvParams.topToTop = pointIcon.id  // Привязываем к ID точки
                    tvParams.bottomToBottom = pointIcon.id  // Привязываем к ID точки
                    tvParams.startToEnd = pointIcon.id  // Привязываем к ID точки
                    tvParams.marginStart = 40 // Отступ справа от точки




                    // Добавляем ограничение по правому краю
                    tvParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    tvParams.horizontalBias = 0f  // Прижимаем к левой стороне

                    val shape = GradientDrawable()
                    shape.shape = GradientDrawable.RECTANGLE
                    shape.cornerRadius = 25f
                    shape.setColor(Color.parseColor("#D9D9D9"))
                    stopNameTv.background = shape
                    stopNameTv.setPadding(20, 4, 20, 4)


                    stopNameTv.layoutParams = tvParams

                    binding.main.addView(stopNameTv)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        timeHandler.removeCallbacks(timeRunnable)
    }

}
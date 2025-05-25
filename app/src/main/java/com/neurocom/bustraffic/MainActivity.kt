package com.neurocom.bustraffic

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.neurocom.bustraffic.databinding.ActivityMainBinding
import java.util.Timer
import java.util.TimerTask
//импорт библиотек для inAppUpdate
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
//импорт для аналитики
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var isSelectionMode = false
    private var selectedRouteCodes = mutableListOf<String?>(null, null, null)
    private var currentSelectionIndex = -1
    private var originalRouteText: String? = "Выберите номер маршрута"
    private var currentIndex = 0
    private var timer: Timer? = null
    private var isAnimationFinished = false
    private var textRoute: String? = null
    private val handler = Handler(Looper.getMainLooper())
    //переменные для inAppUpdate
    private var appUpdate : AppUpdateManager? = null
    private val REQUEST_CODE = 100
    //----------------------------------
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //инициализация аналитики
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)


        //переменные и функция для inAppUpdate
        appUpdate = AppUpdateManagerFactory.create(this)
        checkAppUdate()
        //----------------------------------



        sharedPreferences = getSharedPreferences("BusTrafficPrefs", Context.MODE_PRIVATE)

        if (isFirstLaunch()) {
            showFirstLaunchInfo()
        }

        textRoute = "Выберите номер маршрута"

        startTextAnimation()

        with(binding) {
            val buttons = listOf(btnStar1, btnStar2, btnStar3)
            val textViews = listOf(tvIsSelected1, tvIsSelected2, tvIsSelected3)

            //загрузка сохраненных маршрутов
            for (i in buttons.indices) {
                val savedRouteText = sharedPreferences.getString("selectedRouteText$i", null)
                selectedRouteCodes[i] = sharedPreferences.getString("selectedRouteCode$i", null)

                if (savedRouteText != null) {
                    textViews[i].text = savedRouteText
                    buttons[i].visibility = View.GONE
                    textViews[i].visibility = View.VISIBLE
                } else {
                    buttons[i].visibility = View.VISIBLE
                    textViews[i].visibility = View.GONE
                }
            }

            //обработчики для кнопок избренное с анимацией
            buttons.forEachIndexed { index, button ->
                button.setOnClickListener {
                    it.isEnabled = false //отключение повторного нажатия
                    if (isFirstStarClick()) {
                        showInfoDialog {
                            startStarSelection(index)
                            it.isEnabled = true
                        }
                    } else {
                        animateButton(it) {
                            startStarSelection(index)
                            it.isEnabled = true
                        }
                    }
                }
            }

            //обработчики для выбранных маршрутов с анимацией и блокировкой повторного нажатия
            textViews.forEachIndexed { index, textView ->
                textView.setOnClickListener {
                    textView.isEnabled = false //отключение повторного нажатия
                    animateButton(textView) {
                        selectedRouteCodes[index]?.let { code ->
                            openDirectActivity(code)
                        }
                        textView.isEnabled = true //включаем обратно после завершения
                    }
                }
            }

            val routeViews = mapOf(
                tv1 to "route1а", tv2 to "route2", tv3 to "route3", tv4 to "route4",
                tv5 to "route4a", tv6 to "route5", tv7 to "route6a", tv8 to "route8",
                tv9 to "route10", tv10 to "route16", tv11 to "route101", tv12 to "route102",
                tv13 to "route103", tv14 to "route104", tv15 to "route105", tv16 to "route106",
                tv17 to "route107", tv18 to "route108", tv19 to "route109"
            )

            fun setupClickListeners() {
                routeViews.forEach { (view, routeCode) ->
                    view.setOnClickListener {
                        if (isSelectionMode && currentSelectionIndex != -1) {
                            val textView = textViews[currentSelectionIndex]
                            val button = buttons[currentSelectionIndex]

                            textView.visibility = View.VISIBLE
                            textView.text = (view as TextView).text
                            selectedRouteCodes[currentSelectionIndex] = routeCode

                            button.visibility = View.GONE
                            isSelectionMode = false

                            //восстанавливаем оригинальный текст
                            binding.tvNumChooseRoute.text = originalRouteText

                            //сбрасываем оригинальный текст после использования
                            originalRouteText = null

                            sharedPreferences.edit()
                                .putString("selectedRouteText$currentSelectionIndex", textView.text.toString())
                                .putString("selectedRouteCode$currentSelectionIndex", selectedRouteCodes[currentSelectionIndex])
                                .apply()

                            binding.overlayContainer.visibility = View.GONE
                            currentSelectionIndex = -1
                        } else if (!isSelectionMode) {
                            openDirectActivity(routeCode)
                        }
                    }
                }
            }

            setupClickListeners()
            //затенение при клике на btnStar
            binding.dimOverlay.setOnClickListener {
                isSelectionMode = false
                binding.overlayContainer.visibility = View.GONE
                binding.touchBlocker.visibility = View.GONE
                currentSelectionIndex = -1

                //восстанавливаем оригинальный текст, если выбор был отменён
                binding.tvNumChooseRoute.text = originalRouteText
                originalRouteText = null
            }

            textViews.forEachIndexed { index, textView ->
                textView.setOnLongClickListener {
                    val button = buttons[index]

                    textView.text = ""
                    selectedRouteCodes[index] = null

                    textView.visibility = View.GONE
                    button.visibility = View.VISIBLE

                    sharedPreferences.edit()
                        .remove("selectedRouteText$index")
                        .remove("selectedRouteCode$index")
                        .apply()

                    true
                }
            }
        }
    }
    //проверяет количество нажатий на кнопку "избранное"
    private fun isFirstStarClick(): Boolean {
        val isFirst = sharedPreferences.getBoolean("isFirstStarClick", true)
        if (isFirst) {
            sharedPreferences.edit().putBoolean("isFirstStarClick", false).apply()
        }
        return isFirst
    }
    //показываем диалог с инфой о пользовании кнопкой "избранное"
    private fun showInfoDialog(onDismiss: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        //устанавливаем фон для диалога
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_for_dialog))

        dialogView.findViewById<View>(R.id.btnUnderstand).setOnClickListener {
            dialog.dismiss()
            onDismiss()
        }

        //настройка ширины диалога
        dialog.show()
        val displayMetrics = resources.displayMetrics
        val dialogWidth = (300 * displayMetrics.density).toInt() // 300dp
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    //анимация текста при выборе в избранное
    private fun startStarSelection(index: Int) {
        //блокируем клики на весь экран
        binding.touchBlocker.visibility = View.VISIBLE

        //делаем экран снова кликабельным через 2000 миллисекунд
        handler.postDelayed({
            binding.touchBlocker.visibility = View.GONE
        }, 2000)

        isSelectionMode = true
        currentSelectionIndex = index

        //сохраняем оригинальный текст, если еще не сохранён
        if (originalRouteText == null) {
            originalRouteText = "Выберите номер маршрута"
        }

        textRoute = "Выберите маршрут для добавления в избранное"
        currentIndex = 0 //сбрасываем индекс для анимации

        //запускаем анимацию текста
        startTextAnimation()

        //показываем overlay
        binding.overlayContainer.visibility = View.VISIBLE
        binding.Scroll.bringToFront()
    }
    //анимация текста
    private fun startTextAnimation() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    textRoute?.let {
                        if (currentIndex < it.length) {
                            binding.tvNumChooseRoute.text = it.substring(0, currentIndex + 1)
                            currentIndex++
                        } else {
                            timer?.cancel()
                            isAnimationFinished = true
                        }
                    }
                }
            }
        }, 100, 40) //задержка и интервал в 100 мс
    }
    //анимация кнопки
    private fun animateButton(view: View, onAnimationEnd: () -> Unit) {
        view.animate().apply {
            duration = 150
            scaleX(0.8f)
            scaleY(0.8f)
            withEndAction {
                view.animate().apply {
                    duration = 150
                    scaleX(1f)
                    scaleY(1f)
                    withEndAction { onAnimationEnd() }
                }.start()
            }
        }.start()
    }

    private fun openDirectActivity(route: String) {
        val intent = Intent(this@MainActivity, DirectActivity::class.java).apply {
            putExtra("keyRoute", route)
        }
        startActivity(intent)
    }
    //считает количество запусков после установки
    private fun isFirstLaunch(): Boolean {
        val isFirst = sharedPreferences.getBoolean("isFirstLaunch", true)
        if (isFirst) {
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
        }
        return isFirst
    }
    //показывает диалог с дисклеймером
    private fun showFirstLaunchInfo() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_first_launch_info, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_for_dialog))

        dialogView.findViewById<View>(R.id.btnCloseInfo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        val displayMetrics = resources.displayMetrics
        val dialogWidth = (300 * displayMetrics.density).toInt() // 300dp
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    //---------------------------------функции для inAppUpdate
    fun checkAppUdate(){
        appUpdate?.appUpdateInfo?.addOnSuccessListener { updateInfo->
            if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)){
                appUpdate?.startUpdateFlowForResult(updateInfo, AppUpdateType.IMMEDIATE, this, REQUEST_CODE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inProgressUpdate()
    }

    fun inProgressUpdate(){
        appUpdate?.appUpdateInfo?.addOnSuccessListener { updateInfo->
            if (updateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS){
                appUpdate?.startUpdateFlowForResult(updateInfo, AppUpdateType.IMMEDIATE, this, REQUEST_CODE)
            }
        }
    }
    //----------------------------------

}

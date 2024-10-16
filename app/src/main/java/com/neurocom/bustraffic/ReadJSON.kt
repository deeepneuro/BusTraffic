package com.neurocom.bustraffic

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

//дата класс с маршрутом и его направлениями, конечными точками и остановками
data class Route(
    val there: List<String>,
    val back: List<String>,
    val thereDayOff: List<String>,
    val backDayOff: List<String>,
    val point1: List<String>,
    val point2: List<String>,
//    val thereStop: List<String>, // Добавлено поле для thereStop
//    val backStop: List<String>,  // Добавлено поле для backStop
    val stopTherePoint: List<String>,
    val stopBackPoint: List<String>,
    val durationTimeThere: Int,
    val durationTimeBack: Int,
)

//дата класс, который содержит в массиве маршруты
data class RouteSchedule(val routes: Map<String, Route>)

//функция чтения JSON-файла и преобразования его в объект RouteSchedule
fun loadRouteSchedule(context: Context): RouteSchedule {
    val jsonString = context.assets.open("route_schedule.json").bufferedReader().use { it.readText() }
    val gson = Gson()
    val routeScheduleType = object : TypeToken<RouteSchedule>() {}.type
    return gson.fromJson(jsonString, routeScheduleType)
}

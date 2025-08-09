package projectflower.forecaster

import java.time.LocalDate

class WeatherData (
    var areaCode: String,
    var childCode: String,
    var areaName: String,
    var date: LocalDate,
    var weatherCode: String,
    var pop: Int,
    var reliability: String,
    var image: Int
)
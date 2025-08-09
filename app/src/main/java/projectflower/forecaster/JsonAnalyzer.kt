package projectflower.forecaster

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JsonAnalyzer {
    companion object {
        /** エリアコード - エリア名の Map を返します。 */
        fun getAreas(
            responseBody: String,
        ): Map<String, String> {
            val jsonRoot = JSONObject(responseBody)
            val offices = jsonRoot.getJSONObject("offices")
            val result = mutableMapOf<String, String>()

            for (key in offices.keys()) {
                result[key] = offices.getJSONObject(key).getString("name")
            }

            return result.toMap()
        }

        /** 週間天気予報が取得される子エリアを エリアコード - エリア名 の Map として返します。 */
        fun getChildAreas(responseBody: String): Map<String, String> {
            val jsonRoot = JSONArray(responseBody)
            val result = mutableMapOf<String, String>()

            // 翌日以降の天気
            val weeklyData =
                jsonRoot.getJSONObject(1).getJSONArray("timeSeries").getJSONObject(0)
            val areas = weeklyData.getJSONArray("areas")

            for (i in 0 until areas.length()) {
                val area = areas.getJSONObject(i)
                val areaInfo = area.getJSONObject("area")
                val childCode = areaInfo.getString("code")
                val childName = areaInfo.getString("name")
                result[childCode] = childName
            }

            return result.toMap()
        }

        fun getWeathers(
            responseBody: String,
            areaCode: String,
            children: Set<String>
        ): Map<String, Array<WeatherData>> {
            val jsonRoot = JSONArray(responseBody)
            val nearlyWeather = jsonRoot.getJSONObject(0)
            val timeSeries = nearlyWeather.getJSONArray("timeSeries")
            val timeSeries1 = timeSeries.getJSONObject(0)
            val todayAreas = timeSeries1.getJSONArray("areas")
            val tempResult = mutableMapOf<String, ArrayList<WeatherData>>()
            val todayWeathers = mutableMapOf<String, String>()
            val firstDate = LocalDate.parse(
                timeSeries1.getJSONArray("timeDefines").getString(0),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )

            // 当日の天気
            for (i in 0 until todayAreas.length()) {
                val area = todayAreas.getJSONObject(i)
                val childCode = area.getJSONObject("area").getString("code")
                val weatherCode = area.getJSONArray("weatherCodes").getString(0)
                todayWeathers[childCode] = weatherCode
            }

            // 週間天気の今日の天気の取得の仕方がわからないので、
            // 最も多く出現する天気を選択する
            val representativeWeather =
                todayWeathers.values.groupBy { it }.maxByOrNull { it.value.size }?.value[0] ?: ""

            for (childCode in children) {
                val weatherCode = when {
                    todayWeathers.containsKey(childCode) -> todayWeathers[childCode]
                    else -> representativeWeather
                }

                val weather = WeatherData(
                    areaCode,
                    childCode,
                    "",
                    firstDate,
                    weatherCode!!,
                    -1,
                    "",
                    WeatherCodes.getImageResource(weatherCode)
                )

                var element = tempResult[childCode]

                if (element == null) {
                    element = ArrayList()
                }

                element.add(weather)
                tempResult[childCode] = element
            }

            // 翌日以降の天気
            val weeklyData =
                jsonRoot.getJSONObject(1).getJSONArray("timeSeries").getJSONObject(0)
            val timeDefines = weeklyData.getJSONArray("timeDefines")
            val areas = weeklyData.getJSONArray("areas")

            for (i in 0 until areas.length()) {
                val area = areas.getJSONObject(i)
                val areaInfo = area.getJSONObject("area")
                val childCode = areaInfo.getString("code")

                if (!children.contains(childCode)) continue

                val weathers = area.getJSONArray("weatherCodes")

                for (j in 0 until weathers.length()) {
                    val weatherCode = weathers.getString(j)

                    val weather = WeatherData(
                        areaCode,
                        childCode,
                        areaInfo.getString("name"),
                        LocalDate.parse(
                            timeDefines.getString(j),
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        ),
                        weatherCode,
                        area.getJSONArray("pops").getString(j).toIntOrNull() ?: -1,
                        area.getJSONArray("reliabilities").getString(i),
                        WeatherCodes.getImageResource(weatherCode)
                    )

                    var element = tempResult[childCode]

                    if (element == null) {
                        element = ArrayList<WeatherData>()
                    }

                    element.add(weather)
                    tempResult[childCode] = element
                }
            }

            val result = mutableMapOf<String, Array<WeatherData>>()

            for (t in tempResult) {
                result[t.key] = t.value.toTypedArray()
            }

            return result.toMap()
        }
    }
}
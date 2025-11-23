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
            // jsonRoot[0]
            val element1 = jsonRoot.getJSONObject(0)
            // jsonRoot[0].timeSeries[0]
            val upcomingTimeSeries1 = element1.getJSONArray("timeSeries").getJSONObject(0)
            // jsonRoot[0].timeSeries[0].areas
            val upcomingAreas = upcomingTimeSeries1.getJSONArray("areas")
            // jsonRoot[0].timeSeries[0].timeDefines
            val upcomingDates = upcomingTimeSeries1.getJSONArray("timeDefines")
            val tempResult = mutableMapOf<String, ArrayList<WeatherData>>()
            val todayWeathers = mutableMapOf<String, String>()

            // 直近の天気
            for (i in 0 until upcomingDates.length()) {
                for (j in 0 until upcomingAreas.length()) {
                    // jsonRoot[0].timeSeries[0].areas[j]
                    val childArea = upcomingAreas.getJSONObject(j)
                    // jsonRoot[0].timeSeries[0].areas[j].area.code
                    val childCode = childArea.getJSONObject("area").getString("code")
                    // jsonRoot[0].timeSeries[0].areas[j].weatherCodes[i]
                    val weatherCode = childArea.getJSONArray("weatherCodes").getString(i)
                    todayWeathers[childCode] = weatherCode
                }

                // 週間天気の今日の天気の取得の仕方がわからないので、
                // 最も多く出現する天気を選択する
                val representativeWeather =
                    todayWeathers.values.groupBy { it }.maxByOrNull { it.value.size }?.value[0]
                        ?: ""

                for (childCode in children) {
                    val weatherCode = when {
                        todayWeathers.containsKey(childCode) -> todayWeathers[childCode]
                        else -> representativeWeather
                    }

                    val weather = WeatherData(
                        areaCode,
                        childCode,
                        "",
                        LocalDate.parse(
                            // jsonRoot[0].timeSeries[0].timeDefines[i]
                            upcomingDates.getString(i),
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        ),
                        weatherCode!!,
                        -1,
                        "",
                        WeatherCodes.getImageResource(weatherCode)
                    )

                    var element = tempResult[childCode]

                    if (element == null) {
                        element = ArrayList()
                    } else if (element.any { it.date == weather.date }) {
                        // 基本的に同じ日付が 2 つ以上現れることはないはずだが、
                        // もし存在すれば最初のデータを採用する
                        continue
                    }

                    element.add(weather)
                    tempResult[childCode] = element
                }
            }

            // 翌日以降の天気
            // jsonRoot[1].timeSeries[0]
            val weeklyData =
                jsonRoot.getJSONObject(1).getJSONArray("timeSeries").getJSONObject(0)
            // jsonRoot[1].timeSeries[0].timeDefines
            val timeDefines = weeklyData.getJSONArray("timeDefines")
            // jsonRoot[1].timeSeries[0].areas
            val childAreas = weeklyData.getJSONArray("areas")

            for (i in 0 until childAreas.length()) {
                // jsonRoot[1].timeSeries[0].areas[i]
                val childArea = childAreas.getJSONObject(i)
                // jsonRoot[1].timeSeries[0].areas[i].area
                val areaInfo = childArea.getJSONObject("area")
                // jsonRoot[1].timeSeries[0].areas[i].area.code
                val childCode = areaInfo.getString("code")

                if (!children.contains(childCode)) continue

                // jsonRoot[1].timeSeries[0].areas[i].weatherCodes
                val weathers = childArea.getJSONArray("weatherCodes")

                for (j in 0 until weathers.length()) {
                    val weatherCode = weathers.getString(j)

                    val weather = WeatherData(
                        areaCode,
                        childCode,
                        // jsonRoot[1].timeSeries[0].areas[i].area.name
                        areaInfo.getString("name"),
                        LocalDate.parse(
                            // jsonRoot[1].timeSeries[0].timeDefines[j]
                            timeDefines.getString(j),
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        ),
                        weatherCode,
                        // jsonRoot[1].timeSeries[0].areas[i].pops[j]
                        childArea.getJSONArray("pops").getString(j).toIntOrNull() ?: -1,
                        // jsonRoot[1].timeSeries[0].areas[i].reliabilities[i]
                        childArea.getJSONArray("reliabilities").getString(i),
                        WeatherCodes.getImageResource(weatherCode)
                    )

                    var element = tempResult[childCode]

                    if (element == null) {
                        element = ArrayList()
                    } else if (element.any { it.date == weather.date }) {
                        // 直近の天気に含まれている場合は、直近の天気を採用する
                        continue
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
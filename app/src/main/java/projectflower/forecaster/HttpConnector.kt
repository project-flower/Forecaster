package projectflower.forecaster

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HttpConnector {
    companion object {
        // Public Methods

        /** 定義済の観測エリアをコードと表示名の組み合わせで返します。 */
        @WorkerThread
        suspend fun getAreas(): Map<String, String> {
            val result = withContext(Dispatchers.IO) {
                var workerResult: Map<String, String>
                val responseBody = getHttp(Constants.areaEndpoint)
                workerResult = JsonAnalyzer.getAreas(responseBody)
                workerResult
            }

            return result
        }

        /** 予報が取得可能な子エリアをコードと表示名の組み合わせで返します。 */
        @WorkerThread
        suspend fun getChildAreas(area: String): Map<String, String> {
            val result = withContext(Dispatchers.IO) {
                var workerResult: Map<String, String>
                val responseBody = getWeeklyWeather(area)
                workerResult = JsonAnalyzer.getChildAreas(responseBody)
                workerResult
            }

            return result
        }

        /** [area] に指定したエリアの子エリアから [childAreas] に含まれる週間天気予報を取得します。 */
        @WorkerThread
        suspend fun getWeathers(
            area: String,
            childAreas: Set<String>
        ): Map<String, Array<WeatherData>> {
            val result = withContext(Dispatchers.IO) {
                var workerResult: Map<String, Array<WeatherData>>
                val responseBody = getWeeklyWeather(area)
                workerResult = JsonAnalyzer.getWeathers(responseBody, area, childAreas)
                workerResult
            }

            return result
        }

        // Private Methods

        /** HTTP GET レスポンスを取得します。 */
        private fun getHttp(url: String): String {
            var result: String
            val connection = URL(url).openConnection() as HttpsURLConnection

            connection.run {
                requestMethod = "GET"
                connect()
                result = inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readLines().joinToString()
                }
                disconnect()
                inputStream.close()
            }

            return result
        }

        /** [areaCode] に指定したエリアの週間天気予報を取得します。 */
        private fun getWeeklyWeather(areaCode: String): String {
            return getHttp(Constants.weatherEndpoint.replace("\${area}", areaCode))
        }
    }
}
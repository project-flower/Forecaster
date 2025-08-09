package projectflower.forecaster

class WeatherCodes {
    companion object {
        /** [weatherCode] に応じたアイコン リソース IDを返します。 */
        fun getImageResource(weatherCode: String): Int {
            return getImageResourceFromIconCode(getIconCodeFromWeatherCode(weatherCode))
        }

        private fun getImageResourceFromIconCode(iconCode: String): Int {
            return when (iconCode) {
                "406" -> R.drawable.blowing_snow
                "200" -> R.drawable.cloudy
                "102", "112", "311", "313" -> R.drawable.drizzle
                "401", "402", "411", "413" -> R.drawable.flurries
                "308" -> R.drawable.heavy
                "110", "201", "210" -> R.drawable.mostly_cloudy
                "101" -> R.drawable.mostly_sunny
                "202", "212", "301", "302" -> R.drawable.scattered_showers
                "104", "115", "204", "215" -> R.drawable.scattered_snow
                "300" -> R.drawable.showers
                "400" -> R.drawable.snow_showers
                "100" -> R.drawable.sunny
                "303", "314", "403", "414" -> R.drawable.wintry_mix
                else -> R.drawable.blank
            }
        }

        /**
         * 気象庁の気象コードの通りに対応する表示用気象コードに変換します。
         * 気象庁サイトでは Forecast.Const.TELOPS にて定義されている
         */
        private fun getIconCodeFromWeatherCode(code: String): String {
            return when (code) {
                "100", "123", "124", "130", "131" -> "100"
                "101", "132" -> "101"
                "102", "103", "106", "107", "108", "120", "121", "140" -> "102"
                "104", "105", "160", "170" -> "104"
                "110", "111" -> "110"
                "112", "113", "114", "119", "122", "125", "126", "127", "128" -> "112"
                "115", "116", "117", "181" -> "115"
                "200", "209", "231", "" -> "200"
                "201", "223" -> "201"
                "202", "203", "206", "207", "208", "220", "221", "240" -> "202"
                "204", "205", "250", "260", "270" -> "204"
                "210", "211" -> "210"
                "212", "213", "214", "218", "219", "222", "224", "225", "226" -> "212"
                "215", "216", "217", "228", "229", "230", "281" -> "215"
                "300", "304", "306", "328", "329", "350" -> "300"
                "301" -> "301"
                "302" -> "302"
                "303", "309", "322" -> "303"
                "308" -> "308"
                "311", "316", "320", "323", "324", "325" -> "311"
                "313", "317", "321" -> "313"
                "314", "315", "326", "327" -> "314"
                "340", "400", "405", "425", "426", "427", "450" -> "400"
                "401" -> "401"
                "402" -> "402"
                "403", "409" -> "403"
                "406", "407" -> "406"
                "361", "411", "420" -> "411"
                "371", "413", "421" -> "413"
                "414", "422", "423" -> "414"
                else -> ""
            }
        }
    }
}
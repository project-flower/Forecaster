package projectflower.forecaster

import androidx.datastore.preferences.core.stringPreferencesKey

class Constants {
    companion object {
        val areaEndpoint: String = "https://www.jma.go.jp/bosai/common/const/area.json";
        val settingsKey = stringPreferencesKey("SETTINGS")
        val weatherEndpoint: String =
            "https://www.jma.go.jp/bosai/forecast/data/forecast/\${area}.json";
    }
}
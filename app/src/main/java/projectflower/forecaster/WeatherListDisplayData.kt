package projectflower.forecaster

class WeatherListDisplayData (
    override var displayName: String,
    override var areaName: String,
    override var areaCode: String,
    override var childCode: String,
    override var childName: String,
    var weathers: MutableList<WeatherData>? = null,
    var inProgress: Boolean = false,
    var isEditing: Boolean = false
): WeatherListData(displayName, areaName, areaCode, childCode, childName)
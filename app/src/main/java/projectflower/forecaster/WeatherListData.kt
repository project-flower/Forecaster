package projectflower.forecaster

import kotlinx.serialization.Serializable

/**
 * データの保存用にシリアライズ可能なメンバー
 */
@Serializable
open class WeatherListData (
    open var displayName: String,
    open var areaName: String,
    open var areaCode: String,
    open var childCode: String,
    open var childName: String,
)
package projectflower.forecaster

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import projectflower.forecaster.databinding.ActivityMainBinding
import projectflower.forecaster.databinding.DateHeaderBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity(), AreaSelectDialogFragment.NoticeDialogListener {

    // Private Fields

    private lateinit var addRowButton: FloatingActionButton
    private lateinit var binding: ActivityMainBinding
    private lateinit var dates: Array<LocalDate>
    private lateinit var editButton: FloatingActionButton
    private lateinit var endEditButton: FloatingActionButton
    private lateinit var headers: Array<DateHeaderBinding>
    private lateinit var mainView: View
    private lateinit var updateButton: FloatingActionButton
    private var updateError: String? = null
    private lateinit var weatherListAdapter: WeatherListAdapter
    private var weathersUpdateNeeded: Boolean = true

    // Over-ride Methods

    /** エリア選択ダイアログでエラーが発生した場合のリスナー */
    override fun onAreaSelectDialogErrorOccurred(
        dialog: DialogFragment,
        error: String
    ) {
        showErrorMessage(error)
    }

    /** エリア選択ダイアログの OK タップのイベント リスナー */
    override fun onAreaSelectDialogPositiveClick(
        dialog: DialogFragment,
        itemPosition: Int,
        area: Pair<String, String>,
        childArea: Pair<String, String>
    ) {
        updateWeatherListItem(itemPosition, area, childArea)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        mainView = binding.root
        setContentView(mainView)

        headers = arrayOf(
            binding.header1,
            binding.header2,
            binding.header3,
            binding.header4,
            binding.header5,
            binding.header6,
            binding.header7,
            binding.header8
        )

        val listView = binding.listview
        listView.layoutManager = LinearLayoutManager(this)

        weatherListAdapter = WeatherListAdapter(
            this@MainActivity,
            { position, area, childArea ->
                onWeatherListViewEditClicked(
                    position,
                    area,
                    childArea
                )
            },
            { onWeathersUpdateCompleted() }
        )

        listView.adapter = weatherListAdapter
        // listview.isClickable = true

        editButton = binding.fabEdit
        updateButton = binding.fabSync
        endEditButton = binding.fabEndEdit
        addRowButton = binding.fabAddRow

        editButton.setOnClickListener { setEditMode(true) }
        updateButton.setOnClickListener { updateWeathers() }
        endEditButton.setOnClickListener { onEndEditButtonClicked() }
        addRowButton.setOnClickListener { onAddRowButtonClicked() }

        initializePreference()
    }

    // Private Methods

    /** 編集ボタン、更新ボタンを表示又は非表示にします。 */
    private fun enableButtons(enabled: Boolean) {
        editButton.isEnabled = enabled
        updateButton.isEnabled = enabled
    }

    /** DataStore の変更フローを設定します。 */
    private suspend fun initializeDataStoreFlow(flow: Flow<String>) {
        flow.collect { onFlowCollected(it) }
    }

    /** Preference のための設定を初期化します。 */
    private fun initializePreference() {
        val flow: Flow<String> =
            dataStore.data.catch { exception -> exception.message?.let { showErrorMessage(it) } }
                .map { preferences -> preferences[Constants.settingsKey] ?: "" }

        lifecycleScope.launch {
            initializeDataStoreFlow(flow)
        }
    }

    /** 行追加ボタンのタップ イベント リスナー */
    private fun onAddRowButtonClicked() {
        weatherListAdapter.addItem(
            WeatherListDisplayData(
                "",
                "",
                "",
                "",
                "",
                mutableListOf(),
                false,
                true
            )
        )
    }

    /** 編集終了ボタンのタップ イベント リスナー */
    private fun onEndEditButtonClicked() {
        saveSettings()
        setEditMode(false)
    }

    /** DataStore の更新時に呼び出され、表示中の ListView に反映します。 */
    private fun onFlowCollected(value: String) {
        if (value == "") {
            weathersUpdateNeeded = false
            return
        }

        var decoded: Array<WeatherListData>? = null

        try {
            decoded = Json.decodeFromString<Array<WeatherListData>>(value)
        } catch (exception: Exception) {
            exception.message?.let { message -> showErrorMessage(message) }
        }

        // ここは DataStore.edit からも呼ばれるため、初めにクリアする
        weatherListAdapter.clearItems()

        decoded?.let {
            weatherListAdapter.addItems(decoded.map { d ->
                WeatherListDisplayData(
                    d.displayName,
                    d.areaName,
                    d.areaCode,
                    d.childCode,
                    d.childName,
                    null,
                    false,
                    false
                )
            })
        }

        if (weathersUpdateNeeded) {
            updateWeathers()
        }

        weathersUpdateNeeded = false
    }

    /** HTTP GET により気象予報情報を取得した時の処理をします。 */
    @UiThread
    private fun onWeatherDataReceived(areaCode: String, result: Map<String, Array<WeatherData>>) {
        weatherListAdapter.updateItems(areaCode, result, dates)
    }

    @UiThread
    private fun onWeatherDataReceiveFailed(areaCode: String) {
        weatherListAdapter.updateFailedItems(areaCode)
    }

    /** ListView 要素の編集ボタンのタップ イベント リスナー */
    private fun onWeatherListViewEditClicked(
        position: Int,
        area: Pair<String, String>?,
        childArea: Pair<String, String>?
    ) {
        AreaSelectDialogFragment().show(
            area,
            childArea,
            position,
            supportFragmentManager,
            "DIALOG_SELECT_AREA"
        )
    }

    /**
     * 気象予報の更新完了時の処理をします。
     * @param error 更新時に発生したエラー
     */
    @UiThread
    private fun onWeathersUpdateCompleted() {
        updateError?.let {
            showErrorMessage(it)
        }

        updateError = null
        enableButtons(true)
    }

    /** [savedData] を JSON 形式で Preference に書き込みます。 */
    private suspend fun saveJsonSettings(savedData: Array<WeatherListData>) {
        dataStore.edit { settings ->
            settings[Constants.settingsKey] = Json.encodeToString(savedData)
        }
    }

    /** 現在の ListView の設定を保存します。 */
    private fun saveSettings() {
        lifecycleScope.launch {
            saveJsonSettings(weatherListAdapter.getSettings())
        }
    }

    /** 編集モードを有効化又は解除します。 */
    private fun setEditMode(enabled: Boolean) {
        weatherListAdapter.isEditing = enabled
        editButton.isVisible = !enabled
        updateButton.isVisible = !enabled
        endEditButton.isVisible = enabled
        addRowButton.isVisible = enabled
    }

    /** エラー メッセージを Snackbar に表示します。 */
    @UiThread
    private fun showErrorMessage(error: String) {
        Snackbar.make(mainView, error, Snackbar.LENGTH_LONG)
            .setAction("Action", null)
            .show()
    }

    /** 日付を現在日時を基準に更新します。 */
    private fun synchronizeWeek() {
        val today = LocalDate.now()
        val dates = ArrayList<LocalDate>()

        for (i in 0 until headers.size) {
            val date = today.plusDays(i.toLong())
            val header = headers[i]
            header.day.text = date.format(DateTimeFormatter.ofPattern("M/d", Locale.JAPANESE))
            header.weekday.text = date.format(DateTimeFormatter.ofPattern("(E)", Locale.JAPANESE))
            dates.add(date)
        }

        this.dates = dates.toTypedArray()
    }

    /** ListView に紐づく [projectflower.forecaster.WeatherListDisplayData] を更新します。 */
    private fun updateWeatherListItem(
        itemPosition: Int,
        area: Pair<String, String>,
        childArea: Pair<String, String>
    ) {
        weatherListAdapter.updateItem(itemPosition, area, childArea)
    }

    /** 気象予報を取得し、表示内容を更新します。 */
    @UiThread
    private fun updateWeathers() {
        enableButtons(false)
        synchronizeWeek()
        updateError = null
        weatherListAdapter.beginUpdating()
        val areaCodes = weatherListAdapter.getAreaCodes()

        lifecycleScope.launch {
            for (areaCode in areaCodes) {
                try {
                    val result = HttpConnector.getWeathers(areaCode.key, areaCode.value.toSet())
                    withContext(Dispatchers.Main) {
                        onWeatherDataReceived(
                            areaCode.key,
                            result
                        )
                    }
                } catch (exception: Exception) {
                    if (updateError.isNullOrEmpty()) {
                        updateError = exception.message
                    }

                    exception.message?.let {
                        Log.e("MainActivity", it)
                    }

                    withContext(Dispatchers.Main) {
                        onWeatherDataReceiveFailed(areaCode.key)
                    }
                }
            }
        }
    }
}
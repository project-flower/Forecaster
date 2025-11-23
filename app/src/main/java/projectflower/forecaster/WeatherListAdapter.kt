package projectflower.forecaster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import java.time.LocalDate

/**
 * @param onEditClicked リスト要素の編集ボタンがタップされた時のリスナー
 */
class WeatherListAdapter(
    context: Context,
    private val onEditClicked: (position: Int, area: Pair<String, String>?, childArea: Pair<String, String>?) -> Unit,
    private val onUpdateCompleted: () -> Unit
) : RecyclerView.Adapter<WeatherListAdapter.ViewHolder>() {

    // Public Classes

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Public Fields

        /** 地域名 */
        val areaName: TextView = itemView.findViewById(R.id.listName)

        /** 削除ボタン */
        val deleteButton: ShapeableImageView = itemView.findViewById(R.id.delete_area)

        /** 編集ボタン */
        val editButton: ShapeableImageView = itemView.findViewById(R.id.edit_area)

        /** 気象アイコン */
        val images: Array<ImageView> = arrayOf(
            itemView.findViewById(R.id.weather1),
            itemView.findViewById(R.id.weather2),
            itemView.findViewById(R.id.weather3),
            itemView.findViewById(R.id.weather4),
            itemView.findViewById(R.id.weather5),
            itemView.findViewById(R.id.weather6),
            itemView.findViewById(R.id.weather7),
            itemView.findViewById(R.id.weather8)
        )

        /** プログレス スピナー */
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressbar)
    }

    // Public Fields

    val dataList: ArrayList<WeatherListDisplayData> = arrayListOf()

    // Private Fields

    private val inflater = LayoutInflater.from(context)
    private var mInProgress = false

    // Properties

    /** 処理中にします。 */
    val inProgress: Boolean
        get() = mInProgress

    /** 編集モードにします。 */
    var isEditing: Boolean = false
        get() = field
        set(value) {
            for (data in dataList) {
                data.isEditing = value
            }

            field = value
            notifyItemRangeChanged(0, dataList.size)
        }

    // Over-ride Methods

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val current = dataList[position]
        val areaName = holder.areaName
        areaName.text = current.displayName
        val images = holder.images

        for (image in images) {
            if (image.isVisible == (inProgress || isEditing)) {
                image.isVisible = !(inProgress || isEditing)

                if (inProgress) {
                    image.setImageResource(R.drawable.blank)
                }
            }
        }

        val editButton: ShapeableImageView = holder.editButton
        editButton.isVisible = isEditing

        editButton.setOnClickListener { view ->
            val data = dataList[position]

            val area = when (data.areaCode != "") {
                true -> Pair(data.areaCode, data.areaName)
                else -> null
            }

            val childArea = when (area != null && data.childCode != "") {
                true -> Pair(data.childCode, data.childName)
                else -> null
            }

            onEditClicked(
                position, area, childArea
            )
        }

        val deleteButton: ShapeableImageView = holder.deleteButton
        deleteButton.isVisible = isEditing

        deleteButton.setOnClickListener { view ->
            dataList.removeAt(position)
            notifyItemRemoved(position)
        }

        val progressBar: ProgressBar = holder.progressBar
        progressBar.isVisible = current.inProgress
        val weathers = current.weathers

        for (i in 0 until images.size) {
            var imageId = R.drawable.blank

            weathers?.let {
                if (it.size > i) {
                    imageId = it[i].image
                }
            }

            images[i].setImageResource(imageId)
            images[i].isVisible = !(current.inProgress || isEditing)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.listview_item, parent, false))
    }

    // Public Methods

    fun addItem(item: WeatherListDisplayData) {
        dataList.add(item)
        notifyItemInserted(dataList.size - 1)
    }

    fun addItems(items: List<WeatherListDisplayData>) {
        if (items.isEmpty()) return

        val position = dataList.size
        dataList.addAll(items)
        notifyItemRangeInserted(position, items.size)
    }

    fun beginUpdating() {
        for (data in dataList) {
            if (!data.areaCode.isEmpty()) {
                data.inProgress = true
            }
        }

        mInProgress = true
        notifyItemRangeChanged(0, dataList.size)
    }

    fun cancelUpdating() {
        for (data in dataList) {
            data.inProgress = false
        }

        mInProgress = false
        notifyItemRangeChanged(0, dataList.size)
    }

    fun clearItems() {
        val itemCount = dataList.size
        dataList.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    fun getAreaCodes(): Map<String, MutableSet<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()

        for (listData in dataList) {
            val areaCode = listData.areaCode

            if (areaCode == "") continue // 追加しただけの行は空値になっているのでスキップ

            var mapElement = result[areaCode]

            if (mapElement == null) {
                mapElement = mutableSetOf()
            }

            val childCode = listData.childCode

            if (!mapElement.contains(childCode)) {
                mapElement.add(childCode)
            }

            result[areaCode] = mapElement
        }

        return result
    }

    fun getSettings(): Array<WeatherListData> {
        if (dataList.isEmpty()) return arrayOf()

        val result = mutableListOf<WeatherListData>()

        for (data in dataList) {
            result.add(data)
        }

        return result.toTypedArray()
    }

    fun updateFailedItems(areaCode: String) {
        if (dataList.isEmpty()) return

        for (i in 0 until dataList.size) {
            val data = dataList[i]

            if (data.areaCode == areaCode) {
                data.inProgress = false
                notifyItemChanged(i)
                dataList[i] = data
            }
        }

        notifyUpdateCompleted()
    }

    fun updateItem(itemPosition: Int, area: Pair<String, String>, childArea: Pair<String, String>) {
        val item = dataList[itemPosition]
        item.areaCode = area.first
        item.areaName = area.second
        item.childCode = childArea.first
        item.childName = childArea.second

        item.displayName = when (area.first == childArea.first) {
            true -> area.second
            else -> "${area.second} ${childArea.second}"
        }

        dataList[itemPosition] = item
        notifyItemChanged(itemPosition)
    }

    fun updateItems(
        areaCode: String,
        result: Map<String, Array<WeatherData>>,
        dates: Array<LocalDate>
    ) {
        if (dataList.isEmpty()) return

        for (childArea in result) {
            for (position in 0 until dataList.size) {
                val data = dataList[position]
                val childCode = data.childCode

                if (areaCode != data.areaCode || childCode != childArea.key) continue

                val weathers = ArrayList<WeatherData>()

                for (date in dates) {
                    val weather = childArea.value.find { c -> c.date == date }

                    if (weather == null) {
                        weathers.add(
                            WeatherData(areaCode, childCode, "", date, "", 0, "", R.drawable.blank)
                        )
                        continue
                    }

                    weathers.add(weather)
                }

                data.weathers = weathers
                data.inProgress = false
                dataList[position] = data
                notifyItemChanged(position)
            }
        }

        notifyUpdateCompleted()
    }

    // Private Methods

    @UiThread
    private fun notifyUpdateCompleted() {
        if (dataList.any { it.inProgress }) return

        mInProgress = false
        onUpdateCompleted()
    }
}
package projectflower.forecaster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.imageview.ShapeableImageView

/**
 * @param onEditClicked リスト要素の編集ボタンがタップされた時のリスナー
 */
class WeatherListAdapter(
    context: Context,
    dataList: ArrayList<WeatherListDisplayData?>?,
    private val onEditClicked: (position: Int, area: Pair<String, String>?, childArea: Pair<String, String>?) -> Unit
) : ArrayAdapter<WeatherListDisplayData?>(context, R.layout.listview_item, dataList!!) {
    // Over-ride Methods

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val listData = getItem(position)!!

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.listview_item, parent, false)
        }

        val areaName = view.findViewById<TextView>(R.id.listName)
        areaName.text = listData.displayName

        val images: Array<ImageView> = arrayOf(
            view.findViewById(R.id.weather1),
            view.findViewById(R.id.weather2),
            view.findViewById(R.id.weather3),
            view.findViewById(R.id.weather4),
            view.findViewById(R.id.weather5),
            view.findViewById(R.id.weather6),
            view.findViewById(R.id.weather7),
            view.findViewById(R.id.weather8)
        )

        val inProgress = listData.inProgress
        val isEditing = listData.isEditing

        for (image in images) {
            if (image.isVisible == (inProgress || isEditing)) {
                image.isVisible = !(inProgress || isEditing)

                if (inProgress) {
                    image.setImageResource(R.drawable.blank)
                }
            }
        }

        val editButton: ShapeableImageView = view.findViewById(R.id.edit_area)
        editButton.isVisible = isEditing
        // Click リスナを設定した後から並べ替えられると position は変わるのでボタンに位置を記憶させる
        editButton.tag = position

        if (!editButton.hasOnClickListeners()) {
            editButton.setOnClickListener { view ->
                val currentPosition = view.tag as Int
                val item = getItem(currentPosition)!!

                val area = when (item.areaCode != "") {
                    true -> Pair(item.areaCode, item.areaName)
                    else -> null
                }

                val childArea = when (area != null && item.childCode != "") {
                    true -> Pair(item.childCode, item.childName)
                    else -> null
                }

                onEditClicked(
                    currentPosition,
                    area,
                    childArea
                )
            }
        }

        val deleteButton: ShapeableImageView = view.findViewById(R.id.delete_area)
        deleteButton.isVisible = isEditing
        deleteButton.tag = position

        if (!deleteButton.hasOnClickListeners()) {
            deleteButton.setOnClickListener { view ->
                val currentPosition = view.tag as Int
                remove(getItem(currentPosition))
                notifyDataSetChanged()
            }
        }

        val progressBar: ProgressBar = view.findViewById(R.id.progressbar)
        progressBar.isVisible = inProgress

        if (inProgress) {
            return view
        }

        val weathers = listData.weathers

        for (i in 0 until images.size) {
            var imageId = R.drawable.blank

            weathers?.let {
                if (it.size > i) {
                    imageId = it[i].image
                }
            }

            images[i].setImageResource(imageId)
        }

        return view
    }
}
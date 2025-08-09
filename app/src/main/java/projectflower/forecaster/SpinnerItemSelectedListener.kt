package projectflower.forecaster

import android.view.View
import android.widget.AdapterView

/** Spinner が選択された時にイベントを取得するリスナー */
class SpinnerItemSelectedListener(
    private val onItemSelectedHandler: (Pair<String, String>?) -> Unit,
    private val onNothingSelectedHandler: () -> Unit,
    val adapter: AreaListAdapter
) : AdapterView.OnItemSelectedListener {
    /** スピナーから任意の値が選択されると呼び出されます。 */
    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        val item = adapter.getItem(position)
        onItemSelectedHandler(item)
    }

    /** スピナーが何も選択しなかった場合に呼び出されます。 */
    override fun onNothingSelected(p0: AdapterView<*>?) {
        onNothingSelectedHandler()
    }

}
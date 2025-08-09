package projectflower.forecaster

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AreaSelectDialogFragment : DialogFragment() {
    // Classes

    /** ダイアログから親アクティビティへイベントをコールします。 */
    interface NoticeDialogListener {
        /** OK ボタンがタップされた時のイベント リスナー */
        fun onAreaSelectDialogPositiveClick(
            dialog: DialogFragment,
            itemPosition: Int,
            area: Pair<String, String>,
            childArea: Pair<String, String>
        )

        /** エラーが発生した時のイベント リスナー */
        fun onAreaSelectDialogErrorOccurred(dialog: DialogFragment, error: String)
    }

    // Public Fields

    var isProcessing: Boolean = true

    // Private Fields

    private lateinit var areaListAdapter: AreaListAdapter
    private val areas = arrayListOf<Pair<String, String>?>()
    private lateinit var areaSpinner: Spinner
    private lateinit var childAreaListAdapter: AreaListAdapter
    private var childAreaNeedsSelected = false
    private val childAreas = arrayListOf<Pair<String, String>?>()
    private lateinit var childAreaSpinner: Spinner
    private var initialArea: Pair<String, String>? = null
    private var initialChildArea: Pair<String, String>? = null
    private var itemPosition = -1
    private lateinit var noticeListener: NoticeDialogListener
    private lateinit var positiveButton: Button
    private var selectedArea: Pair<String, String>? = null
    private var selectedChildArea: Pair<String, String>? = null
    private lateinit var view: View

    // Over-ride Methods

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            noticeListener = context as NoticeDialogListener
        } catch (_: ClassCastException) {
            throw ClassCastException(("$context must implement NoticeDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            view = inflater.inflate(R.layout.dialog_area_select, null)
            val context = requireContext()
            areaListAdapter = AreaListAdapter(context, areas)
            areaSpinner = view.findViewById(R.id.area)
            areaSpinner.adapter = areaListAdapter

            areaSpinner.onItemSelectedListener = SpinnerItemSelectedListener(
                { area -> onAreaSelected(area) },
                { onAreaNothingSelected() },
                areaListAdapter
            )

            childAreaListAdapter = AreaListAdapter(context, childAreas)
            childAreaSpinner = view.findViewById(R.id.child_area)
            childAreaSpinner.adapter = childAreaListAdapter

            childAreaSpinner.onItemSelectedListener = SpinnerItemSelectedListener(
                { childArea -> onChildAreaSelected(childArea) },
                { onChildAreaNothingSelected() },
                childAreaListAdapter
            )

            builder.setView(view)
                .setPositiveButton(
                    "OK",
                    { dialog, id ->
                        noticeListener.onAreaSelectDialogPositiveClick(
                            this,
                            itemPosition,
                            selectedArea!!,
                            selectedChildArea!!
                        )
                    })
                .setNegativeButton(
                    "キャンセル",
                    { dialog, id -> getDialog()!!.cancel() })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog
        positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        setSpinnersEnabled(false)
        getAreas()
    }

    // Public Methods

    /**
     * ダイアログを表示します。
     * @param area エリア
     * @param childArea 子エリア
     */
    fun show(
        area: Pair<String, String>?,
        childArea: Pair<String, String>?,
        itemPosition: Int,
        manager: FragmentManager,
        tag: String?
    ) {
        this.initialArea = area
        this.initialChildArea = childArea
        this.itemPosition = itemPosition
        show(manager, tag)
    }

    // Private Methods

    /** エリアが選択された場合のイベント リスナー */
    private fun onAreaSelected(area: Pair<String, String>?) {
        selectedArea = area

        if (isProcessing) {
            isProcessing = false

            if (!childAreaNeedsSelected) return
        }

        area?.let { getChildAreas(it) }
    }

    /** 定義済のエリアを取得します。 */
    private fun getAreas() {
        areaListAdapter.clear()
        positiveButton.isEnabled = false
        val fragment = this

        lifecycleScope.launch {
            try {
                val result = HttpConnector.getAreas()
                setAreas(result)
            } catch (exception: Exception) {
                exception.message?.let {
                    Log.e("MainActivity", it)
                    noticeListener.onAreaSelectDialogErrorOccurred(fragment, it)
                }
            }
        }

        areaListAdapter.notifyDataSetChanged()
    }

    /** 予報取得可能な子エリアを取得します。 */
    private fun getChildAreas(area: Pair<String, String>) {
        childAreaListAdapter.clear()
        val fragment = this

        lifecycleScope.launch {
            try {
                val result = HttpConnector.getChildAreas(area.first)
                setChildAreas(result)
            } catch (exception: Exception) {
                exception.message?.let {
                    Log.e("MainActivity", it)
                    noticeListener.onAreaSelectDialogErrorOccurred(fragment, it)
                }
            }
        }

        childAreaListAdapter.notifyDataSetChanged()
    }

    /** エリアが何も選択されなかった場合のイベント リスナー */
    private fun onAreaNothingSelected() {
        childAreaListAdapter.clear()
        childAreaListAdapter.notifyDataSetChanged()
    }

    /** 子エリアが選択された場合のイベント リスナー */
    private fun onChildAreaSelected(childArea: Pair<String, String>?) {
        selectedChildArea = childArea

        if (childArea != null && selectedArea != null) {
            positiveButton.isEnabled = true
        }
    }

    /** 子エリアが何も選択されなかった場合のイベント リスナー */
    private fun onChildAreaNothingSelected() {
        positiveButton.isEnabled = false
    }

    /** Spinner 要素にエリアを設定します。 */
    private fun setAreas(areas: Map<String, String>) {
        val list = areas.toList()
        areaListAdapter.addAll(list)

        if (initialArea != null) {
            val found = list.find { l ->
                l.first == initialArea!!.first
            }

            if (found != null) {
                val foundPosition = areaListAdapter.getPosition(found)
                areaSpinner.setSelection(foundPosition)
            } else {
                // 現在存在しないエリアの場合は、そのまま選択できるようにする
                areaListAdapter.insert(initialArea, 0)
            }
        }

        initialArea = null
        childAreaNeedsSelected = true
        isProcessing = true
        areaListAdapter.notifyDataSetChanged()
    }

    /** Spinner 要素に子エリアを設定します。 */
    private fun setChildAreas(childAreas: Map<String, String>) {
        val list = childAreas.toList()
        childAreaListAdapter.addAll(list)

        if (childAreaNeedsSelected) {
            initialChildArea?.let {
                val found = list.find { l ->
                    l.first == initialChildArea?.first
                }

                if (found != null) {
                    val foundPosition = childAreaListAdapter.getPosition(found)
                    childAreaSpinner.setSelection(foundPosition)
                } else {
                    // 現在存在しないエリアの場合は、そのまま選択できるようにする
                    childAreaListAdapter.insert(initialChildArea, 0)
                }

                initialChildArea = null
            }

            childAreaNeedsSelected = false
        }

        setSpinnersEnabled(true)
        childAreaListAdapter.notifyDataSetChanged()
    }

    /** Spinner の有効又は無効を設定します。 */
    private fun setSpinnersEnabled(enabled: Boolean) {
        areaSpinner.isEnabled = enabled
        childAreaSpinner.isEnabled = enabled
    }
}
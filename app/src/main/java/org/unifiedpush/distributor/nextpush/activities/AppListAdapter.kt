package org.unifiedpush.distributor.nextpush.activities

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isGone
import com.google.android.material.color.MaterialColors
import org.unifiedpush.distributor.nextpush.Database.Companion.getDb
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.utils.getApplicationName

data class App(
    val token: String,
    val packageId: String
)

class AppListAdapter(context: Context, private val resource: Int, apps: List<App>) : ArrayAdapter<App>(context, resource, apps) {
    private var selectedItemsIds = SparseBooleanArray()
    private val inflater = LayoutInflater.from(context)
    private val db = getDb(context)

    private class ViewHolder {
        var name: TextView? = null
        var description: TextView? = null
    }

    override fun getView(position: Int, pConvertView: View?, parent: ViewGroup): View {
        var viewHolder: ViewHolder? = null
        val convertView = pConvertView?.apply {
            viewHolder = tag as ViewHolder
        } ?: run {
            val rConvertView = inflater.inflate(resource, parent, false)
            viewHolder = ViewHolder().apply {
                this.name = rConvertView.findViewById(R.id.item_app_name) as TextView
                this.description = rConvertView.findViewById(R.id.item_description) as TextView
            }
            rConvertView.apply {
                tag = viewHolder
            }
        }
        getItem(position)?.let {
            if (it.packageId == context.packageName) {
                setViewHolderForLocalChannel(viewHolder, it)
            } else {
                setViewHolderForUnifiedPushApp(viewHolder, it)
            }
        }
        if (selectedItemsIds.get(position)) {
            convertView?.setBackgroundColor(
                MaterialColors.getColor(convertView, com.google.android.material.R.attr.colorOnTertiary)
            )
        } else {
            convertView?.setBackgroundResource(0)
        }
        return convertView
    }

    private fun setViewHolderForUnifiedPushApp(viewHolder: ViewHolder?, app: App) {
        context.getApplicationName(app.packageId)?.let {
            viewHolder?.name?.text = it
            viewHolder?.description?.text = app.packageId
        } ?: run {
            viewHolder?.name?.text = app.packageId
            viewHolder?.description?.isGone = true
        }
    }

    private fun setViewHolderForLocalChannel(viewHolder: ViewHolder?, app: App) {
        val title = db.getNotificationTitle(app.token)
        viewHolder?.name?.text = context.getString(R.string.local_notif_title).format(title)
        viewHolder?.description?.text = context.getString(R.string.local_notif_description)
    }

    fun toggleSelection(position: Int) {
        selectView(position, !selectedItemsIds.get(position))
    }

    fun removeSelection() {
        selectedItemsIds = SparseBooleanArray()
        notifyDataSetChanged()
    }

    private fun selectView(position: Int, value: Boolean) {
        selectedItemsIds.put(position, value)
        notifyDataSetChanged()
    }

    fun getSelectedIds(): SparseBooleanArray {
        return selectedItemsIds
    }
}

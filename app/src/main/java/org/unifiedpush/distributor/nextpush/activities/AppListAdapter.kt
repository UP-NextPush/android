package org.unifiedpush.distributor.nextpush.activities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isGone
import com.google.android.material.color.MaterialColors
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.utils.TAG

data class App(
    val token: String,
    val packageId: String
)

class AppListAdapter(context: Context, private val resource: Int, apps: List<App>) : ArrayAdapter<App>(context, resource, apps) {
    private var selectedItemsIds = SparseBooleanArray()
    private val inflater = LayoutInflater.from(context)

    private class ViewHolder {
        var name: TextView? = null
        var packageId: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var viewHolder: ViewHolder? = null
        val convertView = convertView?.apply {
            viewHolder = tag as ViewHolder
        } ?: run {
            val rConvertView = inflater.inflate(resource, parent, false)
            viewHolder = ViewHolder().apply {
                this.name = rConvertView.findViewById(R.id.item_app_name) as TextView
                this.packageId = rConvertView.findViewById(R.id.item_app_id) as TextView
            }
            rConvertView.apply {
                tag = viewHolder
            }
        }
        getItem(position)?.let {
            try {
                val ai = if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getApplicationInfo(
                        it.packageId,
                        PackageManager.ApplicationInfoFlags.of(
                            PackageManager.GET_META_DATA.toLong()
                        )
                    )
                } else {
                    context.packageManager.getApplicationInfo(it.packageId, 0)
                }
                viewHolder?.name?.text = context.packageManager.getApplicationLabel(ai)
                viewHolder?.packageId?.text = it.packageId
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Could not resolve app name", e)
                viewHolder?.name?.text = it.packageId
                viewHolder?.packageId?.isGone = true
            }
        }
        if (selectedItemsIds.get(position)) {
            convertView?.setBackgroundColor(
                MaterialColors.getColor(convertView, R.attr.colorOnTertiary)
            )
        } else {
            convertView?.setBackgroundResource(0)
        }
        return convertView
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

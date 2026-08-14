package uk.akane.omni.ui.components

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButtonToggleGroup
import uk.akane.omni.R

/**
 * A two-option preference rendered as a segmented button instead of a switch.
 *
 * A switch reads as "off is the absence of the thing", which is wrong for choices where neither
 * option is a default state: metric and imperial are peers, not on and off. The persisted value
 * stays a plain boolean so it is interchangeable with a SwitchPreferenceCompat: false selects the
 * first segment, true selects the second.
 */
class SegmentedPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    private var isEndSelected: Boolean = false

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any = a.getBoolean(index, false)

    override fun onSetInitialValue(defaultValue: Any?) {
        isEndSelected = getPersistedBoolean(defaultValue as? Boolean ?: false)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.isClickable = false
        holder.itemView.isFocusable = false

        val toggleGroup = holder.findViewById(R.id.segmented_group) as MaterialButtonToggleGroup

        // The holder is recycled, so drop the previous binding's listener before restoring state or
        // the restore itself would be reported as a user selection.
        toggleGroup.clearOnButtonCheckedListeners()
        toggleGroup.check(if (isEndSelected) R.id.segmented_end else R.id.segmented_start)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selection = checkedId == R.id.segmented_end
            if (selection == isEndSelected) return@addOnButtonCheckedListener
            if (callChangeListener(selection)) {
                isEndSelected = selection
                persistBoolean(selection)
            } else {
                toggleGroup.check(if (isEndSelected) R.id.segmented_end else R.id.segmented_start)
            }
        }
    }
}

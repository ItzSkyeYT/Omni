package uk.akane.omni.ui.fragments

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uk.akane.omni.R
import uk.akane.omni.logic.enableEdgeToEdgePaddingListener

/**
 * BasePreferenceFragment:
 *   A base fragment for all SettingsTopFragment. It
 * is used to make overlapping color easier.
 *
 * @author AkaneTan
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Keeps the settings list a readable column instead of letting it stretch the full width of
     * the window. On a landscape phone the rows are otherwise about 1040dp wide, which strands
     * each switch a long way from the label it belongs to and pushes summary lines far past a
     * comfortable line length.
     *
     * Applied as an item decoration rather than padding on purpose: enableEdgeToEdgePaddingListener
     * captures the RecyclerView's padding when it is registered and reapplies it on every inset
     * pass, so padding set here would be overwritten or doubled up.
     */
    private class MaxWidthDecoration(private val maxWidthPx: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            val available = parent.width - parent.paddingLeft - parent.paddingRight
            val gutter = ((available - maxWidthPx) / 2).coerceAtLeast(0)
            outRect.left = gutter
            outRect.right = gutter
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainer))
        val recyclerView = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)!!
        recyclerView.enableEdgeToEdgePaddingListener()
        recyclerView.addItemDecoration(
            MaxWidthDecoration(resources.getDimensionPixelSize(R.dimen.settings_content_max_width))
        )
        // Keyed off available width rather than orientation, so split-screen and freeform windows
        // get the same treatment. Offsets are only recomputed when the width actually changes.
        recyclerView.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) recyclerView.invalidateItemDecorations()
        }
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(ColorDrawable(Color.TRANSPARENT))
    }

    override fun setDividerHeight(height: Int) {
        super.setDividerHeight(0)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        // Work around b/331383944: PreferenceFragmentCompat permanently mutates activity theme (enables vertical scrollbars)
        requireContext().theme.applyStyle(R.style.Theme_Omni, true)
        super.onDestroy()
    }

}
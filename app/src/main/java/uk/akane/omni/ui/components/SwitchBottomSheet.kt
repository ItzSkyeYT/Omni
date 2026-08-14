package uk.akane.omni.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import uk.akane.omni.R
import uk.akane.omni.ui.MainActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.color.utilities.ColorUtils

class SwitchBottomSheet(
    private val callFragmentType : CallFragmentType
) : BottomSheetDialogFragment() {

    enum class CallFragmentType {
        COMPASS,
        SPIRIT_LEVEL,
        BAROMETER,
        RULER,
        FLASHLIGHT
    }

    private lateinit var compassMaterialButton: MaterialButton
    private lateinit var spiritLevelMaterialButton: MaterialButton
    private lateinit var barometerMaterialButton: MaterialButton
    private lateinit var rulerMaterialButton: MaterialButton
    private lateinit var flashlightMaterialButton: MaterialButton

    private lateinit var targetMaterialButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.switch_bottom_sheet, container, false)

        compassMaterialButton = rootView.findViewById(R.id.compass_btn)!!
        spiritLevelMaterialButton = rootView.findViewById(R.id.spirit_leveler_btn)!!
        barometerMaterialButton = rootView.findViewById(R.id.barometer_btn)!!
        rulerMaterialButton = rootView.findViewById(R.id.ruler_btn)!!
        flashlightMaterialButton = rootView.findViewById(R.id.flashlight_btn)!!

        targetMaterialButton = when (callFragmentType) {
            CallFragmentType.COMPASS -> compassMaterialButton
            CallFragmentType.SPIRIT_LEVEL -> spiritLevelMaterialButton
            CallFragmentType.BAROMETER -> barometerMaterialButton
            CallFragmentType.RULER -> rulerMaterialButton
            CallFragmentType.FLASHLIGHT -> flashlightMaterialButton
        }

        targetMaterialButton.isChecked = true

        setOnClickListener()

        return rootView
    }

    /**
     * Both entry points go through MainActivity.showTool so a sheet tap and a swipe produce the
     * same transition, and the direction matches where the target sits in the tool order.
     */
    /**
     * Opens fully rather than at the default peek height. The peek is a fraction of the screen,
     * and a landscape phone is only about 448dp tall, so the peek came out shorter than the
     * sheet's own content and the tool buttons were cut off by the bottom of the screen. There
     * is nothing below the fold worth collapsing to here, so collapsing is skipped entirely.
     */
    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        // The sheet view itself is given a fixed height by the dialog; left alone in landscape
        // that height is shorter than the content, which is what clipped the tool buttons.
        bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.let { sheet ->
            sheet.layoutParams = sheet.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        bottomSheetDialog.behavior.apply {
            skipCollapsed = true
            isFitToContents = true
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setOnClickListener() {
        val targets = listOf(
            compassMaterialButton to MainActivity.Tool.COMPASS,
            spiritLevelMaterialButton to MainActivity.Tool.SPIRIT_LEVEL,
            rulerMaterialButton to MainActivity.Tool.RULER,
            flashlightMaterialButton to MainActivity.Tool.FLASHLIGHT
        )
        for ((button, tool) in targets) {
            button.setOnClickListener {
                if (button === targetMaterialButton) return@setOnClickListener
                val activity = requireActivity() as MainActivity
                val current = activity.currentTool()
                val direction = if (current == null || tool.ordinal >= current.ordinal) 1 else -1
                activity.showTool(tool, direction)
                dismiss()
            }
        }
    }

}
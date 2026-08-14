package uk.akane.omni.ui.fragments

import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import uk.akane.omni.R
import uk.akane.omni.logic.checkSensorAvailability
import uk.akane.omni.ui.MainActivity
import uk.akane.omni.ui.components.SpiritLevelView
import uk.akane.omni.ui.components.SwitchBottomSheet
import uk.akane.omni.ui.fragments.settings.MainSettingsFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.atan2


class LevelFragment : BaseFragment(), SensorEventListener {

    private var mainActivity: MainActivity? = null

    private var sensorManager: SensorManager? = null

    private var rotationVectorSensor: Sensor? = null

    private lateinit var sheetMaterialButton: MaterialButton
    private lateinit var settingsMaterialButton: MaterialButton

    private lateinit var levelView: SpiritLevelView

    /**
     * The screen is pinned while this tool is on top. The OS only rotates in 90-degree steps once
     * you pass roughly 45, and that discrete jump fights the continuous sensor-driven drawing
     * inside the view. Pinning it and rotating the readout on the canvas instead keeps everything
     * on one smooth path, and lets the number stay upright at every angle rather than at four.
     */
    private var doNotHaveSensor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = requireActivity() as MainActivity

        sensorManager = ContextCompat.getSystemService(requireContext(), SensorManager::class.java)

        rotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!sensorManager!!.checkSensorAvailability(Sensor.TYPE_ROTATION_VECTOR)) {
            mainActivity!!.postComplete()
            doNotHaveSensor = true
        } else {
            sensorManager!!.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()
        // Hand rotation back so the other tools still turn.
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onDestroy() {
        sensorManager!!.unregisterListener(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_spirit_level, container, false)

        sheetMaterialButton = rootView.findViewById(R.id.sheet_btn)!!
        settingsMaterialButton = rootView.findViewById(R.id.settings_btn)!!

        levelView = rootView.findViewById(R.id.level_view)!!

        settingsMaterialButton.setOnClickListener {
            mainActivity!!.startFragment(MainSettingsFragment())
        }

        sheetMaterialButton.setOnClickListener {
            SwitchBottomSheet(SwitchBottomSheet.CallFragmentType.SPIRIT_LEVEL).show(parentFragmentManager, "switch_bottom_sheet")
        }

        if (doNotHaveSensor) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.warning_dialog_title))
                .setMessage(resources.getString(R.string.warning_dialog_text))
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(resources.getString(R.string.dismiss), null)
                .show()
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            settingsMaterialButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.sprt_btn_marginBottom)
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> updateCompass(event)
        }
        if (mainActivity?.isInflationStarted() == false) {
            mainActivity!!.postComplete()
        }
    }

    private fun updateCompass(event: SensorEvent) {
        val rotationVector = floatArrayOf(event.values[0], event.values[1], event.values[2])

        val rotationMatrix = FloatArray(16)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        // No display remap: the screen is pinned, so the display frame and the device frame are
        // the same thing and the bubble should track tilt in the device's own axes. The reading
        // itself is remap-invariant anyway, being the magnitude of pitch and roll together.
        val orientationInRadians = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationInRadians)

        val pitchInDegrees = Math.toDegrees(orientationInRadians[1].toDouble()).toFloat()
        val rollInDegrees = Math.toDegrees(orientationInRadians[2].toDouble()).toFloat()

        // Where "up" points, expressed in the device's own axes: the third row of the rotation
        // matrix is world-up in device coordinates, and its x/y part is that direction projected
        // onto the screen. atan2 keeps the quadrant, so this is continuous through a full turn.
        // The previous asin() version could only express -90..90 and lost which way round it was,
        // which is what all the 180-minus-angle patching downstream was compensating for.
        val uprightAngle = Math.toDegrees(
            atan2(rotationMatrix[8].toDouble(), rotationMatrix[9].toDouble())
        ).toFloat()

        levelView.updatePitchAndRollAndBalance(pitchInDegrees, rollInDegrees, uprightAngle)
    }

}
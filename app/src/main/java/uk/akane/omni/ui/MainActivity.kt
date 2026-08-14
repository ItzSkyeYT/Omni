package uk.akane.omni.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import uk.akane.omni.R
import com.google.android.material.transition.MaterialSharedAxis
import uk.akane.omni.logic.enableEdgeToEdgeProperly
import uk.akane.omni.ui.fragments.CompassFragment
import uk.akane.omni.ui.fragments.FlashlightFragment
import uk.akane.omni.ui.fragments.LevelFragment
import uk.akane.omni.ui.fragments.RulerFragment
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private var ready: Boolean = false

    /** Tool order, shared by the switcher sheet and by swiping. */
    enum class Tool(val create: () -> Fragment) {
        COMPASS({ CompassFragment() }),
        SPIRIT_LEVEL({ LevelFragment() }),
        RULER({ RulerFragment() }),
        FLASHLIGHT({ FlashlightFragment() })
    }

    private var swipeDetector: GestureDetector? = null

    fun currentTool(): Tool? = when (supportFragmentManager.findFragmentById(R.id.container)) {
        is CompassFragment -> Tool.COMPASS
        is LevelFragment -> Tool.SPIRIT_LEVEL
        is RulerFragment -> Tool.RULER
        is FlashlightFragment -> Tool.FLASHLIGHT
        else -> null
    }

    /** Wraps around, so the tools form a loop in either direction. */
    fun switchTool(delta: Int) {
        val current = currentTool() ?: return
        val tools = Tool.entries
        val target = tools[(current.ordinal + delta).mod(tools.size)]
        if (target != current) showTool(target, delta)
    }

    /**
     * Uses MaterialSharedAxis rather than setCustomAnimations: BaseFragment already gives every
     * screen transitions, and mixing transitions with animations in one transaction leaves the
     * entering fragment invisible. X-axis is the horizontal counterpart of the swipe.
     */
    fun showTool(tool: Tool, direction: Int = 1) {
        val forward = direction >= 0
        supportFragmentManager.findFragmentById(R.id.container)?.apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, forward)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, !forward)
        }
        val next = tool.create().apply {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, forward)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, !forward)
        }
        supportFragmentManager.commit {
            replace(R.id.container, next)
            runOnCommit {
                // Re-evaluate chrome on the switch itself, so a swipe lands in the right state
                // instead of waiting for the next activity resume.
                applyImmersiveModeForOrientation()
            }
        }
    }

    /**
     * Swiping is read at the window level so every tool screen gets it without each fragment
     * wiring up its own detector. It is ignored while a sub-screen such as settings is on the
     * back stack, and a fling has to be decisively horizontal so it cannot fight a slider drag.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) swipeDetector?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun createSwipeDetector(): GestureDetector {
        val minDistance = 96f * resources.displayMetrics.density
        val minVelocity = ViewConfiguration.get(this).scaledMinimumFlingVelocity * 2f
        return GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) < minDistance || abs(dx) < abs(dy) * 1.5f) return false
                if (abs(velocityX) < minVelocity) return false
                // Swiping left reveals the next tool, matching the order in the switcher.
                switchTool(if (dx < 0) 1 else -1)
                return true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash hold cancels every draw pass of the window while it is true, so a tool that
        // never reports readiness would hold it forever and the window would never be painted at
        // all, which reads as a black screen with no exception anywhere. A recreation has nothing
        // to wait for: the content is restored synchronously.
        if (savedInstanceState != null) ready = true
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeProperly()
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Backstop so no tool can wedge the splash by forgetting to report in. Deliberately
        // delayed rather than posted to the next frame: the compass holds the splash on purpose
        // until its first sensor reading arrives (a few frames) so the dial appears already
        // oriented, and releasing immediately would show it pointing north and then snapping.
        window.decorView.postDelayed({ postComplete() }, SPLASH_TIMEOUT_MS)
        swipeDetector = createSwipeDetector()

        // Only act on the launch shortcut the first time. A rotation recreates the activity with
        // the same intent, so re-running this would stack a second copy of the tool on top of the
        // restored one and leave a blank screen. The extra is consumed so it cannot fire again.
        if (savedInstanceState == null && intent.hasExtra("targetFragment")) {
            val target = intent.getIntExtra("targetFragment", 0)
            intent.removeExtra("targetFragment")
            when (target) {
                1 -> {
                    startFragment(LevelFragment())
                    postComplete()
                }
                2 -> {
                    startFragment(RulerFragment())
                    postComplete()
                }
                3 -> {
                    startFragment(FlashlightFragment())
                    postComplete()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveModeForOrientation()
    }

    /**
     * Landscape is the orientation you hold the phone in to actually read an instrument, so hand
     * the whole screen over to it and let the bars return on a swipe. Portrait keeps normal chrome.
     * The spirit level is always edge to edge: it is pinned to portrait so the landscape rule
     * would never fire for it, and the dial rotates through every angle, which status bar chrome
     * sitting at a fixed angle would only argue with.
     */
    private fun applyImmersiveModeForOrientation() {
        val immersive = currentTool() == Tool.SPIRIT_LEVEL ||
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        setSystemBarsHidden(immersive)
    }

    fun setSystemBarsHidden(hidden: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (hidden) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun postComplete() = run { ready = true }

    fun isInflationStarted() = ready

    fun startFragment(frag: Fragment, args: (Bundle.() -> Unit)? = null) {
        supportFragmentManager.commit {
            // replace() already removes the container's occupant, so there is nothing to hide.
            // The old hide() chose its target with fragments.last(), which is the tail of the
            // whole added list: a shown BottomSheetDialogFragment lands there too, and the call
            // throws outright if that list is ever empty.
            setReorderingAllowed(true)
            addToBackStack(null)
            replace(R.id.container, frag.apply { args?.let { arguments = Bundle().apply(it) } })
        }
    }

    companion object {
        /** Upper bound on how long the splash may hold the window before it is force-released. */
        private const val SPLASH_TIMEOUT_MS = 1500L
    }
}

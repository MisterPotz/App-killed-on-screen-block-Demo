package ru.mrpotz.killingsample

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import ru.mrpotz.killingsample.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var _binding: FragmentSecondBinding? = null
    var seconds = 0L
    var lastUpdateAt = 0L
    var multiplier = 1

    val counterModifyRunnable = object : Runnable {
        override fun run() {
            if (_binding != null && view != null && view?.isAttachedToWindow == true) {
                val current = System.currentTimeMillis()
                seconds += (current - lastUpdateAt) * multiplier
                lastUpdateAt = current
                _binding!!.textviewSecond.text = "current counter: $seconds"
                mainHandler.removeCallbacks(this)
                mainHandler.postDelayed(this, 1000)
            } else {
                mainHandler.removeCallbacks(this)
            }
        }
    }
    private val rotationViewModel by lazy {
        ViewModelProvider(this).get(RotationViewModel::class.java)
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreOrCreateSaveData(savedInstanceState)
        rotationViewModel.viewIsToBeDestroyedAndRotated = false

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    private fun decideToRotateScreen(isClosing: Boolean): Boolean {
        if (rotationViewModel.viewIsToBeDestroyedAndRotated) return false
        val rotationInt = currentRotationInt()
        val newRotation: Rotation?

        val targetRotation = Rotation.LANDSCAPE
        val currentRotation = Rotation.fromRotationInt(rotationInt)

        newRotation = if (isClosing) {
            // must get back to initial rotation
            rotationViewModel.initialRotation
        } else {
            if (currentRotation != targetRotation) {
                targetRotation
            } else {
                null
            }
        }

        if (newRotation != null) {
            if (activity?.requestedOrientation != newRotation.toActivityInfoRotation()) {
                activity?.requestedOrientation = newRotation.toActivityInfoRotation()
                rotationViewModel.viewIsToBeDestroyedAndRotated = true
            } else {
                rotationViewModel.viewIsToBeDestroyedAndRotated = false
            }
        }
        return rotationViewModel.viewIsToBeDestroyedAndRotated
    }

    fun checkIfDontNeedToRotateAtStart(): Boolean {
        return !decideToRotateScreen(false)
    }

    private fun restoreOrCreateSaveData(savedInstanceState: Bundle?) {
        // if there is already saveData that means that the screen was already rotated and the initial screen state was saved
        if (savedInstanceState == null) {
            saveInitialScreenOrientation()
        }
    }

    private fun saveInitialScreenOrientation() {
        rotationViewModel.initialRotation = Rotation.fromRotationInt(currentRotationInt())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lastUpdateAt = System.currentTimeMillis()
        mainHandler.postDelayed(counterModifyRunnable, 0)
        binding.buttonMultiplier.setOnClickListener {
            multiplier += 2
        }
        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        if (checkIfDontNeedToRotateAtStart()) {
            Toast.makeText(requireContext(), "don't need to rotate", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "will rotate", Toast.LENGTH_SHORT).show();
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun currentRotationInt(): Int {
        val windowManager =
            (requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        return windowManager.defaultDisplay.rotation
    }
}

class RotationViewModel() : ViewModel() {
    var initialRotation: Rotation = Rotation.ANY
    var viewIsToBeDestroyedAndRotated: Boolean = false
}


enum class Rotation {
    ANY,
    LANDSCAPE;

    fun toActivityInfoRotation(): Int {
        return when (this) {
            LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ANY -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    companion object {
        fun fromRotationInt(rotationInt: Int): Rotation {
            return when (rotationInt) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> Rotation.LANDSCAPE
                else -> Rotation.ANY
            }
        }
    }
}

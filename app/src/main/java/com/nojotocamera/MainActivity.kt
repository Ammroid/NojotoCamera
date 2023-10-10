package com.nojotocamera

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Choreographer
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.iammert.library.cameravideobuttonlib.CameraVideoButton
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.nojotocamera.PermissionFragment.Companion.newInstance
import com.nojotocamera.databinding.ActivityMainBinding
import com.nojotocamera.encoder.MediaAudioEncoder
import com.nojotocamera.encoder.MediaEncoder
import com.nojotocamera.encoder.MediaMuxerWrapper
import com.nojotocamera.encoder.MediaVideoEncoder
import com.snap.camerakit.Session
import com.snap.camerakit.inputFrom
import com.snap.camerakit.lenses.LENS_GROUP_ID_BUNDLED
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasFirst
import iknow.android.utils.BaseUtils
import iknow.android.utils.DeviceUtil
import java.io.Closeable

private const val DEFAULT_IMAGE_INPUT_FIELD_OF_VIEW = 50F

class MainActivity : AppCompatActivity() {

    private val permissionsRequired = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )
    private val permissionsRequiredForAndroid13 =
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

    lateinit var binding: ActivityMainBinding;
    private val TAG = "MainActivity"
    private lateinit var session: Session
    private lateinit var inputSurface: Surface
    private lateinit var inputSurfaceUpdateCallback: Choreographer.FrameCallback
    private val choreographer = Choreographer.getInstance()
    private val closeOnDestroy = mutableListOf<Closeable>()

    var mediaMuxer: MediaMuxerWrapper? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
        BaseUtils.init(this)
        checkPermission()
    }

    private fun initViews() {
        var width = DeviceUtil.getDeviceWidth();
        var height = DeviceUtil.getDeviceWidth() * 1024 / 576;
        val inputTextSureSize = Size(width, height)
        with(binding) {
            session = com.snap.camerakit.Session(this).apply {
                lenses.repository.get(LensesComponent.Repository.QueryCriteria.Available(LENS_GROUP_ID_BUNDLED)) { result ->
                    result.whenHasFirst { lens ->
                        lenses.processor.apply(lens)
                    }
                }
            }
            val layoutParams: ViewGroup.LayoutParams = cameraView.layoutParams
            layoutParams.width = width
            layoutParams.height = height
            cameraView.layoutParams = layoutParams
            cameraView.visibility = View.VISIBLE
            val inputSurfaceText = cameraView.surfaceTexture.apply {
                setDefaultBufferSize(inputTextSureSize.width, inputTextSureSize.height)
                detachFromGLContext()
            }
            val input = inputFrom(
                surfaceTexture = inputSurfaceText,
                width = inputTextSureSize.width,
                height = inputTextSureSize.height,
                facingFront = true,
                rotationDegrees = 0,
                horizontalFieldOfView = DEFAULT_IMAGE_INPUT_FIELD_OF_VIEW,
                verticalFieldOfView = DEFAULT_IMAGE_INPUT_FIELD_OF_VIEW
            )
            closeOnDestroy.add(session.processor.connectInput(input))
            button.actionListener = object : CameraVideoButton.ActionListener {
                override fun onDurationTooShortError() {
                    Log.e(TAG, "onDurationTooShortError: ")
                }

                override fun onEndRecord() {
                    Log.e(TAG, "onEndRecord: ")
                    if (mediaMuxer != null) {
                        mediaMuxer!!.stopRecording()
                        mediaMuxer = null
                    }
                }

                override fun onSingleTap() {
                    Log.e(TAG, "onSingleTap: ")
                }

                override fun onStartRecord() {
                    Log.e(TAG, "onStartRecord: ")
                    recordVideo()
                }

            }
        }
    }

    private fun checkPermission() {
        val permissions: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = permissionsRequiredForAndroid13;
        } else {
            permissions = permissionsRequired
        }
        launchDialogFragment(newInstance(permissions) { permissionDeniedResponses: List<PermissionDeniedResponse?>? ->
            if (permissionDeniedResponses == null) {
                initViews()
            } else {
                Log.e(TAG, "checkPermission: Permission Denied")
            }
        })
    }

    private fun recordVideo() {
        mediaMuxer = MediaMuxerWrapper(".mp4")
        MediaVideoEncoder(mediaMuxer, object : MediaEncoder.MediaEncoderListener {
            override fun onPrepared(encoder: MediaEncoder?) {
                if (encoder is MediaVideoEncoder)
                    binding.cameraView.setVideoEncoder(encoder)
            }

            override fun onStopped(encoder: MediaEncoder?) {
                binding.cameraView.setVideoEncoder(null)
            }

        }, binding.cameraView.videoWidth, binding.cameraView.height)
        MediaAudioEncoder(mediaMuxer, object : MediaEncoder.MediaEncoderListener {
            override fun onPrepared(encoder: MediaEncoder?) {

            }

            override fun onStopped(encoder: MediaEncoder?) {

            }

        })
        mediaMuxer!!.prepare()
        mediaMuxer!!.startRecording()
    }
}
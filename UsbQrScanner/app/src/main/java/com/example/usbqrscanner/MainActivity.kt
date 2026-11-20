package com.example.usbqrscanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.usbqrscanner.databinding.ActivityMainBinding
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.UVCCamera

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var qrCodeAnalyzer: QrCodeAnalyzer

    private var cameraHelper: ICameraHelper? = null
    private var previewWidth: Int = DEFAULT_PREVIEW_WIDTH
    private var previewHeight: Int = DEFAULT_PREVIEW_HEIGHT
    private var lastResult: QrScanResult? = null
    private var isSurfaceReady: Boolean = false

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            initUsbCameraHelper()
        } else {
            binding.statusText.setText(R.string.status_waiting_permission)
        }
    }

    private val frameCallback = IFrameCallback { frame ->
        if (frame != null) {
            qrCodeAnalyzer.analyzeNv21Frame(frame, previewWidth, previewHeight)
        }
    }

    private val usbStateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            runOnUiThread {
                val name = device.productName ?: device.deviceName
                binding.statusText.text = getString(R.string.status_connecting, name)
            }
            cameraHelper?.selectDevice(device)
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            cameraHelper?.openCamera()
        }

        override fun onCameraOpen(device: UsbDevice) {
            val helper = cameraHelper ?: return
            helper.startPreview()

            val size = helper.previewSize
            if (size != null && size.width > 0 && size.height > 0) {
                previewWidth = size.width
                previewHeight = size.height
            }

            if (isSurfaceReady) {
                helper.addSurface(binding.cameraSurfaceView.holder.surface, false)
            }

            helper.setFrameCallback(frameCallback, UVCCamera.PIXEL_FORMAT_NV21)

            runOnUiThread {
                updatePreviewAspectRatio(previewWidth, previewHeight)
                val name = device.productName ?: device.deviceName
                binding.statusText.text = getString(R.string.status_connected, name)
            }
        }

        override fun onCameraClose(device: UsbDevice) {
            if (isSurfaceReady) {
                cameraHelper?.removeSurface(binding.cameraSurfaceView.holder.surface)
            }
        }

        override fun onDeviceClose(device: UsbDevice) = Unit

        override fun onDetach(device: UsbDevice) {
            runOnUiThread {
                binding.statusText.setText(R.string.status_waiting_camera)
            }
        }

        override fun onCancel(device: UsbDevice) {
            runOnUiThread {
                binding.statusText.setText(R.string.status_permission_denied)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        qrCodeAnalyzer = QrCodeAnalyzer { result ->
            runOnUiThread { renderQrResult(result) }
        }

        setupSurfaceView()
        setupControls()
        ensureCameraPermissionAndStart()
    }

    private fun setupSurfaceView() {
        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                isSurfaceReady = true
                cameraHelper?.addSurface(holder.surface, false)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isSurfaceReady = false
                cameraHelper?.removeSurface(holder.surface)
            }
        })
    }

    private fun setupControls() {
        binding.rotateButton.text = getString(R.string.btn_rotate, qrCodeAnalyzer.rotationDegrees)

        binding.rotateButton.setOnClickListener {
            val nextRotation = (qrCodeAnalyzer.rotationDegrees + 90) % 360
            qrCodeAnalyzer.rotationDegrees = nextRotation
            binding.rotateButton.text = getString(R.string.btn_rotate, nextRotation)
        }

        binding.pauseResumeButton.setOnClickListener {
            val nowPaused = !qrCodeAnalyzer.isPaused
            qrCodeAnalyzer.isPaused = nowPaused
            binding.pauseResumeButton.setText(
                if (nowPaused) R.string.btn_resume else R.string.btn_pause
            )
            if (nowPaused) {
                binding.statusText.setText(R.string.status_paused)
            }
        }

        binding.selectCameraButton.setOnClickListener {
            showUsbCameraPicker()
        }

        binding.copyButton.setOnClickListener {
            val value = lastResult?.rawValue ?: return@setOnClickListener
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", value))
            Toast.makeText(this, R.string.copied_toast, Toast.LENGTH_SHORT).show()
        }

        binding.openLinkButton.setOnClickListener {
            val url = lastResult?.urlToOpen ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun ensureCameraPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            initUsbCameraHelper()
        } else {
            binding.statusText.setText(R.string.status_waiting_permission)
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initUsbCameraHelper() {
        if (cameraHelper != null) return
        cameraHelper = CameraHelper().apply {
            setStateCallback(usbStateCallback)
        }
    }

    private fun showUsbCameraPicker() {
        val devices = cameraHelper?.deviceList.orEmpty()
        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.no_usb_cameras, Toast.LENGTH_SHORT).show()
            return
        }

        val labels = devices.map { device ->
            val name = device.productName ?: "USB Camera"
            "$name (${device.vendorId}:${device.productId})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.select_camera_title)
            .setItems(labels) { _, which ->
                cameraHelper?.selectDevice(devices[which])
            }
            .show()
    }

    private fun renderQrResult(result: QrScanResult) {
        lastResult = result
        binding.resultTypeText.text = result.typeLabel
        binding.resultValueText.text = result.displayText
        binding.copyButton.isEnabled = true
        binding.openLinkButton.isEnabled = result.urlToOpen != null
    }

    private fun updatePreviewAspectRatio(videoWidth: Int, videoHeight: Int) {
        val container = binding.previewContainer
        container.post {
            val containerW = container.width
            val containerH = container.height
            if (containerW <= 0 || containerH <= 0 || videoWidth <= 0 || videoHeight <= 0) return@post

            val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
            val containerAspect = containerW.toFloat() / containerH.toFloat()

            val params = binding.cameraSurfaceView.layoutParams as FrameLayout.LayoutParams
            if (videoAspect > containerAspect) {
                params.width = containerW
                params.height = (containerW / videoAspect).toInt()
            } else {
                params.height = containerH
                params.width = (containerH * videoAspect).toInt()
            }
            binding.cameraSurfaceView.layoutParams = params
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodeAnalyzer.close()
        cameraHelper?.release()
        cameraHelper = null
    }

    companion object {
        private const val DEFAULT_PREVIEW_WIDTH = 640
        private const val DEFAULT_PREVIEW_HEIGHT = 480
    }
}

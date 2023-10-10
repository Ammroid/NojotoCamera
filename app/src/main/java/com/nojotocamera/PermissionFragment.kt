package com.nojotocamera

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.nojotocamera.databinding.FragmentPermissionBinding

/**
 * A simple [Fragment] subclass.
 * Use the [PermissionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PermissionFragment : BottomSheetDialogFragment() {

    private var result: (MutableList<PermissionDeniedResponse>?) -> Unit = {}
    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!
    private var singlePermissionText: String = ""
    private var arrayOfPermissions = emptyArray<String>()
    private var permissions = emptyList<String>().toMutableList()
    private var mIsAfterOnSaveInstanceState = false
    private var mContext: Context? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

//        if (mContext?.javaClass?.name == MainActivity::class.java.name)
//            CleverTapUtils.buttonClick(HOME_SCREEN, PERMISSION_POPUP)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUi()

        with(binding) {
            allowPermissions.setOnClickListener {
                checkForPermissions()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setUi() {
        arrayOfPermissions.forEach {
            when (it) {
                CAMERA -> {
                    if (!hasPermissions(mContext, it)) {
                        singlePermissionText = "Allow Camera Permission"
                        this.permissions.add(CAMERA)
                        binding.cameraLayout.visibility=View.VISIBLE
                    }
                }

                RECORD_AUDIO -> {
                    if (!hasPermissions(mContext, it)) {
                        singlePermissionText = "Allow Microphone Permission"
                        this.permissions.add(RECORD_AUDIO)
                        binding.micLayout.visibility=View.VISIBLE
                    }
                }

                READ_MEDIA_AUDIO, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO -> {
                    if (!hasPermissions(mContext, it)) {
                        singlePermissionText = "Allow Storage Permission"
                        if (!hasPermissions(mContext, READ_MEDIA_AUDIO)) {
                            this.permissions.add(READ_MEDIA_AUDIO)
                        }
                        if (!hasPermissions(mContext, READ_MEDIA_IMAGES)) {
                            this.permissions.add(READ_MEDIA_IMAGES)
                        }
                        if (!hasPermissions(mContext, READ_MEDIA_VIDEO)) {
                            this.permissions.add(READ_MEDIA_VIDEO)
                        }
                        binding.storageLayout.visibility=View.VISIBLE
                    }
                }

                WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE -> {
                    if (!hasPermissions(mContext, it)) {
                        singlePermissionText = "Allow Storage Permission"
                        if (!hasPermissions(mContext, WRITE_EXTERNAL_STORAGE))
                            this.permissions.add(WRITE_EXTERNAL_STORAGE)
                        if (!hasPermissions(mContext, READ_EXTERNAL_STORAGE))
                            this.permissions.add(READ_EXTERNAL_STORAGE)
                        binding.storageLayout.visibility=View.VISIBLE
                    }
                }

                READ_CONTACTS -> {
                    if (!hasPermissions(mContext, it)) {
                        singlePermissionText = "Allow Contacts Permission"
                        this.permissions.add(READ_CONTACTS)
                        binding.contactLayout.visibility=View.VISIBLE
                    }
                }
            }
        }

        binding.allowPermissions.visibility=View.VISIBLE
        binding.generalMessage.visibility=View.VISIBLE

        if (permissions.isEmpty()) {
            result(null)
            dismiss()
        } else if (permissions.size == 1) {
            binding.allowPermissions.text = singlePermissionText
        } else if (permissions.size == 2) {
            if (permissions[0] == WRITE_EXTERNAL_STORAGE && permissions[1] == READ_EXTERNAL_STORAGE)
                binding.allowPermissions.text = singlePermissionText
        }
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PermissionFragment.
         */
        @JvmStatic
        fun newInstance(
            permission: Array<String>,
            launchPermission: (MutableList<PermissionDeniedResponse>?) -> Unit = {}
        ) = PermissionFragment().apply {
            arrayOfPermissions = permission
            this.result = launchPermission
        }
    }

    private fun checkForPermissions() {
        Dexter.withContext(mContext).withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(reports: MultiplePermissionsReport?) {
                    if (reports?.areAllPermissionsGranted() == true) {

                        result(null)
                        if (!getAppCompatActivity(mContext)?.isFinishing!! && !isRemoving && !isDetached)
                            dismiss()
                    }

                    if (reports?.deniedPermissionResponses?.isNotEmpty() == true) {
                        result(reports.deniedPermissionResponses)
                        if (!getAppCompatActivity(mContext)?.isFinishing!! && !isRemoving && !isDetached)
                            dismiss()
                    }

                    if (reports?.isAnyPermissionPermanentlyDenied == true) {
                        mContext?.showSettingDialog {
                            if (!this) {
                                if (!getAppCompatActivity(mContext)?.isFinishing!! && !isRemoving && !isDetached)
                                    dismiss()
                            }
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }
            })
            .withErrorListener {}
            .onSameThread()
            .check()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mIsAfterOnSaveInstanceState = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //    public static ShotWatch getShotWatchInstance(Context context, String screenname, String postAttachmentType) {
    //        if (shotWatch == null) {
    //            shotWatch = new ShotWatch(context.getContentResolver(), new ShotWatch.Listener() {
    //                @Override
    //                public void onScreenShotTaken(ScreenshotData screenshotData) {
    //                    System.out.println("-----onScreenShotTaken----"+screenname);
    //                    CleverTapUtils.screenshotTaken(screenname,Variables.ButtonName.SCREENSHOT,postAttachmentType);
    //
    //                }
    //            });
    //        }
    //        return shotWatch;
    //    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && permissions.equals(WRITE_EXTERNAL_STORAGE)) {
                    return Environment.isExternalStorageManager()
                } else if (ActivityCompat.checkSelfPermission(
                        context,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }
    fun getAppCompatActivity(mContext: Context?): AppCompatActivity? {
        var context = mContext
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
    fun Context.showSettingDialog(isShowOrCancel: Boolean.() -> Unit = {}) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, _ ->
            dialog.cancel()
            openSettings(this)
            isShowOrCancel(true)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.cancel()
            isShowOrCancel(false)
        }
        builder.show()
    }
    private fun openSettings(mContext: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", mContext.packageName, null)
        intent.data = uri
        (mContext as Activity).startActivityForResult(intent, 101)
    }
}
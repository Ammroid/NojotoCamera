package com.nojotocamera

import androidx.appcompat.app.AppCompatActivity
import com.nojotocamera.dialogpresenter.DialogFragmentPresenter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.nojotocamera.dialogpresenter.DialogFragmentPresenterProviders.of

fun Fragment.launchDialogFragment(dialog: DialogFragment) {
    val fragmentPresenter = of(context as FragmentActivity)
    launchDialog(dialog, fragmentPresenter, childFragmentManager)
}
fun AppCompatActivity.launchDialogFragment(dialog: DialogFragment) {
    val fragmentPresenter = of(this)
    launchDialog(dialog, fragmentPresenter, supportFragmentManager)
}
fun launchDialog(
    dialog: DialogFragment,
    fragmentPresenter: DialogFragmentPresenter?,
    fragmentManager: FragmentManager
) {
    if (!fragmentManager.isDestroyed) {
        if (fragmentPresenter != null) {
            fragmentPresenter.show(dialog, dialog.tag)
        } else {
            dialog.show(fragmentManager, dialog.tag)
        }
    }
}
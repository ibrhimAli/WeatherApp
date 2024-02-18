package com.musalasoft.weatherapp.fragments

import com.musalasoft.weatherapp.R
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AboutDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val VersionName: String?
        val context = context
        VersionName = try {
            context!!.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            getString(R.string.about_unknown)
        }
        val alertDialog: AlertDialog = AlertDialog.Builder(
            context!!
        )
            .setTitle(getText(R.string.app_name))
            .setMessage(
                TextUtils.concat(
                    VersionName, "\n\n",
                    getText(R.string.about_description), "\n\n",
                )
            )
            .setPositiveButton(R.string.dialog_ok, null)
            .create()
        alertDialog.show()
        val message = alertDialog.findViewById<TextView>(android.R.id.message)
        if (message != null) {
            message.movementMethod = LinkMovementMethod.getInstance()
        }
        return alertDialog
    }
}

package com.sleepygloa.vision.api

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtil {


    fun requestPermission(activity: Activity, requestcode:Int, vararg permissions: String )
    : Boolean{
        var granted = true
        var permissionNeeded = ArrayList<String>()

        permissions.forEach{
            val permissionCheck = ContextCompat.checkSelfPermission(activity, it)
            var hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
            granted = granted and hasPermission
            if(!hasPermission){
                permissionNeeded.add(it)
            }
        }
        if(granted){
            return true
        }else{
            ActivityCompat.requestPermissions(
                activity, permissionNeeded.toTypedArray(), requestcode
            )
            return false
        }
    }

}
package com.sleepygloa.vision.api

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupListener();
    }


    private fun setupListener(){

        upload_image.setOnClickListener(){
            //1
            //UploadChooser().show(supportFragmentManager, "")

            //2
            UploadChooser().apply {
                addNotifier(object:UploadChooser.UploadChooserNotifierInterface{
                    override fun cameraOnClick() {
                        Log.d("upload", "camera onclick")
                        //카메라 권한
                        checkCameraPermission()
                    }

                    override fun galleryOnClick() {
                        Log.d("upload", "gallery onclick")
                        //사진첩 권한
                        checkGalleryPermission()
                    }
                })

            }.show(supportFragmentManager, "")
        }
    }
    private fun checkCameraPermission(){
        PermissionUtil().requestPermission(
            this,
            CAMERA_PERMISSION_REQUEST,
            Manifest.permission.CAMERA
        )
    }
    private fun checkGalleryPermission(){
        PermissionUtil().requestPermission(
            this,
            GALLERY_PERMISSION_REQUEST,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }
}

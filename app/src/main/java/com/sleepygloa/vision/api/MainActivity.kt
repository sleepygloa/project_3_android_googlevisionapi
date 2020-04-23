package com.sleepygloa.vision.api

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.File

class MainActivity : AppCompatActivity() {


    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser: UploadChooser? = null

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
            uploadChooser = UploadChooser().apply {
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

            }
            uploadChooser!!.show(supportFragmentManager, "")
        }
    }
    private fun checkCameraPermission(){
        if(PermissionUtil().requestPermission(this,CAMERA_PERMISSION_REQUEST,Manifest.permission.CAMERA)
        ){
            openCamera()
        }
    }
    private fun checkGalleryPermission(){
        if(PermissionUtil().requestPermission(this,GALLERY_PERMISSION_REQUEST,Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            openGallery()
        }

    }

    private fun openGallery(){
        val intent = Intent().apply {
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a Photo"), GALLERY_PERMISSION_REQUEST)
    }

    private fun openCamera(){
        val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName+".provider", createCameraFile())

        startActivityForResult(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply{
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            CAMERA_PERMISSION_REQUEST -> {
                if(resultCode != Activity.RESULT_OK) return;
                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName+".provider", createCameraFile())
                uploadImage(photoUri)
            }
            GALLERY_PERMISSION_REQUEST ->  data?.let{ uploadImage(it.data) }
        }
    }

    private fun uploadImage(imageUri: Uri){
        val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            uploaded_image.setImageBitmap(bitmap)
            uploadChooser?.dismiss()
    }

    private fun createCameraFile (): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            GALLERY_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode, GALLERY_PERMISSION_REQUEST, grantResults)) openGallery()
            }
            CAMERA_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode, CAMERA_PERMISSION_REQUEST, grantResults)) openCamera()
            }
        }
    }
}

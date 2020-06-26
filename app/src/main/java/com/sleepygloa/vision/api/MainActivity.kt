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
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.File

class MainActivity : AppCompatActivity() {


    private val CAMERA_PERMISSION_REQUEST = 1000 //카메라 요청 변수
    private val GALLERY_PERMISSION_REQUEST = 1001 //사진첩 요청 변수
    private val FILE_NAME = "picture.jpg" //사진 기본 이름
    private var uploadChooser: UploadChooser? = null //
    private var labelDectionTask: LabelDetectionTask? = null // 
    val LABEL_DETECTION_REQUEST = "label_detection_request" //라벨 분석 요청 변수
    val LANDMARK_DETECTION_REQUEST = "landmark_detection_request" //랜드마크 분석 요청 변수


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //라벨요청하는 클래스 변수로 저장
        labelDectionTask = LabelDetectionTask(
            packageName,
            packageManager,
            this
        )
        
        //기본 리스너 설정
        setupListener();
    }


    /********************************************************
     * 기본 리스너 설정
     ********************************************************/
    private fun setupListener(){
        upload_image.setOnClickListener(){
            //1
            //UploadChooser().show(supportFragmentManager, "")

            //2
            /**************************************
             * 이미지 업로드 버튼 클릭시 권한 요청 알림
             **************************************/
            uploadChooser = UploadChooser().apply {
                addNotifier(object:UploadChooser.UploadChooserNotifierInterface{
                    override fun cameraOnClick() {
                        Log.d("upload", "camera onclick")
                        //카메라 권한 요청
                        checkCameraPermission()
                    }
                    override fun galleryOnClick() {
                        Log.d("upload", "gallery onclick")
                        //사진첩 권한 요청
                        checkGalleryPermission()
                    }
                })
            }
            uploadChooser!!.show(supportFragmentManager, "")
        }
    }

    //카메라 권한요청
    private fun checkCameraPermission(){
        if(PermissionUtil().requestPermission(this,CAMERA_PERMISSION_REQUEST,Manifest.permission.CAMERA)
        ){
            openCamera()
        }
    }
    //사진첩 권한요청
    private fun checkGalleryPermission(){
        if(PermissionUtil().requestPermission(this,GALLERY_PERMISSION_REQUEST,Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            openGallery()
        }

    }
    //카메라열기
    private fun openCamera(){
        val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName+".provider", createCameraFile())

        startActivityForResult(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply{
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, CAMERA_PERMISSION_REQUEST
        )
    }
    //사진첩 열기
    private fun openGallery(){
        val intent = Intent().apply {
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a Photo"), GALLERY_PERMISSION_REQUEST)
    }

    //카메라, 사진첩 등 activity 결과
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

    /***************************************
     * 사진 업로드
     ***************************************/
    private fun uploadImage(imageUri: Uri?){
        val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        uploadChooser?.dismiss()
        //requestCloudVisionApi(bitmap)

        /*********************************************
         * 사진선택시 google Cloud API 기능 선택
         * click event
         *********************************************/
        DetectionChooser().apply {
            addDetectionChooserNotifierInterface(object:DetectionChooser.DetectionChooserNotifierInterface{
                override fun detectLabel() {
                    findViewById<ImageView>(R.id.uploaded_image).setImageBitmap(bitmap)
                    requestCloudVisionApi(bitmap, LABEL_DETECTION_REQUEST)
                }

                override fun detectLanmark() {
                    findViewById<ImageView>(R.id.uploaded_image).setImageBitmap(bitmap)
                    requestCloudVisionApi(bitmap, LANDMARK_DETECTION_REQUEST)
                }
            })
        }.show(supportFragmentManager, "")
        //DetectionChooser().show(supportFragmentManager, "")
    }

    /**********************************************************************
     * google Cloud API 분석 결과 -> Text 저장
     **********************************************************************/
    private fun requestCloudVisionApi(bitmap: Bitmap, requestType: String){
        labelDectionTask?.requestCloudVisionApi(bitmap, object : LabelDetectionTask.LabelDetectionNotifierInterface {
            override fun notifyResult(result: String) {
                uploaded_image_result.text = result
            }
        }, requestType)
    }

    /**************************************
     * 카메라 사진 촬영후 파일 경로 및 저장
     **************************************/
    private fun createCameraFile (): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }


    /***************************************
     * 카메라 및 사진첩 권한 요청 이후 결과
     ***************************************/
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

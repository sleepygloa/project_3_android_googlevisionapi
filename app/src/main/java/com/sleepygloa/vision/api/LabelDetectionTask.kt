package com.sleepygloa.vision.api

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.AnnotateImageRequest
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse
import com.google.api.services.vision.v1.model.Feature
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class LabelDetectionTask (
    private val packageName : String,
    private val packageManager: PackageManager,
    private val activity: MainActivity
)
{

    private val CLOUD_VISION_API_KEY = "AIzaSyBvAj6h6RSCop0pIkggkSpZVvqSYYVKXKI"
    private val ANDROID_PACKAGE_HEADER = "X-Android-Package"
    private val ANDROID_CERT_HEADER = "X-Adnroid-cert"
    private val MAX_LAFTEL_RESULTS = 10
    private var labelDetectionNotifierInterface: LabelDetectionNotifierInterface? = null
    private var requestType:String? = null

    interface LabelDetectionNotifierInterface{
        fun notifyResult(result:String)
    }

    fun requestCloudVisionApi(bitmap: Bitmap, labelDetectionNotifierInterface: LabelDetectionNotifierInterface, requestType:String){
        this.requestType = requestType
        this.labelDetectionNotifierInterface = labelDetectionNotifierInterface
        val visionTask = ImageRequestTask(prepareImageRequest(bitmap))
        visionTask.execute()
    }


    inner class ImageRequestTask constructor(
        //activity: MainActivity,
        val request : Vision.Images.Annotate
    ) : AsyncTask<Any, Void, String>(){
        private val weakReference : WeakReference<MainActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun doInBackground(vararg params: Any?): String {
            try{
                val response = request.execute()
                return findPropertResponseType(response)
            }catch(e: Exception){

            }
            return "분석 실패"
        }

        override fun onPostExecute(result: String?) {
            //super.onPostExecute(result)
            val activity = weakReference.get()
            if(activity != null && !activity.isFinishing){
//                uploaded_image_result.text = result
                Log.d("testt", "result: "+result)
                //activity.findViewById<TextView>(R.id.uploaded_image)
                result?.let{
                    labelDetectionNotifierInterface?.notifyResult(it)
                }
            }
        }
    }

    private fun prepareImageRequest(bitmap:Bitmap): Vision.Images.Annotate{
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        /********************************************************************************
         * 요청을 보내는 헤더
         ********************************************************************************/
        val requestInitializer = object : VisionRequestInitializer(CLOUD_VISION_API_KEY){
            override fun initializeVisionRequest(request : VisionRequest<*>?){
                super.initializeVisionRequest(request)
                val packageName = packageName
                request?.requestHeaders?.set(ANDROID_PACKAGE_HEADER, packageName)
                val sig = PackageManagerUtil().getSignature(packageManager, packageName)
                request?.requestHeaders?.set(ANDROID_CERT_HEADER, sig)
            }
        }

        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer);
        //builder.setApplicationName(firebaseUtil.getApplicationName())
        builder.setApplicationName("sleepygloaVisionApi")
        val vision = builder.build()

        val batchAnnotateImageRequest = BatchAnnotateImagesRequest()
        batchAnnotateImageRequest.requests = object : ArrayList<AnnotateImageRequest>(){
            init{
                /**********************************************
                 * 전송하기 위한 이미지를 byteArray로 변환
                 * ********************************************/
                val annotateImageRequet = AnnotateImageRequest()

                val base64EncodedImage = com.google.api.services.vision.v1.model.Image()
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()

                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequet.image = base64EncodedImage

                /*********************************************
                 * 결과값 갯수 : 10
                 ********************************************/
                annotateImageRequet.features = object : ArrayList<Feature>(){
                    init{
                        val labelDetection = Feature()

                        when(requestType){
                            activity.LABEL_DETECTION_REQUEST->labelDetection.type="LABEL_DETECTION"
                            activity.LANDMARK_DETECTION_REQUEST->labelDetection.type="LANDMARK_DETECTION"
                        }

                        labelDetection.maxResults = MAX_LAFTEL_RESULTS
                        add(labelDetection)
                        /*
                          TYPE_UNSPECIFIED	Unspecified feature type.
                          FACE_DETECTION	Run face detection.
                          LANDMARK_DETECTION	Run landmark detection.
                          LOGO_DETECTION	Run logo detection.
                          LABEL_DETECTION	Run label detection.
                          TEXT_DETECTION	Run text detection / optical character recognition (OCR). Text detection is optimized for areas of text within a larger image; if the image is a document, use DOCUMENT_TEXT_DETECTION instead.
                          DOCUMENT_TEXT_DETECTION	Run dense text document OCR. Takes precedence when both DOCUMENT_TEXT_DETECTION and TEXT_DETECTION are present.
                          SAFE_SEARCH_DETECTION	Run Safe Search to detect potentially unsafe or undesirable content.
                          IMAGE_PROPERTIES	Compute a set of image properties, such as the image's dominant colors.
                          CROP_HINTS	Run crop hints.
                          WEB_DETECTION	Run web detection.
                          PRODUCT_SEARCH	Run Product Search.
                          OBJECT_LOCALIZATION	Run localizer for object detection.
                        */
                    }
                }
                add(annotateImageRequet)
            }
        }
        val annotateRequest = vision.images().annotate(batchAnnotateImageRequest)
        annotateRequest.setDisableGZipContent(true)
        return annotateRequest
    }


    private fun findPropertResponseType(response: BatchAnnotateImagesResponse):String{
        when (requestType){
            activity.LABEL_DETECTION_REQUEST->{
                return converResponseToString(response.responses[0].labelAnnotations)
            }
            activity.LANDMARK_DETECTION_REQUEST->{
                return converResponseToString(response.responses[0].landmarkAnnotations)
            }
        }

        return "분석 실패"
    }

    private fun converResponseToString(labels: MutableList<com.google.api.services.vision.v1.model.EntityAnnotation>): String{
        val message = StringBuilder("결과 분석/n")
        labels?.let{
            it.forEach{
                message.append(String.format(Locale.US, "%.3F: %s", it.score, it.description))
                message.append("\n")
            }
            return message.toString()
        }
    }
}

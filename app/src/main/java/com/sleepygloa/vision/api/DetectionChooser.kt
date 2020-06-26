package com.sleepygloa.vision.api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.detection_chooser.*

class DetectionChooser : DialogFragment(){

    private var detextionChooserNotifierInterface: DetectionChooserNotifierInterface? = null

    interface DetectionChooserNotifierInterface{
        fun detectLabel()
        fun detectLanmark()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.detection_chooser, container, false)
        //return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun addDetectionChooserNotifierInterface(listener:DetectionChooserNotifierInterface){
        detextionChooserNotifierInterface = listener
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListener()
    }

    private fun setupListener(){
        detect_label.setOnClickListener{
            detextionChooserNotifierInterface?.detectLabel()
            dismiss()
        }
        detect_landmark.setOnClickListener{
            detextionChooserNotifierInterface?.detectLanmark()
            dismiss()
        }
        detect_cancel.setOnClickListener {
            dismiss()
        }
    }
}

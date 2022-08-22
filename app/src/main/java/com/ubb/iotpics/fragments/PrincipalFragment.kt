package com.ubb.iotpics.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cottacush.android.hiddencam.CameraType
import com.cottacush.android.hiddencam.HiddenCam
import com.cottacush.android.hiddencam.OnImageCapturedListener
import com.ubb.iotpics.BitmapToMessageAdapter
import com.ubb.iotpics.R
import com.ubb.iotpics.databinding.PrincipalFragmentBinding
import com.ubb.iotpics.mqtt.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import java.io.File


class PrincipalFragment:Fragment() {
    private var _binding: PrincipalFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var mqttClient: MQTTClient

    private var hiddenCam:HiddenCam? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                disconnect()
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = PrincipalFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = "Take Picture"

        connectToMQTT()



        binding.buttonPublish.setOnClickListener {
            val topic   = binding.edittextPubtopic.text.toString()
            publish(topic, REQUEST_TOPIC)
        }

        binding.buttonSubscribe.setOnClickListener {
            val topic = binding.edittextSubtopic.text.toString()
            subscribe(topic)
        }

        binding.buttonUnsubscribe.setOnClickListener {
            unsubscribe()
        }
        binding.buttonDisconnect.setOnClickListener{
            disconnect()
        }

        binding.buttonPrefillSupervisor.setOnClickListener{
            unsubscribe()
            subscribe(RESPONSE_TOPIC)
            binding.edittextPubtopic.setText(REQUEST_TOPIC)
            binding.edittextSubtopic.setText(RESPONSE_TOPIC)
        }

        binding.buttonPrefillCamera.setOnClickListener {
            camSetup()
            unsubscribe()
            subscribe(REQUEST_TOPIC)
            binding.edittextSubtopic.setText(REQUEST_TOPIC)
        }

    }

    private fun connectToMQTT(){
        var serverURI = arguments?.getString(MQTT_SERVER_URI_KEY)
        val clientId = arguments?.getString(MQTT_CLIENT_ID_KEY)
        val username = arguments?.getString(MQTT_USERNAME_KEY)
        val pwd = arguments?.getString(MQTT_PWD_KEY)

        if(serverURI == null)
            serverURI = SERVER

        if (clientId != null && username != null && pwd != null) {
            // Open MQTT Broker communication
            mqttClient = MQTTClient(context, serverURI, clientId)

            // Connect and login to MQTT Broker
            mqttClient.connect(username,
                pwd,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(this.javaClass.name, "Connection success")
                        Toast.makeText(context, "MQTT Connection success", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.i(this.javaClass.name, "Connection failure: ${exception.toString()}")
                        Toast.makeText(
                            context,
                            "MQTT Connection fails: ${exception.toString()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.action_principalFragment_to_connectFragment)
                    }
                },
                object : MqttCallback {
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val msg = " ${message.toString()}"
                        Log.i("MessageArrived", msg)
                        handleMessage(msg,topic!!)
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.i(this.javaClass.name, "Connection lost ${cause.toString()}")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.i(this.javaClass.name, "Delivery complete")
                    }
                })
        }
    }

    private fun handleMessage(msg: String, topic: String) {
        if(topic == REQUEST_TOPIC){
            takePic()
        }
        else
            if(topic == RESPONSE_TOPIC){
                showDialog(BitmapToMessageAdapter.adaptMessageToBitmap(msg))
            }

    }

    private fun camSetup() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        val directory: File = requireActivity().getDir("imageDir", Context.MODE_PRIVATE)
        Log.e("Storage",directory.absolutePath.toString())
        val captureListener = object: OnImageCapturedListener{
            override fun onImageCaptureError(e: Throwable?) {
                Log.e("ImageCaptureError",e!!.message.toString())
            }

            override fun onImageCaptured(image: File) {
                Log.i("ImageCapture",image.name)
               lifecycleScope.launch{
                   publish(RESPONSE_TOPIC,BitmapToMessageAdapter.adaptBitmapToMessage(BitmapFactory.decodeFile(image.path)))
                   delay(200)
                   hiddenCam!!.stop()
               }
            }

        }
        try{
            hiddenCam = HiddenCam(requireContext(), directory, captureListener, cameraType = CameraType.BACK_CAMERA)
        }
        catch (e:Exception){
            e.printStackTrace()
            Log.e("HiddenCamInitError",e.message.toString())

        }
    }

    private fun takePic(){
        if (allPermissionsGranted()) {
            hiddenCam!!.start()
            hiddenCam!!.captureImage()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

    }

    private fun showDialog(bitmap: Bitmap){


        val inflater:LayoutInflater = activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.image_display,null)
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )
        popupWindow.elevation = 10.0F

        val imageView = view.findViewById<ImageView>(R.id.response_imageView)
        imageView.setImageBitmap(bitmap)
        imageView.rotation = 90.0F;

        val backButton = view.findViewById<Button>(R.id.button_popup)
        backButton.setOnClickListener {
            popupWindow.dismiss()
        }

        TransitionManager.beginDelayedTransition(binding.root as ViewGroup?)
        popupWindow.showAtLocation(
            binding.root as ViewGroup?, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(requireContext(),"Permissions Granted",Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private fun publish(topic:String,message:String) {
        if (mqttClient.isConnected()) {
            mqttClient.publish(topic,
                message,
                1,
                false,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg ="Publish message: $message to topic: $topic"
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(this.javaClass.name, "Failed to publish message to topic")
                    }
                })
        } else {
            Log.d(this.javaClass.name, "Impossible to publish, no server connected")
        }
    }

    private fun subscribe(topic: String){


        if (mqttClient.isConnected()) {
            mqttClient.subscribe(topic,
                1,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Subscribed to: $topic"
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to subscribe: $topic"
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Log.d(this.javaClass.name, "Impossible to subscribe, no server connected")
        }
    }

    private fun unsubscribe(){
        val topic = binding.edittextSubtopic.text.toString()

        if (mqttClient.isConnected()) {
            mqttClient.unsubscribe( topic,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Unsubscribed to: $topic"
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val msg = "Failed to unsubscribe to: $topic"
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Log.d(this.javaClass.name, "Impossible to unsubscribe, no server connected")
        }
    }

    private fun disconnect(){
        if (mqttClient.isConnected()) {
            // Disconnect from MQTT Broker
            mqttClient.disconnect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(this.javaClass.name, "Disconnected")
                    Toast.makeText(
                        context,
                        "MQTT Disconnection success",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Disconnection success, come back to Connect Fragment
                    findNavController().navigate(R.id.action_principalFragment_to_connectFragment)
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken?,
                    exception: Throwable?
                ) {
                    Log.d(this.javaClass.name, "Failed to disconnect")
                }
            })
        } else {
            Log.d(this.javaClass.name, "Impossible to disconnect, no server connected")
        }

    }



    override fun onDestroyView() {
        _binding = null
        if(hiddenCam!=null)
            hiddenCam!!.stop()
        super.onDestroyView()

    }

}
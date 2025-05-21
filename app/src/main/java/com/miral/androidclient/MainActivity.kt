package com.miral.androidclient

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.SurfaceViewRenderer

class MainActivity : AppCompatActivity(), SignalingClient.Listener{
    private lateinit var signalingClient: SignalingClient
    private lateinit var webRTCClient: WebRTCClient
    private lateinit var eglBase: EglBase
    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var btnCall: Button
    private lateinit var btnHangup: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        localView = findViewById(R.id.local_view)
        remoteView = findViewById(R.id.remote_view)
        btnCall    = findViewById(R.id.btn_call)
        btnHangup  = findViewById(R.id.btn_hangup)


        // Hide call button until WebRTC is initialized
        btnCall.visibility = View.GONE

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.values.all { it }) {
                initWebRTC()
            } else finish()
        }.launch(arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        ))
    }

    private fun initWebRTC() {
        eglBase = EglBase.create()
        signalingClient = SignalingClient(this)
        webRTCClient = WebRTCClient(this, eglBase, signalingClient)

        webRTCClient.initPeerConnection(localView, remoteView)
        signalingClient.connect("room1", "ws://192.168.29.220:8080")
        btnCall.visibility = View.VISIBLE
        // Start the call as the initiating user
        btnCall.setOnClickListener {
            btnCall.isEnabled = false
            webRTCClient.startCall()
        }

        btnHangup.setOnClickListener {
            // Tell the other side we’re leaving
            Log.e("TAG", "initWebRTC: Hangup", )
            signalingClient.leave()
            // Tear down local PeerConnection
            webRTCClient.dispose()    // implement this to call pc.close()
            finish()
        }
    }

    override fun onOffer(sdp: String) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Incoming Call")
                .setMessage("User is calling. Accept?")
                .setPositiveButton("Accept") { _, _ ->
                    webRTCClient.handleOffer(sdp)
                    btnHangup.visibility = View.VISIBLE
                    btnCall.visibility = View.GONE
                }
                .setNegativeButton("Decline") { _, _ -> signalingClient.leave(); finish() }
                .setCancelable(false)
                .show()

        }
    }

    override fun onAnswer(sdp: String) {
        webRTCClient.handleAnswer(sdp)
        runOnUiThread {
            btnCall.visibility = View.GONE
            btnHangup.visibility = View.VISIBLE
        }
    }

    override fun onCandidate(candidate: IceCandidate): Boolean = webRTCClient.addIceCandidate(candidate)
    override fun onLeave() = finish()

    override fun onDestroy() {
        super.onDestroy()
        signalingClient.leave()
    }
}
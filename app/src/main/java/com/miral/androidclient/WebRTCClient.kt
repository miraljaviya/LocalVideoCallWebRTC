package com.miral.androidclient

import android.content.Context
import android.util.Log
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import kotlin.math.log

class WebRTCClient(
    private val context: Context,
    private val eglBase: EglBase,
    private val signalingClient: SignalingClient
) {
    private val factory: PeerConnectionFactory
    private lateinit var pc: PeerConnection

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        )
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun initPeerConnection(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer) {

        val iceServers = listOf(
            // 1) Free public STUN for discovery
            PeerConnection.IceServer
                .builder("stun:stun.l.google.com:19302")
                .createIceServer()

            // 2) YOUR TURN server for relay (replace host/user/pass)
//            PeerConnection.IceServer
//                .builder("turn:turn.myserver.com:3478")  // ← your TURN server’s address
//                .setUsername("myTurnUsername")          // ← configured in your TURN server
//                .setPassword("mySecureTurnPassword")    // ← configured in your TURN server
//                .createIceServer()
        )

        // Initialize renderers
        localView.init(eglBase.eglBaseContext, null)
        localView.setMirror(true)
        // Initialize the remote SurfaceViewRenderer
        remoteView.init(eglBase.eglBaseContext, null)
        remoteView.setMirror(false)

        pc = factory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServers).apply {
                // Ensure Unified Plan semantics
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            },
            object : PeerConnectionAdapter() {
                override fun onIceCandidate(candidate: IceCandidate) {
                    Log.e("TAG", "onIceCandidate: $candidate", )
                    signalingClient.sendCandidate(candidate)
                }

                override fun onTrack(transceiver: RtpTransceiver) {
                    (transceiver.receiver.track() as? VideoTrack)?.addSink(remoteView)
                }

                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                    super.onAddTrack(receiver, streams)
                    val track = receiver.track()
                    if (track is VideoTrack) {
                        track.addSink(remoteView)
                    }
                }
            }
        )!!

        // 3) Prepare local video track
        val capturer = Camera2Enumerator(context)
            .deviceNames
            .first { Camera2Enumerator(context).isFrontFacing(it) }
            .let { Camera2Enumerator(context).createCapturer(it, null)!! }
        val helper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val videoSource = factory.createVideoSource(false)
        capturer.initialize(helper, context, videoSource.capturerObserver)
        capturer.startCapture(640, 480, 30)

        val localVideoTrack = factory.createVideoTrack("VID_TRACK", videoSource)
        localVideoTrack.setEnabled(true)
        localVideoTrack.addSink(localView)

        // 5) Prepare local audio track
        val audioSource = factory.createAudioSource(MediaConstraints())
        val localAudioTrack = factory.createAudioTrack("AUD_TRACK", audioSource)
        localAudioTrack.setEnabled(true)

        pc.addTrack(localVideoTrack,  listOf("stream1"))
        pc.addTrack(localAudioTrack,  listOf("stream1"))

    }

    fun startCall() {
        val offerConstraints = MediaConstraints().apply {
                 mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                 mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
               }
        pc.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription) {
                Log.e("startCall", "onCreateSuccess: $desc", )
                pc.setLocalDescription(SdpObserverAdapter(), desc)
                signalingClient.sendOffer(desc.description)
            }

            override fun onCreateFailure(p0: String) {
                Log.e("startCall", "onCreateFailure: $p0", )
                super.onCreateFailure(p0)
            }
        }, offerConstraints)
    }

    fun handleOffer(offer: String) {
        val sessionDesc = SessionDescription(SessionDescription.Type.OFFER, offer)
        val answerConstraints = MediaConstraints().apply {
                 mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                 mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
               }
        pc.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                pc.createAnswer(object : SdpObserverAdapter() {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        pc.setLocalDescription(SdpObserverAdapter(), desc)
                        signalingClient.sendAnswer(desc.description)
                    }
                    override fun onCreateFailure(error: String) {
                        Log.e("WebRTCClient", "Failed to create answer: $error")
                    }
                }, answerConstraints)
            }
            override fun onSetFailure(error: String) {
                Log.e("WebRTCClient", "Failed to set remote description: $error")
            }
        }, sessionDesc)
    }

    fun handleAnswer(answer: String) {
        pc.setRemoteDescription(object : SdpObserverAdapter(){
            override fun onSetSuccess() {
                Log.d("WEBRTC","Caller got ANSWER, applied…")
            }
            override fun onSetFailure(p0: String) {
                Log.e("WebRTCClient", "Failed to set remote description: $p0")
                super.onSetFailure(p0)
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, answer))
    }

    fun addIceCandidate(c: IceCandidate) = pc.addIceCandidate(c)

    fun dispose() {
        if (::pc.isInitialized) {
            pc.close()
        }
    }
}
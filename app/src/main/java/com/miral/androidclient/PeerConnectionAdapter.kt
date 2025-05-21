package com.miral.androidclient

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver

open class PeerConnectionAdapter : PeerConnection.Observer {
    override fun onIceCandidate(candidate: IceCandidate) {}
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
    override fun onDataChannel(dc: DataChannel) {}
    override fun onIceConnectionReceivingChange(p0: Boolean) {}
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
    override fun onAddStream(p0: MediaStream) {}
    override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
    override fun onRemoveStream(p0: MediaStream) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
    override fun onTrack(transceiver: RtpTransceiver) {}
}
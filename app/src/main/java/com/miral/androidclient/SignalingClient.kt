package com.miral.androidclient

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.IceCandidate


class SignalingClient(private val listener: Listener) {
    interface Listener {
        fun onOffer(sdp: String)
        fun onAnswer(sdp: String)
        fun onCandidate(candidate: IceCandidate): Boolean
        fun onLeave()
    }

    private val client = OkHttpClient()
    private lateinit var ws: WebSocket
    private var roomId: String = ""

    fun connect(room: String, serverUrl: String) {
        roomId = room
        ws = client.newWebSocket(Request.Builder().url(serverUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    sendMessage("join", roomId)
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val json = JSONObject(text)
                    when (val type = json.getString("type")) {
                        "offer" -> listener.onOffer(json.getString("payload"))
                        "answer" -> listener.onAnswer(json.getString("payload"))
                        "candidate" -> run {
                            val data = json.getJSONObject("payload")
                            listener.onCandidate(IceCandidate(
                                data.getString("sdpMid"),
                                data.getInt("sdpMLineIndex"),
                                data.getString("candidate")
                            ))
                        }
                        "leave" -> listener.onLeave()
                    }
                }
            })
    }

    private fun sendMessage(type: String, payload: Any) {
        val message = JSONObject().apply {
            put("type", type)
            put("room", roomId)
            when (payload) {
                is String -> put("payload", payload)
                is JSONObject -> put("payload", payload)
                else -> put("payload", payload.toString())
            }
        }
        ws.send(message.toString())
    }

    fun sendOffer(sdp: String) = sendMessage("offer", sdp)
    fun sendAnswer(sdp: String) {
        // Log answer being sent
        sendMessage("answer", sdp)
    }
    fun sendCandidate(c: IceCandidate) {
        val data = JSONObject().apply {
            put("candidate", c.sdp)
            put("sdpMid", c.sdpMid)
            put("sdpMLineIndex", c.sdpMLineIndex)
        }
        sendMessage("candidate", data)
    }

    fun leave() = sendMessage("leave", "")
}
# Android WebRTC Video Call App

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> A peer-to-peer video & audio calling Android app built in Kotlin using the open-source WebRTC SDK.  
> No third-party media servers—just WebRTC + a custom signaling server.


## About

This Android application demonstrates a complete WebRTC peer-to-peer video & audio call using the official `io.github.webrtc-sdk:android` library. A minimal Node.js signaling server (WebRTCLocalServer) is used to exchange SDP offers/answers and ICE candidates over WebSocket.

## Features

- 🎥 Two-way video & audio  
- 🔄 Call, accept/decline, hang up  
- 🔒 PeerConnection with Unified Plan semantics  
- 🔧 Uses only open-source WebRTC (no commercial SDKs)  
- 🔗 Custom WebSocket signaling

## Demo

![Local & Remote Video](docs/screenshot.png)

## Getting Started

### Prerequisites

- Android Studio 
- Android device or emulator (API 21+)  
- A running signaling server (see below)

## Setup & Run

1. **Signaling server**  
   Before running the Android app, you need a signaling server.  
   Check out and start the server here:  
   👉 [WebRTC Local Server](https://github.com/miraljaviya/WebRTCLocalServer)  
   Follow its **README** to install and run on `ws://YOUR_SERVER_IP:8080`.  

2. **Android client**  
   - Open this project in Android Studio and let it sync Gradle.  
   - Build & install on two devices or emulators.  
   - Grant camera & microphone permissions when prompted.  
   - Tap **Call** on Device A, accept on Device B, and hang up when done.

## Configuration

You must point the app to your signaling server’s WebSocket URL. In **`MainActivity.kt`**, update the `connect` call:

```kotlin
// MainActivity.kt, inside initWebRTC()
signalingClient.connect(
    "room1",
    "ws://YOUR_SERVER_IP:8080" // ← replace with your actual server IP or domain
)

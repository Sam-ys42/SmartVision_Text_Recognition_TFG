# SmartVision_Text_Recognition_TFG
Reading assistance system for people with visual disabilities using smart glasses with ESP32-S3, Android app, and OCR API with local AI.

SmartVision is a reading assistance system developed as a Final Cycle Project for the Higher Degree in Cross-Platform Application Development (DAM) at IES Augustóbriga (Navalmoral de la Mata, Cáceres, Spain). The project was born with the goal of providing people with partial or total visual impairment with a portable, affordable and easy-to-use tool that allows them to read the textual content of any printed surface in their daily environment: signs, medication packaging, menus, transport tickets or any physical document.
The system integrates three components that work in a coordinated way. The first is a hardware device consisting of an ESP32-S3 board with an integrated OV5640 camera, mounted on a 3D-printed glasses frame and powered by a USB-C rechargeable LiPo battery. The second is an Android mobile application that acts as the control centre: it receives the image captured by the glasses via Bluetooth Low Energy, sends it to the API for processing and reads the resulting text aloud using Android's native TTS engine. The third is a REST API developed in Python with FastAPI that receives the images, processes them with the GLM-OCR artificial intelligence model running locally through Ollama, stores the images in Amazon S3 and logs each user's operation history in Amazon DynamoDB.
The complete system flow, from the moment the user presses the physical button on the glasses to hearing the text read aloud, has a response time of between 3 and 8 seconds depending on the length of the captured text.

# Repository structure
```
SmartVision/
│
├── OCR_Python_SmartVision/     -> REST API 
├── Programa_Placa_SmartVision/ -> ESP32-S3 firmware 
└── SmartVision/                -> Android app 
```

# Technologies used

Hardware

ESP32-S3 CAM (dual-core Xtensa LX7 microcontroller at 240 MHz, 16 MB Flash, 8 MB OPI PSRAM)
OV5640 camera module (up to 5 MP, configured in OCR-optimised JPEG resolution)
1000 mAh LiPo battery with USB-C charging and DC-DC boost converter to stable 5V
Glasses frame designed and 3D-printed to integrate all components

Firmware

C++ on Arduino IDE 2.x with the esp32 by Espressif Systems package v3.x
Bluetooth Low Energy (BLE) using the ESP32-S3 native stack
Custom fragmentation protocol with START/END signals and 500-byte blocks
50 ms debounce filter for the physical capture button

Android Application

Java · Android Studio · minimum API 24 (Android 7.0), compiled for API 36 (Android 16)
Bluetooth Low Energy with 512-byte MTU negotiation
Retrofit + OkHttp for REST API communication (60 s timeouts)
SQLite for local user data storage
SharedPreferences for session and voice configuration persistence
Android native TextToSpeech (TTS) configured in Spanish with three speed levels

REST API

Python 3.10+ · FastAPI · Uvicorn
Ollama as local inference engine with the GLM-OCR model (1.11B parameters, 2.2 GB)
Endpoints: POST /registro · GET /obtenerUsuario/{usuario} · POST /obtenerTexto
60-second timeouts for OCR operations

AWS Cloud

Amazon S3 for image storage with path user/user_timestamp.jpg
Amazon DynamoDB (USUARIOS and historialUsuarios tables) for user management and OCR history
AWS SDK for Python (boto3)

# Requirements to run the project

Arduino IDE 2.x with the esp32 by Espressif Systems package v3.x or higher
Python 3.10 or higher and Ollama installed on the server
Android device running version 7.0 or higher with BLE Bluetooth
AWS account with access to S3 and DynamoDB and configured IAM credentials
Android device and server on the same local network

# Author and supervisors
Author: Yelena Samira de la Cruz Menacho
Supervisors: Soraya Blázquez Moreno · Santiago Monroy Cabrero
School: IES Augustóbriga · Navalmoral de la Mata, Cáceres, Spain
Degree: Cross-Platform Application Development (DAM) · May 2026

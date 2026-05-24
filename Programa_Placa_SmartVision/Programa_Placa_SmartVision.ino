#include "esp_camera.h"   // Librería oficial de Espressif para inicializar y capturar fotos con el módulo de la cámara
#include <BLEDevice.h>    // Librería base de BLE para activar el chip Bluetooth y configurar las propiedades del dispositivo
#include <BLEServer.h>    // Permite configurar la placa como un servidor que espera conexiones entrantes 
#include <BLEUtils.h>     // Contiene herramientas de utilidad para formatear datos y gestionar los identificadores (UUID)
#include <BLE2902.h>      // Descriptor necesario para habilitar el envío automático de datos mediante notificaciones BLE

// CONFIGURACIÓN BLE 
// -----------------------------------------------------------------------
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914c" // ID del servicio generado por IA para que el movil reconozca mi placa
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a9" // ID de la caracteristica para que por este canal viaje la foto

BLECharacteristic *pCharacteristic; // Variable para manejar el canal de envio de datos
bool deviceConnected = false; // booleable para saber si el movil esta conectado o no

// Clase para controlar los eventos de conexion y desconexion
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true; // El movil se ha conectado, ya podemos enviar datos
      Serial.println("Movil conectado");
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false; // Se ha perdido la conexion con el movil
      Serial.println("movil desconectado");
      delay(500); 
      BLEDevice::startAdvertising(); // Volvemos a activar el Bluetooth para que el movil nos encuentre otra vez
    }
};

// Pin boton
#define BUTTON_PIN 14 

// Configuración de los pines de la cámara
#define PWDN_GPIO_NUM -1
#define RESET_GPIO_NUM -1
#define XCLK_GPIO_NUM 15
#define SIOD_GPIO_NUM 4
#define SIOC_GPIO_NUM 5
#define Y2_GPIO_NUM 11
#define Y3_GPIO_NUM 9
#define Y4_GPIO_NUM 8
#define Y5_GPIO_NUM 10
#define Y6_GPIO_NUM 12
#define Y7_GPIO_NUM 18
#define Y8_GPIO_NUM 17
#define Y9_GPIO_NUM 16
#define VSYNC_GPIO_NUM 6
#define HREF_GPIO_NUM 7
#define PCLK_GPIO_NUM 13

//funciones de la cámara-------------------------------------------------

bool iniciarCamara() {
  camera_config_t config;

  // configuramos los pines de la cámara, la frecuencia, formato y resolución
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM; config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM; config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM; config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM; config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM; 
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM; config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM; config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM; config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG; // Usamos JPEG porque es más ligero para enviar
  
  // Resolución VGA (640x480) para que el OCR lea bien el texto sin saturar el canal
  config.frame_size = FRAMESIZE_VGA; 
  
  config.jpeg_quality = 10; // Calidad alta para no perder nitidez en las letras
  config.fb_count = 1;
  config.fb_location = CAMERA_FB_IN_DRAM;
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  return (esp_camera_init(&config) == ESP_OK); // Si devuelve OK es que la cámara está lista
}

//enviar foto por BLE--------------------------------------------------------------
void enviarFotoPorBLE(camera_fb_t * fb) {
  if (!deviceConnected || !fb) return; // Si no hay conexión o no hay foto, no hacemos nada.

  Serial.println("Iniciando transferencia BLE");
  
  pCharacteristic->setValue("START"); // Avisamos a la App de que empieza una nueva imagen
  pCharacteristic->notify();
  delay(150); // Pequeña pausa para que la App se prepare

  size_t totalLen = fb->len; // tamaño total de bytes de la imagen
  size_t bufferSize = 500; // Enviamos en trozos de 500 bytes para no saturar el canal
  uint8_t *fbBuf = fb->buf; // puntero al primer byte de datos de la imagen

  for (size_t n = 0; n < totalLen; n += bufferSize) {
    size_t chunkLen = (n + bufferSize < totalLen) ? bufferSize : (totalLen - n);
    pCharacteristic->setValue(&fbBuf[n], chunkLen); // Cargamos el trozo de la foto
    pCharacteristic->notify(); // Enviamos el trozo
    delay(25); // (milisegundos) Esperamos un poco para que la conexión sea estable y no se pierdan datos
  }

  delay(150); // espera 150 milisegundos
  pCharacteristic->setValue("END"); // Avisamos a la App de que ya se ha enviado todo
  pCharacteristic->notify();
  Serial.printf("Envio finalizado. Total: %d bytes\n", totalLen);
}

// tomar foto y
bool tomarFoto() {
  // Limpiamos el buffer por si hay alguna imagen vieja retenida
  camera_fb_t *temp_fb = esp_camera_fb_get();
  if (temp_fb) {
    esp_camera_fb_return(temp_fb);
  }

  // Hacemos la captura nueva
  camera_fb_t *fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Error capturando foto");
    return false;
  }


  // Llamamos a la función para mandar los datos al móvil
  enviarFotoPorBLE(fb);
  esp_camera_fb_return(fb); // Liberamos la memoria de la foto para poder hacer la siguiente
  return true;
}

//se ejecuta 1 vez---------------------------------------------------
void setup() {
  Serial.begin(115200); // abre el puerto al 115200
  delay(1000); // Espera de cortesía para que el sistema arranque bien

  pinMode(BUTTON_PIN, INPUT_PULLUP); // Configuramos el botón con resistencia interna

  // Configuración del nombre del Bluetooth
  BLEDevice::init("ESP32-S3-OCR");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_NOTIFY 
                     );

  pCharacteristic->addDescriptor(new BLE2902()); // Necesario para que las notificaciones funcionen
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  BLEDevice::startAdvertising();
  
  if(!iniciarCamara()){
      Serial.println("Error en Cámara");
  }
  
  Serial.println("Listo");
}

//bucle que se ejecuta siempre------------------------------------------------------
void loop() {
  // Comprobamos si se ha pulsado el botón (lógica negativa)
  if (digitalRead(BUTTON_PIN) == LOW) {
    delay(50); // Filtro para evitar rebotes eléctricos
    if (digitalRead(BUTTON_PIN) == LOW) {
      Serial.println("Boton pulsado");

      tomarFoto();

      // Mientras el botón esté pulsado, nos quedamos aquí para no repetir la foto
      while (digitalRead(BUTTON_PIN) == LOW) {
        delay(10);
      }
      delay(500); // Pausa de medio segundo antes de dejar pulsar otra vez
    }
  }
  delay(20); // Pequeño respiro para el procesador
}
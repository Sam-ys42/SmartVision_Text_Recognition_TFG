package com.example.smartvision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914c");
    private static final UUID CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9");
    private static final UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private TextView tvStatus, tvResultado;
    private ImageView tvImagen;
    private Button btnConnect, btnBack;

    private TextToSpeech tts;
    private ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();

    private Retrofit retrofit;
    private APISmartVision api;
    private String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvResultado = (TextView) findViewById(R.id.tvResultado);
        tvImagen = (ImageView) findViewById(R.id.ivPhoto);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnBack = (Button) findViewById(R.id.btnBack);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.40:8000/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(APISmartVision.class);


        final SharedPreferences prefs = getSharedPreferences("SmartVisionPrefs", MODE_PRIVATE);
        usuarioActual = prefs.getString("user_id", "desconocido");

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(new Locale("es", "ES"));
                    hablar("Aplicación lista");
                }
            }
        });

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (manager != null && manager.getAdapter() != null) {
            scanner = manager.getAdapter().getBluetoothLeScanner(); // BluetoothLeScanner
        }

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                revisarPermisos();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                liberarRecursos();
                finish();
            }
        });
    }

    private void hablar(String t) {
        if (tts != null) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void revisarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        hablar("Buscando dispositivo");
        comenzarEscaner();
    }

    @SuppressLint("MissingPermission")
    private void comenzarEscaner() {
        if (scanner == null) return;
        ScanFilter filter = new ScanFilter.Builder().setDeviceName("ESP32-S3-OCR").build(); // nombre de las gafas
        scanner.startScan(Collections.singletonList(filter), new ScanSettings.Builder().build(), scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onScanResult(int callbackType, ScanResult result) {
            scanner.stopScan(this); // el escaneo para cuando se detecta el dispositivo
            gatt = result.getDevice().connectGatt(MainActivity.this, false, gattCallback);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("MissingPermission")
        // Cambio de estado de conexión (conectado o desconectado)
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(new Runnable() { public void run() {
                    tvStatus.setText("Estado: CONECTADO");
                    hablar("Dispositivo conectado");
                } });
                g.requestMtu(512); // Negociar MTU máximo para fragmentos de 500B
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() { public void run() {
                    tvStatus.setText("Estado: Desconectado");
                } });
            }
        }

        @Override
        @SuppressLint("MissingPermission") // esta línea de código requiere permisos de Bluetooth, lo ponemos para ignorar llos errores que nos puede dar por el uso de bluetooth
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            g.discoverServices();
        }

        @Override
        @SuppressLint("MissingPermission")
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            BluetoothGattService s = g.getService(SERVICE_UUID);
            if (s != null) {
                BluetoothGattCharacteristic c = s.getCharacteristic(CHAR_UUID);
                g.setCharacteristicNotification(c, true);
                BluetoothGattDescriptor d = c.getDescriptor(CLIENT_CONFIG);
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(d);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            byte[] d = c.getValue();
            String s = new String(d);
            if (s.startsWith("START")) {
                imageBuffer.reset();
            } else if (s.equals("END")) {
                procesarImagen();
            } else {
                imageBuffer.write(d, 0, d.length);
            }
        }
    };

    private void procesarImagen() {
        byte[] b = imageBuffer.toByteArray(); // convertimos los valores dentro del buffer en un array de bytes
        Bitmap bit = BitmapFactory.decodeByteArray(b, 0, b.length); // pasamos de array a BitMap
        if (bit != null) {
            Matrix m = new Matrix();
             m.postScale(-1, 1, bit.getWidth()/2f, bit.getHeight()/2f); // con esto arreglamos el problema "espejo" que teníamos antes
            final Bitmap f = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(), bit.getHeight(), m, true);
            runOnUiThread(new Runnable() {
                public void run() {
                    tvImagen.setImageBitmap(f); //mostramos la imagen codificada en el objeto tvImagen
                } });

            ByteArrayOutputStream o = new ByteArrayOutputStream();
            f.compress(Bitmap.CompressFormat.JPEG, 90, o); // el bitmap lo comprimimos en jpeg para mandárselo por POST
            subirServidor(o.toByteArray());
        }
    }

    private void subirServidor(byte[] bytes) {
        RequestBody u = RequestBody.create(usuarioActual, MediaType.parse("text/plain")); // cuerpo de tipo texto
        RequestBody f = RequestBody.create(bytes, MediaType.parse("image/jpeg")); // em cuerpo es de tipo imagen jpeg
        MultipartBody.Part p = MultipartBody.Part.createFormData("file", "capture.jpg", f); // metemos la imagen dentro del cuerpo de texto

        //empieza la llamada asíncrona ----------------------------------------------
        api.procesarVision(u, p).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) { // Verificamos que el código de estado HTTP no sea un error y el cuerpo no sea nulo
                    Usuario res = response.body(); // guardamos la respuesta en un objeto de tipo usuario
                    if ("ok".equals(res.status)) {
                        final String textoIA = res.texto;
                        runOnUiThread(new Runnable() { //actualizamos la imagen y  el motor de tts
                            @Override
                            public void run() {
                                tvResultado.setText(textoIA);
                                hablar(textoIA);
                            }
                        });
                    } else { // en caso de que sea un error, se muestra el error
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvResultado.setText("Error DB: " + res.mensajeError);
                            }
                        }); // salida a los errores que nos diga el servidor
                    }
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable t) { // si falla algo, la aplicación nos dirá que ha habido un error
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvResultado.setText("Error, algo salió mal");
                        hablar("error, algo salió mal");
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop(); // paramos el motor de voz
            tts.shutdown(); // liberar los recursos de tts
        }
        liberarRecursos();


    }


    @SuppressLint("MissingPermission")
    private void liberarRecursos() {
        if (gatt != null) {
            gatt.disconnect(); // desconecta las gafas
            gatt.close(); // libera los recursos del sistema del bluetooth
            gatt = null; // se anula para su posterior limpieza
        }
    }
}
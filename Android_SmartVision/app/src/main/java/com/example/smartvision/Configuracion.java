package com.example.smartvision;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class Configuracion extends AppCompatActivity {

    private RadioGroup rgVelocidad;
    private Button btnGuardar;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        rgVelocidad = (RadioGroup) findViewById(R.id.rgVelocidad);
        btnGuardar = (Button) findViewById(R.id.btnGuardarConfig);

        // Configuro el motor de voz para que hable en español al entrar
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    tts.setLanguage(new Locale("es", "ES"));
                    aplicarVelocidadYHablar("Configuración");
                }
            }
        });
        cargarConfiguracionPrevia();
        // Si el usuario cambia el radio button, hago una prueba de voz para que vea como suena
        rgVelocidad.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                float velocidadVoz = obtenerVelocidadSeleccionada();
                if(tts != null){
                    tts.setSpeechRate(velocidadVoz);
                    hablar("Esta es una prueba de velocidad de voz.");
                }
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarConfiguracion();
            }
        });
    }

    // Traduzco lo que está marcado en el RadioGroup a números que entiende el TTS
    private float obtenerVelocidadSeleccionada() {
        int seleccionado = rgVelocidad.getCheckedRadioButtonId();
        if(seleccionado == R.id.rbLento){
            return 0.5f;
        } else if(seleccionado == R.id.rbRapido){
            return 1.5f;
        }
        return 1.0f; // Por defecto la velocidad normal
    }

    private void aplicarVelocidadYHablar(String texto) {
        float velocidadVoz = obtenerVelocidadSeleccionada();
        if(tts != null){
            tts.setSpeechRate(velocidadVoz);
            hablar(texto);
        }
    }

    private void hablar(String texto) {
        if(tts != null){
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // Saco los datos de SharedPreferences para que los botones aparezcan como los dejó el usuario
    private void cargarConfiguracionPrevia() {
        SharedPreferences prefs = getSharedPreferences("SmartVisionPrefs", MODE_PRIVATE);
        String velocidad = prefs.getString("velocidad", "Medio");

        if(velocidad.equals("Lento")){
            rgVelocidad.check(R.id.rbLento);
        } else if(velocidad.equals("Rápido")){
            rgVelocidad.check(R.id.rbRapido);
        }
    }

    // guardo la elección en el XML de preferencias y cierro la activity
    private void guardarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("SmartVisionPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int idVelocidad = rgVelocidad.getCheckedRadioButtonId();
        String velocidad = "Medio";

        if(idVelocidad == R.id.rbLento){
            velocidad = "Lento";
        } else if(idVelocidad == R.id.rbRapido){
            velocidad = "Rápido";
        }

        editor.putString("velocidad", velocidad);
        editor.apply();

        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        //terminamos la voz para que no gaste recursos
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
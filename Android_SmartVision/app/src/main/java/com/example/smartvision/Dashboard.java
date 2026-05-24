package com.example.smartvision;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

public class Dashboard extends AppCompatActivity {

    private TextToSpeech tts;
    private Button botonScan;
    private Button botonAjustes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        // Casting a Button normal
        botonScan = (Button) findViewById(R.id.btnScan);
        botonAjustes = (Button) findViewById(R.id.btnSettings);


        // Botón grande para ir al main y conectarse con las gafas
        botonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dashboard.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Botón fino para ir a configuración
        botonAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dashboard.this, Configuracion.class);
                startActivity(intent);
            }
        });

        // Inicializamos el motor de voz
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(new Locale("es", "ES"));
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.salir) {
            AlertDialog.Builder dialogo1 = new AlertDialog.Builder(this);
            dialogo1.setTitle("Mensaje de Confirmación");
            dialogo1.setMessage("¿Seguro que desea salir de la aplicación?");
            hablar("¿Seguro que desea salir de la aplicación?");
            dialogo1.setCancelable(false);

            dialogo1.setPositiveButton("Salir", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Borrado de datos locales
                    getApplicationContext().deleteDatabase("SmartVisionDBLocal");

                    SharedPreferences prefs = getSharedPreferences("SmartVisionPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();

                    finishAffinity();
                }
            });

            dialogo1.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // No hace nada, cierra el cuadro de diálogo
                }
            });
            dialogo1.show();
        }
        return true;
    }

    private void hablar(String texto) {
        if (tts != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
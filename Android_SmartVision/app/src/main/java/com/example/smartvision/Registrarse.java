package com.example.smartvision;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.regex.Pattern;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Registrarse extends AppCompatActivity {

    private EditText etNombreUsuario, etNombre, etApellido, etEmail, etContrasena;
    private Button btnRegistro, btnVolver;
    private Retrofit retrofit;
    private APISmartVision api;

    private AccesoDatos accesoDatos;

    // Regla para la contraseña: minimo 8 caracteres, letras y numeros
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        // Configuro Retrofit para conectar con el servidor
        retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.40:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(APISmartVision.class);

        accesoDatos = new AccesoDatos(this);

        etNombreUsuario = (EditText) findViewById(R.id.usuarioNombreUsuarioRegistro);
        etNombre = (EditText) findViewById(R.id.usuarioNombreRegistro);
        etApellido = (EditText) findViewById(R.id.usuarioApellidoRegistro);
        etEmail = (EditText) findViewById(R.id.usuarioEmailRegistro);
        etContrasena = (EditText) findViewById(R.id.usuarioContrasenaRegistro);
        btnRegistro = (Button) findViewById(R.id.btnRegistro);
        btnVolver = (Button) findViewById(R.id.btnVolver);

        btnRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                registrarEnServidor();
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Regresa al Login
                startActivity(new Intent(Registrarse.this, Login.class));
                finish();
            }
        });
    }

    private void registrarEnServidor() {
        //recoge los valores de los edittext
        String usuario = etNombreUsuario.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();

        // Miro que no haya ningun campo vacio
        if (TextUtils.isEmpty(usuario) || TextUtils.isEmpty(nombre) ||
                TextUtils.isEmpty(apellido) || TextUtils.isEmpty(email) || TextUtils.isEmpty(contrasena)) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        // Compruebo que el email tenga un formato valido
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email no valido");
            return;
        }
        // Valido que la contraseña sea segura (8 caracteres, letra, numero y simbolo)
        if (!PASSWORD_PATTERN.matcher(contrasena).matches()) {
            etContrasena.setError("Minimo 8 caracteres con letra y numero");
            Toast.makeText(this, "La contraseña es muy floja", Toast.LENGTH_LONG).show();
            return;
        }

        // Si esta toodo bien, llamamoa a la API
        Call<Usuario> call = api.registrar(usuario, nombre, apellido, email, contrasena);
        call.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        if ("ok".equals(response.body().status)) {
                            // Se añade el usuario al insertar localmente
                            accesoDatos.insertar(usuario, nombre, apellido, email, contrasena);
                            Toast.makeText(Registrarse.this, "Usuario registrado", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Registrarse.this, Login.class));
                            finish();
                        } else {
                            Toast.makeText(Registrarse.this, "Error: " + response.body().mensajeError, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Toast.makeText(Registrarse.this, "Error de red al registrar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
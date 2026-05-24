package com.example.smartvision;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Login extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private AccesoDatos accesoDatos;
    private SharedPreferences sharedPreferences;
    private Retrofit retrofit;
    private APISmartVision api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Control de sesión previa
        sharedPreferences = getSharedPreferences("SmartVisionPrefs", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("is_logged_in", false)) {
            iniciarDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.40:8000/") //ip del ordenador servidor según la wifi a la que esté conectada
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(APISmartVision.class);

        accesoDatos = new AccesoDatos(this);
        etUsuario = (EditText) findViewById(R.id.etUsuario);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        tvRegister = (TextView) findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsuario.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                if(!user.isEmpty() && !pass.isEmpty()) { // si no están rellenos los campos, no se puede continuar
                    consultarUsuario(user, pass);
                } else {
                    Toast.makeText(Login.this, "Completa los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() { // si no tenemos una cuenta a la cual loguear, nos creamos una
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, Registrarse.class));
                finish();
            }
        });
    }

    private void consultarUsuario(String nombreUsuario, String pass) {
        Call<Usuario> call = api.getUsuario(nombreUsuario);
        call.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Usuario res = response.body();
                    if ("ok".equals(res.status)) {
                        if (res.contrasena.equals(pass)) {
                            accesoDatos.insertar(res.usuario, res.nombre, res.apellido, res.email, res.contrasena);
                            guardarYAcceder(res.usuario);
                        } else {
                            Toast.makeText(Login.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Aquí usamos el message que viene del server
                        Toast.makeText(Login.this, res.mensajeError, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Toast.makeText(Login.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarYAcceder(String nombreUsuario) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("user_id", nombreUsuario);
        editor.apply(); // guardamos preferencias sin que se quede congelada la interfaz
        iniciarDashboard();
    }

    private void iniciarDashboard() {
        startActivity(new Intent(Login.this, Dashboard.class));
        finish();
    }
}
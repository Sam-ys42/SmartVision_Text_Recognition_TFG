package com.example.smartvision;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;
import androidx.annotation.Nullable;

public class BaseDatos extends SQLiteOpenHelper {
    Context contexto;

    public BaseDatos(@Nullable Context context) {
        super(context, "SmartVisionDBLocal", null, 1);
        contexto = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Crea la tabla con todos los campos del usuario incluyendo el usuario
            db.execSQL("CREATE TABLE usuarios(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "usuario VARCHAR, " +
                    "nombre VARCHAR, " +
                    "apellido VARCHAR, " +
                    "email VARCHAR UNIQUE, " +
                    "contrasena VARCHAR)");
        } catch (Exception e) {
            Toast.makeText(contexto, "Error al crear la base de datos: " + e, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS usuarios");
            onCreate(db);
        } catch (Exception e) {
            Toast.makeText(contexto, "Error al actualizar la base de datos: " + e, Toast.LENGTH_SHORT).show();
        }
    }
}
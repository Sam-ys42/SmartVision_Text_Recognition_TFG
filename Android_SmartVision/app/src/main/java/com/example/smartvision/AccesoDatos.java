package com.example.smartvision;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class AccesoDatos {
    private Context contexto;
    BaseDatos miBD;

    public AccesoDatos(Context contexto) {   // constructor de la clase AccesoDatos, teniendo de base de datos al que hemos creado con anterioridad
        this.contexto = contexto;
        miBD = new BaseDatos(contexto);
    }

    public long insertar(String usuario, String nombre, String apellido, String email, String contrasena) {    // inserta los valores en la SQLite
        SQLiteDatabase accesoEscritura = miBD.getWritableDatabase();
        ContentValues registro = new ContentValues();
        registro.put("usuario", usuario);
        registro.put("nombre", nombre);
        registro.put("apellido", apellido);
        registro.put("email", email);
        registro.put("contrasena", contrasena);

        long resultado = accesoEscritura.insert("usuarios", null, registro);
        return resultado;
    }

}
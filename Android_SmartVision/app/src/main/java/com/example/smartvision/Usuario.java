package com.example.smartvision;

public class Usuario {
    // Claves del JSON devuelto por FastAPI
    public String status;

    public String usuario; // Nombre de usuario que devuelve el servidor
    public String nombre;
    public String apellido;
    public String email;
    public String contrasena; // Coincide con el campo del servidor
    public String texto;    // Resultado del procesamiento OCR
    public String mensajeError;
}
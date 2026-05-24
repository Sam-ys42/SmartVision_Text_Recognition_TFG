import datetime # uso de fechas
import requests # para peticiones de http
import base64 #pasar a base64 las imágenes
import boto3 #interación con S3
import os
from fastapi import FastAPI, Form, File, UploadFile #API

app = FastAPI()

# CONFIGURACION AWS (S3 Y DYNAMODB)
# -----------------------------------------------------------------------


AWS_ACCESS_KEY = os.environ.get("AWS_ACCESS_KEY_ID")
AWS_SECRET_KEY = os.environ.get("AWS_SECRET_ACCESS_KEY")
AWS_SESSION_TOKEN = os.environ.get("AWS_SESSION_TOKEN")
AWS_REGION = "us-east-1"
S3_BUCKET_NAME = "ysdelacruz.bucket.smartvision"

# Inicializamos el cliente de s3 y el recurso de dynamodb
s3Cliente = boto3.client(
    's3',
    aws_access_key_id=AWS_ACCESS_KEY,
    aws_secret_access_key=AWS_SECRET_KEY,
    aws_session_token=AWS_SESSION_TOKEN,
    region_name=AWS_REGION
)

dynamoRecurso = boto3.resource(
    'dynamodb',
    aws_access_key_id=AWS_ACCESS_KEY,
    aws_secret_access_key=AWS_SECRET_KEY,
    aws_session_token=AWS_SESSION_TOKEN,
    region_name=AWS_REGION
)
# Conectamos con las tablas específicas creadas en DynamoDB
tablaHistorial = dynamoRecurso.Table('historialUsuarios')
tablaUsuarios = dynamoRecurso.Table('usuarios')


# FUNCIONES DE APOYO
# -----------------------------------------------------------------------

def guardar_imagen_s3(usuario, image_bytes):
    try:
        timestamp_str = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
        nombreArchivo = f"{usuario}/{usuario}_{timestamp_str}.jpg"

        s3Cliente.put_object(
            Bucket=S3_BUCKET_NAME,
            Key=nombreArchivo,
            Body=image_bytes,
            ContentType="image/jpeg"
        )
        return f"https://{S3_BUCKET_NAME}.s3.{AWS_REGION}.amazonaws.com/{nombreArchivo}"
    except Exception as e:
        print(f"Error S3: {e}")
        return ""


def guardar_historial_db(usuario, texto, urlFoto):
    #Guarda el resultado del OCR en la tabla "historialUsuarios"
    try:
        tablaHistorial.put_item(
            Item={
                'fecha': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'usuario': usuario,
                'texto': texto,
                'urlFoto': urlFoto
            }
        )
        return True
    except Exception as e:
        print(f"Error DynamoDB Historial: {e}")
        return False


# RUTAS DE LA API
# -----------------------------------------------------------------------

@app.post("/obtenerTexto")
async def obtenerTexto(usuario: str = Form(...), file: UploadFile = File(...)):
    textoFinal = ""

    try:
        image_bytes = await file.read() # Lee los bytes del archivo de imagen recibido en la petición
        urlFoto = guardar_imagen_s3(usuario, image_bytes)

        # Procesamiento de la imagen
        image_base64 = base64.b64encode(image_bytes).decode('utf-8')
        ollama_response = requests.post(
            "http://localhost:11434/api/generate",
            json={
                "model": "glm-ocr",
                "prompt": "Extract the text from this image accurately. Use ONLY plain text. Output ONLY the detected text in UTF-8.",
                "stream": False,
                "images": [image_base64]
            },
            timeout=60
        )

        if ollama_response.status_code == 200:
            textoFinal = ollama_response.json().get("response", "").strip()
            if not textoFinal:
                textoFinal = "No se detecto texto."
        else:
            textoFinal = f"Error : {ollama_response.status_code}"

        # Guardar en Historial
        db_ok = guardar_historial_db(usuario, textoFinal, urlFoto)
        status_code = "ok" if db_ok else "error_db"

    except Exception as e:
        print(f"Error proceso: {e}")
        return {"status": "error", "mensajeError": "Error procesando imagen."}

    return {"status": status_code, "texto": textoFinal, "urlFoto": urlFoto}


@app.post("/registro")
async def registrar_usuario(
        usuario: str = Form(...),
        nombre: str = Form(...),
        apellido: str = Form(...),
        email: str = Form(...),
        contrasena: str = Form(...)
):
    try:
        respuesta = tablaUsuarios.get_item(Key={'usuario': usuario}) #comprobamos que ese usuario no exista
        if 'Item' in respuesta:
            return {"status": "error", "mensajeError": "El usuario ya existe"}

        tablaUsuarios.put_item( # si no existe, lo añadimos a la tabla
            Item={
                'usuario': usuario,
                'nombre': nombre,
                'apellido': apellido,
                'email': email,
                'contrasena': contrasena,
                'fecha': datetime.datetime.now(datetime.timezone.utc).isoformat()
            }
        )
        return {"status": "ok", "mensajeError": "Usuario registrado"}
    except Exception as e:
        return {"status": "error", "mensajeError": str(e)}


@app.get("/obtenerUsuario/{usuario}")
async def otenerusuario(usuario: str):
    try:
        respuesta = tablaUsuarios.get_item(Key={'usuario': usuario})

        if 'Item' in respuesta:
            u = respuesta['Item']
            return {
                "status": "ok",
                "usuario": u['usuario'],
                "nombre": u['nombre'],
                "apellido": u['apellido'],
                "email": u['email'],
                "contrasena": u['contrasena']
            }
        else:
            return {"status": "error", "mensajeError": "Usuario no encontrado"}
    except Exception as e:
        return {"status": "error", "mensajeError": str(e)}
# ImageDownloader

Aplicación de línea de comandos en Java que valida una URL HTTP/HTTPS, obtiene el documento HTML y lista las referencias únicas de imágenes encontradas en etiquetas `<img>`.

## Requisitos

- JDK 17 o superior.
- Una URL HTTP o HTTPS válida.

## Compilación y ejecución

```bash
javac -d out src/main/java/ImageDownloader.java
java -cp out ImageDownloader https://example.com/
```

## Alcance actual

El proyecto cubre las fases 1 a 4 de la práctica:

1. Estructura del proyecto y configuración de Git.
2. Recepción y validación de un argumento URL.
3. Conexión mediante `URL.openConnection()` y lectura del HTML.
4. Extracción de referencias `src` únicas en etiquetas `<img>`.

La aplicación muestra el protocolo, servidor, puerto, ruta, código HTTP, tipo de contenido y hasta los primeros 500 caracteres del documento. Para HTTP/HTTPS utiliza `HttpURLConnection`, configura tiempos de conexión y lectura, y cierra los recursos al terminar.

## Limitaciones

La detección de imágenes usa expresiones regulares con fines didácticos. No admite atributos `src` sin comillas y puede fallar con marcado HTML complejo o contenido generado por JavaScript.

# ImageDownloader

Aplicación de línea de comandos en Java que obtiene una página web, identifica sus imágenes y las descarga localmente.

## Requisitos

- JDK 17 o superior.
- Una URL HTTP o HTTPS válida.

## Compilación y ejecución

```bash
javac -d out src/main/java/ImageDownloader.java
java -cp out ImageDownloader https://example.com/
```

## Funcionamiento

El proyecto cubre las fases 1 a 7 de la práctica:

1. Estructura del proyecto y configuración de Git.
2. Recepción y validación de un argumento URL.
3. Conexión mediante `URL.openConnection()` y lectura del HTML.
4. Extracción de referencias `src` únicas en etiquetas `<img>`.
5. Resolución de referencias relativas y absolutas.
6. Descarga binaria de imágenes en `imagenes/`.
7. Integración y resumen de descargas.

La aplicación muestra los datos de la URL, el código HTTP, el tipo de contenido y los primeros 500 caracteres del documento. Cada imagen se descarga con una conexión independiente. Solo acepta PNG, JPEG, GIF y WebP; usa nombres consecutivos, no sobrescribe archivos y limita cada descarga a 10 MB.

## Limitaciones

La detección de imágenes usa expresiones regulares con fines didácticos. No admite atributos `src` sin comillas y puede fallar con marcado HTML complejo o contenido generado por JavaScript.

## Pruebas funcionales

| Caso | Resultado esperado |
|---|---|
| Sin argumentos | Muestra instrucciones de uso. |
| URL inválida o protocolo distinto de HTTP/HTTPS | Muestra un error controlado. |
| Imagen relativa o absoluta | Se resuelve y se descarga. |
| Imagen repetida | Se descarga una sola vez. |
| Recurso no imagen, inexistente o mayor a 10 MB | Se registra como descarga fallida. |
| Archivo existente | No se sobrescribe. |

## Preguntas de reflexión

1. La aplicación actúa como cliente: solicita recursos al servidor y procesa sus respuestas.
2. `URL` representa una dirección, `URLConnection` configura y accede a un recurso, y `HttpURLConnection` agrega funciones propias de HTTP, como el código de estado.
3. `openConnection()` crea y configura el objeto de conexión, pero no necesariamente inicia la comunicación.
4. `connect()`, `getInputStream()` y `getResponseCode()` pueden iniciar la conexión.
5. El HTML y cada recurso referenciado se solicitan de forma independiente.
6. `getInputStream()` entrega el contenido; `getContentType()` informa el tipo MIME declarado por el servidor.
7. El texto requiere decodificación de caracteres y las imágenes deben conservarse como bytes.
8. Evitan que la aplicación espere indefinidamente ante servidores lentos o no disponibles.
9. Las expresiones regulares no comprenden por completo la estructura ni las variantes válidas de HTML.
10. Podría usarse un `ExecutorService` para asignar cada descarga a una tarea y controlar su finalización con la API estándar de Java.

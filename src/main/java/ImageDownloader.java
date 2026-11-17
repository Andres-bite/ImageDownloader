import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageDownloader {
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Pattern IMG_PATTERN = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SRC_PATTERN = Pattern.compile("\\bsrc\\s*=\\s*([\\\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java ImageDownloader <URL>");
            return;
        }

        URL url = validarURL(args[0]);
        if (url == null) {
            return;
        }

        mostrarDatosURL(url);

        try {
            String html = obtenerHTML(url);
            if (html == null) {
                return;
            }

            System.out.println("\nPrimeros caracteres del HTML:");
            System.out.println(html.substring(0, Math.min(500, html.length())));

            Set<String> imagenes = extraerImagenes(html);
            System.out.println("\nImágenes encontradas: " + imagenes.size());
            Path directorio = Path.of("imagenes");
            int exitosas = 0;
            int fallidas = 0;
            int numero = 1;

            for (String referencia : imagenes) {
                URL imagen = resolverImagen(url, referencia);
                if (imagen == null || !descargarImagen(imagen, directorio, numero)) {
                    fallidas++;
                } else {
                    exitosas++;
                }
                numero++;
            }

            System.out.println("\nDescargas exitosas: " + exitosas);
            System.out.println("Descargas fallidas: " + fallidas);
            System.out.println("Directorio: " + directorio.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error al obtener la página: " + e.getMessage());
        }
    }

    public static URL validarURL(String argumento) {
        try {
            URI uri = URI.create(argumento);
            String esquema = uri.getScheme();
            if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
                System.err.println("Protocolo no admitido. Use HTTP o HTTPS.");
                return null;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                System.err.println("URL inválida: falta el servidor.");
                return null;
            }
            return uri.toURL();
        } catch (IllegalArgumentException e) {
            System.err.println("URL inválida: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("URL no admitida: " + e.getMessage());
        }
        return null;
    }

    public static String obtenerHTML(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        HttpURLConnection httpConnection = connection instanceof HttpURLConnection
                ? (HttpURLConnection) connection : null;
        try {
            if (httpConnection != null) {
                httpConnection.setRequestMethod("GET");
                int codigo = httpConnection.getResponseCode();
                System.out.println("Código HTTP: " + codigo);
                if (codigo < 200 || codigo >= 300) {
                    System.err.println("El servidor respondió con un estado no exitoso.");
                    return null;
                }
            }

            String tipoContenido = connection.getContentType();
            System.out.println("Tipo de contenido: " + tipoContenido);
            Charset codificacion = obtenerCodificacion(tipoContenido);

            StringBuilder html = new StringBuilder();
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, codificacion))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    html.append(linea).append(System.lineSeparator());
                }
            }
            return html.toString();
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    public static Set<String> extraerImagenes(String html) {
        Set<String> imagenes = new LinkedHashSet<>();
        Matcher imgMatcher = IMG_PATTERN.matcher(html);
        while (imgMatcher.find()) {
            Matcher srcMatcher = SRC_PATTERN.matcher(imgMatcher.group());
            if (srcMatcher.find()) {
                imagenes.add(srcMatcher.group(2));
            }
        }
        return imagenes;
    }

    public static URL resolverImagen(URL urlPagina, String referencia) {
        if (referencia == null || referencia.isBlank()) {
            System.err.println("Referencia de imagen vacía.");
            return null;
        }

        try {
            URL imagen = new URL(urlPagina, referencia.trim());
            String protocolo = imagen.getProtocol();
            if (!(protocolo.equalsIgnoreCase("http") || protocolo.equalsIgnoreCase("https"))) {
                System.err.println("Referencia no admitida: " + referencia);
                return null;
            }
            if (imagen.getHost() == null || imagen.getHost().isBlank()) {
                System.err.println("Referencia sin servidor: " + referencia);
                return null;
            }
            return imagen;
        } catch (MalformedURLException e) {
            System.err.println("No se pudo resolver la imagen: " + referencia);
            return null;
        }
    }

    public static boolean descargarImagen(URL urlImagen, Path directorio, int numero) {
        URLConnection connection = null;
        HttpURLConnection httpConnection = null;
        Path destino = null;

        try {
            connection = urlImagen.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            if (connection instanceof HttpURLConnection) {
                httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod("GET");
                int codigo = httpConnection.getResponseCode();
                if (codigo < 200 || codigo >= 300) {
                    System.err.println("No se descargó " + urlImagen + ": HTTP " + codigo);
                    return false;
                }
            }

            String extension = extensionDeImagen(connection.getContentType());
            if (extension == null) {
                System.err.println("El recurso no es una imagen admitida: " + urlImagen);
                return false;
            }
            if (connection.getContentLengthLong() > MAX_IMAGE_SIZE) {
                System.err.println("La imagen supera el límite de 10 MB: " + urlImagen);
                return false;
            }

            Files.createDirectories(directorio);
            destino = directorio.resolve(String.format("imagen_%03d.%s", numero, extension));
            if (Files.exists(destino)) {
                System.err.println("El archivo ya existe: " + destino);
                return false;
            }

            long bytesLeidos = 0;
            byte[] buffer = new byte[8192];
            try (InputStream input = connection.getInputStream();
                 OutputStream output = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW)) {
                int leidos;
                while ((leidos = input.read(buffer)) != -1) {
                    bytesLeidos += leidos;
                    if (bytesLeidos > MAX_IMAGE_SIZE) {
                        throw new IOException("La imagen supera el límite de 10 MB.");
                    }
                    output.write(buffer, 0, leidos);
                }
            }

            System.out.println("Imagen descargada: " + destino);
            return true;
        } catch (IOException e) {
            System.err.println("Error al descargar " + urlImagen + ": " + e.getMessage());
            if (destino != null) {
                try {
                    Files.deleteIfExists(destino);
                } catch (IOException ignored) {
                    System.err.println("No se pudo eliminar el archivo parcial: " + destino);
                }
            }
            return false;
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    private static void mostrarDatosURL(URL url) {
        System.out.println("Protocolo: " + url.getProtocol());
        System.out.println("Servidor: " + url.getHost());
        System.out.println("Puerto: " + url.getPort());
        System.out.println("Ruta: " + url.getPath());
    }

    private static Charset obtenerCodificacion(String tipoContenido) {
        if (tipoContenido != null) {
            for (String parte : tipoContenido.split(";")) {
                String valor = parte.trim();
                if (valor.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                    try {
                        return Charset.forName(valor.substring("charset=".length()).trim());
                    } catch (Exception ignored) {
                        return StandardCharsets.UTF_8;
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String extensionDeImagen(String tipoContenido) {
        if (tipoContenido == null) {
            return null;
        }
        String tipo = tipoContenido.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return switch (tipo) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> null;
        };
    }
}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageDownloader {
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;
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
            for (String imagen : imagenes) {
                System.out.println(imagen);
            }
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
}

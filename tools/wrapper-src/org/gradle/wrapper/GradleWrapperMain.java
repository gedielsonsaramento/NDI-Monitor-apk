package org.gradle.wrapper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Launcher pequeno e autocontido usado apenas para inicializar a distribuicao Gradle. */
public final class GradleWrapperMain {
    private GradleWrapperMain() {}

    public static void main(String[] args) throws Exception {
        File project = new File(System.getProperty("user.dir")).getCanonicalFile();
        File propertiesFile = new File(project, "gradle/wrapper/gradle-wrapper.properties");
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
        }

        String distributionUrl = properties.getProperty("distributionUrl");
        if (distributionUrl == null || distributionUrl.trim().isEmpty()) {
            throw new IOException("distributionUrl ausente em gradle-wrapper.properties");
        }

        String fileName = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1);
        String folderName = fileName.replace("-bin.zip", "").replace("-all.zip", "");
        File wrapperHome = new File(System.getProperty("user.home"), ".gradle/wrapper/dists/ndi-monitor/" + folderName);
        File gradleHome = new File(wrapperHome, folderName);
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        File executable = new File(gradleHome, windows ? "bin/gradle.bat" : "bin/gradle");

        if (!executable.isFile()) {
            wrapperHome.mkdirs();
            File zip = new File(wrapperHome, fileName);
            if (!zip.isFile()) download(distributionUrl, zip);
            unzip(zip, wrapperHome);
        }

        if (!executable.isFile()) throw new IOException("Executavel Gradle nao encontrado apos a extracao: " + executable);
        if (!windows) executable.setExecutable(true);

        List<String> command = new ArrayList<>();
        command.add(executable.getAbsolutePath());
        for (String arg : args) command.add(arg);
        Process process = new ProcessBuilder(command)
                .directory(project)
                .inheritIO()
                .start();
        System.exit(process.waitFor());
    }

    private static void download(String source, File target) throws IOException {
        File temporary = new File(target.getAbsolutePath() + ".part");
        URL url = new URL(source);
        for (int redirects = 0; redirects < 8; redirects++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Gradle-Wrapper");
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) throw new IOException("Redirecionamento sem destino");
                url = new URL(url, location);
                continue;
            }
            if (status != 200) throw new IOException("Falha ao baixar Gradle: HTTP " + status);
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            } finally {
                connection.disconnect();
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        throw new IOException("Muitos redirecionamentos ao baixar Gradle");
    }

    private static void unzip(File zip, File destination) throws IOException {
        String destinationPath = destination.getCanonicalPath() + File.separator;
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = input.getNextEntry()) != null) {
                File output = new File(destination, entry.getName());
                if (!output.getCanonicalPath().startsWith(destinationPath)) {
                    throw new IOException("Entrada ZIP invalida: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    output.mkdirs();
                } else {
                    File parent = output.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream stream = new FileOutputStream(output)) {
                        int count;
                        while ((count = input.read(buffer)) >= 0) stream.write(buffer, 0, count);
                    }
                }
                input.closeEntry();
            }
        }
    }
}

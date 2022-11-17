package com.pushwoosh.internal.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileUtils {
    private static final String TAG = "FileUtils";

    private static final int TRY_COUNT = 3;

    @Nullable
    public static File downloadFile(String linkUrl, File destination) {
        int count;
        int tryCount = 0;
        InputStream input = null;
        OutputStream output = null;
        try {
            while (tryCount++ < TRY_COUNT) {
                HttpURLConnection openConnection = null;
                try {
                    URL url = new URL(linkUrl);
                    openConnection = (HttpURLConnection) url.openConnection();
                    openConnection.connect();

                    if (openConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        PWLog.error(TAG, "fail download: " + linkUrl + "  responseCode: " + openConnection.getResponseCode());
                        return null;
                    }

                    input = new BufferedInputStream(openConnection.getInputStream());
                    output = new BufferedOutputStream(new FileOutputStream(destination));

                    byte[] data = new byte[1024];

                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }

                    output.flush();
                    return destination;
                } catch (MalformedURLException ignore) {
                    //not need retry
                    break;
                } catch (IOException e) {
                    PWLog.exception(e);
                    //XXX retry
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException ignore) {
                            // ignore
                        }
                    }
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException ignore) {
                            // ignore
                        }
                    }
                    if (openConnection != null) {
                        openConnection.disconnect();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static boolean isSSLSessionNPEException(Exception e) {
        return (e instanceof NullPointerException) && e.getMessage().equals("ssl_session == null");
    }

    @Nullable
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File unzip(@Nullable File zip, @Nullable File destination) {
        if (zip == null || destination == null) {
            return null;
        }

        final int bufferSize = 8192;

        if (!isValidZip(zip)) {
            return null;
        }

        deleteDirectory(destination);

        if (!destination.isDirectory()) {
            if (!destination.mkdirs()) {
                return null;
            }
        }

        ZipInputStream zis = null;

        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip), bufferSize));

            ZipEntry ze;
            byte[] buff = new byte[bufferSize];
            int read;
            String destinationString = destination.getCanonicalPath();
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(destinationString, ze.getName());
                String canonicalPath = file.getCanonicalPath();
                if (!canonicalPath.startsWith(destinationString)) {
                    throw new SecurityException("Provided zip file path has Path Traversal Vulnerability");
                }
                if (ze.isDirectory()) {
                    file.mkdir();
                    continue;
                }

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(file.getParent()).mkdirs();

                OutputStream out = null;

                try {
                    out = new BufferedOutputStream(new FileOutputStream(file), bufferSize);

                    while ((read = zis.read(buff)) != -1) {
                        out.write(buff, 0, read);
                    }

                    out.flush();
                } catch (IOException e) {
                    PWLog.exception(e);
                    file.delete();
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }

            return destination;
        } catch (FileNotFoundException e) {
            PWLog.error(TAG, e.getMessage(), e);
        } catch (IOException e) {
            PWLog.error(TAG, e.getMessage(), e);
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    public static boolean deleteDirectory(File directory) {
        if (!directory.exists()) {
            return true;
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }

    public static String readFile(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            reader.close();
            fin.close();
        }

        return sb.toString();
    }

    public static void writeFile(File file, String content) throws IOException {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                return;
            }
        }

        FileOutputStream stream = new FileOutputStream(file);
        try {
            stream.write(content.getBytes());
        } finally {
            stream.close();
        }
    }

    @NonNull
    public static String getMd5Hash(@NonNull File file) {
        InputStream inputStream = null;
        MessageDigest md;

        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            md = MessageDigest.getInstance("MD5");

            byte[] dataBytes = new byte[1024];
            int count;
            while ((count = inputStream.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, count);
            }

            StringBuilder sb = new StringBuilder();
            for (final byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (IOException e) {
            return "";
        } catch (NoSuchAlgorithmException e) {
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    PWLog.error("Failed to read file " + file.getName(), e);
                }
            }
        }
    }

    private static boolean isValidZip(final File file) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    public static String getLastPathComponent(String path) {
        String[] components = path.split("/");
        return components[components.length - 1];
    }

    public static String removeExtension(String path) {
        return path.substring(0, path.lastIndexOf("."));
    }
}

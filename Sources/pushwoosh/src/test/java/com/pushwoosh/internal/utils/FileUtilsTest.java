/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.internal.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.Okio;

/**
 * Created by aevstefeev on 05/03/2018.
 */
public class FileUtilsTest {

    public static final String TEST_FOLDER = "test";

    // Deliberately not FileUtils.TRY_COUNT: reading the production constant makes the assert pass for
    // any budget. This copy going stale IS the signal — raising the budget must be a conscious choice.
    private static final int EXPECTED_ATTEMPTS = 3;

    private int originalConnectTimeoutMs;
    private int originalReadTimeoutMs;

    @Before
    public void setUp() {
        // @Before/@After sit outside the @Test(timeout) statement, so they run on the main thread and
        // still restore the statics when a test body hangs and dies as TestTimedOutException.
        originalConnectTimeoutMs = FileUtils.connectTimeoutMs;
        originalReadTimeoutMs = FileUtils.readTimeoutMs;
        new File(TEST_FOLDER).mkdir();
    }

    @After
    public void taerDown() {
        FileUtils.connectTimeoutMs = originalConnectTimeoutMs;
        FileUtils.readTimeoutMs = originalReadTimeoutMs;
        FileUtils.deleteDirectory(new File("test"));
    }

    @Test
    public void downloadFile() throws Exception {
        MockWebServer server = enableMockServerForTestDownload();

        HttpUrl baseUrl = server.url("/");
        String requestUrl = baseUrl.url().toString();
        File destFile = new File(TEST_FOLDER + "/destFile.zip");
        FileUtils.downloadFile(requestUrl, destFile);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath(), is(equalTo("/")));
        Assert.assertTrue(destFile.exists());
        String md5SoureFile = FileUtils.getMd5Hash(new File(TEST_FOLDER + "/sourceFile.zip"));
        String md5DestFile = FileUtils.getMd5Hash(new File(TEST_FOLDER + "/destFile.zip"));
        Assert.assertEquals(md5SoureFile, md5DestFile);
    }

    // A ServerSocket we never accept() on: the OS backlog completes the handshake, so connect
    // succeeds and every attempt has to die on readTimeoutMs instead of pinning the thread.
    @Test(timeout = 10000)
    public void downloadFileAllAttemptsTimeOutReturnsNull() throws Exception {
        ServerSocket silentServer = new ServerSocket(0);
        try (MockedStatic<PWLog> ignored = Mockito.mockStatic(PWLog.class)) {
            FileUtils.readTimeoutMs = 200;
            File destFile = new File(TEST_FOLDER + "/destFile.zip");

            File result = FileUtils.downloadFile("http://127.0.0.1:" + silentServer.getLocalPort() + "/", destFile);

            Assert.assertNull(result);
            Assert.assertEquals(EXPECTED_ATTEMPTS, drainAcceptQueue(silentServer));
        } finally {
            silentServer.close();
        }
    }

    @Test(timeout = 10000)
    public void downloadFileFirstAttemptTimesOutRetriesAndSucceeds() throws Exception {
        createTestZip(TEST_FOLDER + "/sourceFile.zip");
        MockWebServer server = new MockWebServer();
        server.setBodyLimit(0);
        try (MockedStatic<PWLog> ignored = Mockito.mockStatic(PWLog.class)) {
            FileUtils.readTimeoutMs = 500;

            Buffer delayedBody = new Buffer();
            delayedBody.writeAll(Okio.source(new File(TEST_FOLDER + "/sourceFile.zip")));
            // finite delay > readTimeoutMs: the client times out, the server thread does not hang
            server.enqueue(new MockResponse().setBody(delayedBody).setBodyDelay(2, TimeUnit.SECONDS));

            Buffer normalBody = new Buffer();
            normalBody.writeAll(Okio.source(new File(TEST_FOLDER + "/sourceFile.zip")));
            server.enqueue(new MockResponse().setBody(normalBody));

            File destFile = new File(TEST_FOLDER + "/destFile.zip");
            File result = FileUtils.downloadFile(server.url("/").toString(), destFile);

            Assert.assertNotNull(result);
            Assert.assertEquals(
                    FileUtils.getMd5Hash(new File(TEST_FOLDER + "/sourceFile.zip")), FileUtils.getMd5Hash(destFile));
            Assert.assertEquals(2, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void downloadFileConnectTimesOutOnEveryAttemptReturnsNull() throws Exception {
        ServerSocket backloggedServer = new ServerSocket(0, 1);
        List<Socket> queueHolders = new ArrayList<>();
        try (MockedStatic<PWLog> ignored = Mockito.mockStatic(PWLog.class)) {
            fillAcceptQueue(backloggedServer, queueHolders);
            FileUtils.connectTimeoutMs = 200;
            File destFile = new File(TEST_FOLDER + "/destFile.zip");

            File result = FileUtils.downloadFile("http://127.0.0.1:" + backloggedServer.getLocalPort() + "/", destFile);

            Assert.assertNull(result);
        } finally {
            for (Socket queueHolder : queueHolders) {
                queueHolder.close();
            }
            backloggedServer.close();
        }
    }

    // Connects until one stalls: past that point the OS drops every SYN, so connect() can only die on
    // connectTimeoutMs. Probing beats a fixed count — the queue depth differs across platforms.
    private void fillAcceptQueue(ServerSocket server, List<Socket> queueHolders) throws IOException {
        while (queueHolders.size() < 50) {
            Socket probe = new Socket();
            try {
                probe.connect(new InetSocketAddress("127.0.0.1", server.getLocalPort()), 200);
                queueHolders.add(probe);
            } catch (SocketTimeoutException e) {
                probe.close();
                return;
            }
        }
    }

    // Every attempt leaves its connection in the backlog, so the queue depth is the attempt count:
    // it pins the retry budget, which assertNull alone cannot distinguish from any other failure.
    private int drainAcceptQueue(ServerSocket server) throws IOException {
        server.setSoTimeout(200);
        int accepted = 0;
        while (true) {
            try {
                server.accept().close();
                accepted++;
            } catch (SocketTimeoutException e) {
                return accepted;
            }
        }
    }

    private MockWebServer enableMockServerForTestDownload() throws IOException, InterruptedException {
        createTestZip("test/sourceFile.zip");
        final MockWebServer server = new MockWebServer();
        server.setBodyLimit(0);
        Buffer buffer = new Buffer();
        buffer.writeAll(Okio.source(new File(TEST_FOLDER + "/sourceFile.zip")));
        server.enqueue(new MockResponse().setBody(buffer));
        return server;
    }

    @Test
    public void unzip() throws Exception {
        String zipFilePath = TEST_FOLDER + "/test.zip";
        String destFilePath = TEST_FOLDER + "/destFolder";
        File destFolder = new File(destFilePath);
        File zipFile = new File(zipFilePath);
        createTestZip(zipFilePath);

        File resulstFile = FileUtils.unzip(zipFile, destFolder);

        Assert.assertNotNull(resulstFile);
        Assert.assertTrue(resulstFile.exists());
        Assert.assertEquals(destFolder, resulstFile);

        File[] fileList = destFolder.listFiles();
        Assert.assertEquals(fileList.length, 1);
        File file = fileList[0];
        Assert.assertEquals(TEST_FOLDER + "/destFolder/mytext.txt", file.getPath());
        // TODO replace with normal way read text from file
        Assert.assertEquals("Test String\n", FileUtils.readFile(file));
    }

    private void createTestZip(String zipFilePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Test String");

        File f = new File(zipFilePath);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry("mytext.txt");
        out.putNextEntry(e);

        byte[] data = sb.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();

        out.close();
    }

    @Test
    public void deleteDirectory() throws Exception {
        File directory = new File(TEST_FOLDER + "/dir1");
        directory.mkdir();
        FileUtils.deleteDirectory(directory);
        Assert.assertFalse(directory.exists());

        File file = new File(TEST_FOLDER + "/file1");
        boolean mkFileResult = file.createNewFile();
        FileUtils.deleteDirectory(file);
        Assert.assertFalse(file.exists());
    }

    @Test
    public void readFile() throws Exception {
        String textFile = TEST_FOLDER + "/text.txt";
        String contentTextFile = "Normal _Content 123";
        try (PrintWriter out = new PrintWriter(textFile)) {
            out.println(contentTextFile);
        }
        String text = FileUtils.readFile(new File(textFile));

        Assert.assertEquals(contentTextFile + "\n", text);
    }

    @Test
    public void writeFile() throws Exception {
        // TODO
    }

    @Test
    public void getMd5Hash() throws Exception {
        String textFile = TEST_FOLDER + "/text.txt";
        String contentTextFile = "Normal _Content 123";
        try (PrintWriter out = new PrintWriter(textFile)) {
            out.println(contentTextFile);
        }
        String hash = FileUtils.getMd5Hash(new File(textFile));
        Assert.assertEquals("3223c67025d93b58f2c57eb0e8aab7b9", hash);
    }

    @Test
    public void getLastPathComponent() throws Exception {
        String path = "/dir/File";
        String newPath = FileUtils.getLastPathComponent(path);
        Assert.assertEquals("File", newPath);

        String path2 = "dir/dir2/dir3/File";
        String newPath2 = FileUtils.getLastPathComponent(path2);
        Assert.assertEquals("File", newPath2);

        String path3 = "dir/dir2/dir3/File/";
        String newPath3 = FileUtils.getLastPathComponent(path3);
        Assert.assertEquals("File", newPath3);
    }

    @Test
    public void removeExtension() throws Exception {
        String path = "/dir/File.txt";
        String newPath = FileUtils.removeExtension(path);
        Assert.assertEquals("/dir/File", newPath);

        String path2 = "dir/dir2/dir3/File.txt";
        String newPath2 = FileUtils.removeExtension(path2);
        Assert.assertEquals("dir/dir2/dir3/File", newPath2);

        String path3 = "dir/dir2/dir3/File.txt/";
        String newPath3 = FileUtils.removeExtension(path3);
        Assert.assertEquals("dir/dir2/dir3/File", newPath3);
    }
}

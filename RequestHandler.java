package com.libproxy;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;


/**
 * RequestHandler processes each client's request through proxy server,
 * then sends to remote server and write back to client.
 *
 * @author hwong3
 */
class RequestHandler implements Runnable {

    /**
     * Client input stream for reading request
     */
    private DataInputStream clientInputStream;


    /**
     * Client output stream for rendering response
     */
    private OutputStream clientOutputStream;

    /**
     * Remote output stream to send in client's request
     */
    private OutputStream remoteOutputStream;

    /**
     * Remote input stream to read back response to client
     */
    private InputStream remoteInputStream;

    /**
     * Client socket object
     */
    private Socket clientSocket;

    /**
     * Remote socket object
     */
    private Socket remoteSocket;

    /**
     * Client request type (Only "GET" or "POST" are handled)
     */
    private String requestType;

    /**
     * Client request url (e.g. http://www.google.com)
     */
    private String url;

    /**
     * Client request uri parsed from url (e.g. /index.html)
     */
    private String uri;

    /**
     * Client request version (e.g. HTTP/1.1)
     */
    private String httpVersion;

    /**
     * Data structure to hold all client request handers (e.g. proxy-connection: keep-alive)
     */
    private HashMap<String, String> header;

    /**
     * End of line character
     */
    private static String endOfLine = "\r\n";

    /**
     * Create a RequestHandler instance with clientSocket object
     *
     * @param clientSocket Client socket object
     */
    RequestHandler(Socket clientSocket) {
        header = new HashMap<String, String>();
        this.clientSocket = clientSocket;
    }

    /**
     * When instance is created, open client/remote streams then
     * proceed with the following 3 tasks:<br>
     * <p/>
     * 1) get request from client<br>
     * 2) forward request to remote host<br>
     * 3) read response from remote back to client<br>
     * <p/>
     * Close client/remote streams when finished.<br>
     *
     * @see Runnable#run()
     */
    public void run() {

        try {

            clientInputStream = new DataInputStream(clientSocket.getInputStream());
            clientOutputStream = clientSocket.getOutputStream();

            // step 1) get request from client
            clientToProxy();

            // step 2) forward request to remote host
            proxyToRemote();

            // step 3) read response from remote back to client
            remoteToClient();

            System.out.println();

            if (remoteOutputStream != null) remoteOutputStream.close();
            if (remoteInputStream != null) remoteInputStream.close();
            if (remoteSocket != null) remoteSocket.close();


            if (clientOutputStream != null) clientOutputStream.close();
            if (clientInputStream != null) clientInputStream.close();
            if (clientSocket != null) clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive and pre-process client's request headers before redirecting to remote server
     */
    private void clientToProxy() {

        String line, key, value;
        StringTokenizer tokens;

        try {
            byte[] data=new byte[1024];
            int len;
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            // get remote response header
            while ((len =  clientInputStream.read(data)) > 0 ) {
                if(data[0]=='\r' || data[1]=='\n'){
                    break;
                }
                outputStream.write(data, 0, len);
            }
            line = outputStream.toString();
            // HTTP Command
            if (!TextUtils.isEmpty(line)) {
                tokens = new StringTokenizer(line);
                requestType = tokens.nextToken();
                url = tokens.nextToken();
                httpVersion = tokens.nextToken();
                Log.d(ProxySetting.LOG_TAG, String.format("requestType=%s,url=%s,httpVersion=%s", requestType, url, httpVersion));
            }
            outputStream.reset();
            while (clientInputStream.read(data) > 0 ) {
                if (data[0] == '\r' || data[1] == '\n') {
                    line = outputStream.toString();
                    tokens = new StringTokenizer(line);
                    key = tokens.nextToken(":");
                    value = line.replaceAll(key, "").replace(": ", "");

                    Log.d(ProxySetting.LOG_TAG, "clientToProxy  key=" + key + ", value=" + value);

                    if ("x-requested-with".equalsIgnoreCase(key)) {
                        continue;
                    }
                    header.put(key.toLowerCase(), value);
                }
            }
//            // Header Info
//            while ((line = clientInputStream.readLine()) != null) {
//                // check for empty line
//                if (line.trim().length() == 0) break;
//
//                // tokenize every header as key and value pair
//                tokens = new StringTokenizer(line);
//                key = tokens.nextToken(":");
//                value = line.replaceAll(key, "").replace(": ", "");
//
//                Log.d(ProxySetting.LOG_TAG, "clientToProxy  key=" + key + ", value=" + value);
//
//                if ("x-requested-with".equalsIgnoreCase(key)) {
//                    continue;
//                }
//                header.put(key.toLowerCase(), value);
//            }

            stripUnwantedHeaders(header);
            getUri();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sending pre-processed client request to remote server
     */
    private void proxyToRemote() {

        try {
            if (header.get("host") == null) return;
//            if (!requestType.startsWith("GET") && !requestType.startsWith("POST"))
//                return;

            String host = header.get("host");
            String[] split = host.split(":");
            remoteSocket = new Socket(split[0], split.length > 1 ? Integer.parseInt(split[1]) : 80);
            remoteOutputStream = remoteSocket.getOutputStream();

            // make sure streams are still open
            checkRemoteStreams();
            checkClientStreams();

            // make request from client to remote server
            String request = requestType + " " + uri + " HTTP/1.0";
            Log.d(ProxySetting.LOG_TAG, "request=" + request);
            remoteOutputStream.write(request.getBytes());
            remoteOutputStream.write(endOfLine.getBytes());

            // send hostname
            String command = "host: " + header.get("host");
            remoteOutputStream.write(command.getBytes());
            remoteOutputStream.write(endOfLine.getBytes());
            System.out.println(command);

            // send rest of the headers
            for (String key : header.keySet()) {
                if (!key.equals("host")) {
                    command = key + ": " + header.get(key);
                    remoteOutputStream.write(command.getBytes());
                    remoteOutputStream.write(endOfLine.getBytes());
                    System.out.println(command);
                    Log.d(ProxySetting.LOG_TAG, "proxyToRemote command=" + command);
                }
            }

            remoteOutputStream.write(endOfLine.getBytes());
            remoteOutputStream.flush();

            // send client request data if its a POST request
            if (requestType.startsWith("POST")) {

                int contentLength = Integer.parseInt(header.get("content-length"));
                for (int i = 0; i < contentLength; i++) {
                    remoteOutputStream.write(clientInputStream.read());
                }
            }

            // complete remote server request
            remoteOutputStream.write(endOfLine.getBytes());
            remoteOutputStream.flush();
        } catch (Exception e) {
            Log.e(ProxySetting.LOG_TAG, "proxyToRemote error=" + e);
        }
    }

    /**
     * Sending buffered remote server response back to client with minor header processing
     */
    private void remoteToClient() {

        try {

            // If socket is closed, return
            if (remoteSocket == null) return;

//            String line;
            DataInputStream remoteOutHeader = new DataInputStream(remoteSocket.getInputStream());

            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            byte[] data=new byte[1024];
            int len;
            // get remote response header
            while ((len =  remoteOutHeader.read(data)) > 0 ) {
                if(data[0] == '\r' || data[0] == '\n'){
                    String line = outputStream.toString();
                    Log.d(ProxySetting.LOG_TAG, "remoteToClient line=" + line);
                    outputStream.reset();
                }
                clientOutputStream.write(data, 0 ,len);
//
//                // check for end of header blank line
//                if (line.trim().length() == 0) break;
//
//                // check for proxy-connection: keep-alive
//               /* if (line.toLowerCase().startsWith("proxy")) continue;
//                if (line.contains("keep-alive")) continue;*/
//
//                // write remote response to client
//
//                clientOutputStream.write(line.getBytes());
//                clientOutputStream.write(endOfLine.getBytes());
            }
            // complete remote header response
            //clientOutputStream.write(endOfLine.getBytes());
            clientOutputStream.flush();

            // get remote response body
            remoteInputStream = remoteSocket.getInputStream();
            byte[] buffer = new byte[1024];

            // buffer remote response then write it back to client
            for (int i; (i = remoteInputStream.read(buffer)) != -1; ) {
                clientOutputStream.write(buffer, 0, i);
                clientOutputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function to strip out unwanted request header from client
     */
    private void stripUnwantedHeaders(HashMap<String, String> header) {

//        if (header.containsKey("user-agent")) header.remove("user-agent");
//        if (header.containsKey("referer")) header.remove("referer");
        if (header.containsKey("proxy-connection")) {
            header.put("connection", header.get("proxy-connection"));
            header.remove("proxy-connection");
        }

      /*  if (header.containsKey("connection") && header.get("connection").equalsIgnoreCase("keep-alive")) {
            header.remove("connection");
        }*/
    }

    /**
     * Helper function to check for client input and output stream, reconnect if closed
     */
    private void checkClientStreams() {

        try {
            if (clientSocket.isOutputShutdown())
                clientOutputStream = clientSocket.getOutputStream();
            if (clientSocket.isInputShutdown())
                clientInputStream = new DataInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function to check for remote input and output stream, reconnect if closed
     */
    private void checkRemoteStreams() {

        try {
            if (remoteSocket.isOutputShutdown())
                remoteOutputStream = remoteSocket.getOutputStream();
            if (remoteSocket.isInputShutdown())
                remoteInputStream = new DataInputStream(remoteSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function to parse URI from full URL
     */
    private void getUri() {

        if (header.containsKey("host")) {
            int temp = url.indexOf(header.get("host"));
            temp += header.get("host").length();

            if (temp < 0) {
                // prevent index out of bound, use entire url instead
                uri = url;
            } else {
                // get uri from part of the url
                uri = url.substring(temp);
            }
        }
        Log.d(ProxySetting.LOG_TAG, "uri=" + uri);
    }

}

package org.votingsystem.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.fasterxml.jackson.core.type.TypeReference;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.throwable.BadRequestExceptionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.NotFoundExceptionVS;
import org.votingsystem.throwable.RequestRepeatedExceptionVS;
import org.votingsystem.throwable.ServerExceptionVS;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static org.votingsystem.util.Constants.SIGNED_FILE_NAME;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class HttpConnection {
    
	public static final String TAG = HttpConnection.class.getSimpleName();
    
    private DefaultHttpClient httpclient;
    private ThreadSafeClientConnManager cm;
    private HttpContext httpContext;
    private static HttpConnection INSTANCE;

    public class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
                KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);
            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    LOGD(TAG, "checkServerTrusted BYPASSING CERT VALIDATION!!!");
                }
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    LOGD(TAG, "checkServerTrusted BYPASSING CERT VALIDATION!!!");
                }
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    private HttpConnection(X509Certificate trustedSSLServerCert) {
        try {
            if(trustedSSLServerCert != null) {
                HttpParams params = new BasicHttpParams();
                params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
                params.setParameter(CoreProtocolPNames.USER_AGENT, "Apache-HttpClient/Android");
                params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
                params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
                SchemeRegistry schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                trustStore.setCertificateEntry(trustedSSLServerCert.getSubjectDN().toString(),
                        trustedSSLServerCert);
                SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
                //schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
                schemeRegistry.register(new Scheme("https", socketFactory, 443));
                LOGD(TAG, "Added Scheme https with port 443 to Apache httpclient");
                cm = new ThreadSafeClientConnManager(params, schemeRegistry);
                httpclient = new DefaultHttpClient(cm, params);
                CookieStore cookieStore = new BasicCookieStore();
                httpContext = new BasicHttpContext();
                httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            } else {
                LOGD(TAG, "httpContext BYPASSING CERT VALIDATION!!!");
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                HttpParams params = new BasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
                SchemeRegistry registry = new SchemeRegistry();
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                registry.register(new Scheme("https", sf, 443));
                cm = new ThreadSafeClientConnManager(params, registry);
                httpclient = new DefaultHttpClient(cm, params);
                CookieStore cookieStore = new BasicCookieStore();
                httpContext = new BasicHttpContext();
                httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public Cookie getCookie(String domain) {
        CookieStore cookieStore = (CookieStore) httpContext.getAttribute(ClientContext.COOKIE_STORE);
        Cookie result = null;
        for(Cookie cookie : cookieStore.getCookies()) {
            if(cookie.getDomain().equals(domain)) result = cookie;
        }
        return result;
    }

    public String getSessionId(String domain) {
        Cookie cookie = getCookie(domain);
        String result = null;
        if(cookie != null) {
            result = cookie.getValue().contains(".") ? cookie.getValue().split("\\.")[0] : cookie.getValue();
        }
        return result;
    }

    public static void init(X509Certificate trustedSSLServerCert) {
        INSTANCE = new HttpConnection(trustedSSLServerCert);
    }

    public static HttpConnection getInstance() {
        return INSTANCE;
    }
    
    public void shutdown () {
        LOGD(TAG, "shutdown");
        try {
            httpclient.getConnectionManager().shutdown();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    
    public ResponseDto getData (String serverURL, ContentType contentType) {
        LOGD(TAG + ".getData" , "serverURL: " + serverURL + " - contentType: " + contentType);
        HttpResponse response = null;
        ResponseDto responseDto = null;
        ContentType responseContentType = null;
        try {
            HttpGet httpget = new HttpGet(serverURL);
            if(contentType != null) httpget.setHeader("Content-Type", contentType.getName());
            response = httpclient.execute(httpget, httpContext);
            LOGD(TAG + ".getData", "----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) { System.out.println(headers[i]); }*/
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentType.getByName(header.getValue());
            LOGD(TAG + ".getData", "Connections in pool: " + cm.getConnectionsInPool());
            LOGD(TAG + ".getData" ,response.getStatusLine().toString() + " - " +
                    response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
            LOGD(TAG + ".getData", "----------------------------------------");
            if(ResponseDto.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),
                        responseBytes, responseContentType);
            } else {
                responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()),responseContentType);
            }
        } catch(ConnectTimeoutException ex) {
            responseDto = new ResponseDto(ResponseDto.SC_CONNECTION_TIMEOUT, ex.getMessage());
        } catch(Exception ex) {
            LOGD(TAG + ".getData" , "exception: " + ex.getMessage());
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
        }
        return responseDto;
    }

    public <T> T getData(Class<T> type, TypeReference typeReference, String serverURL, String mediaType) throws Exception {
        LOGD(TAG + ".getData" , "serverURL: " + serverURL + " - mediaType: " + mediaType);
        HttpResponse response = null;
        String responseContentType = null;
        HttpGet httpget = new HttpGet(serverURL);
        if(mediaType != null) httpget.setHeader("Content-Type", mediaType);
        response = httpclient.execute(httpget, httpContext);
        LOGD(TAG + ".getData", "----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) { System.out.println(headers[i]); }*/
        Header header = response.getFirstHeader("Content-Type");
        if(header != null) responseContentType = header.getValue();
        LOGD(TAG + ".getData", "Connections in pool: " + cm.getConnectionsInPool());
        LOGD(TAG + ".getData" ,response.getStatusLine().toString() + " - " +
                response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
        LOGD(TAG + ".getData", "----------------------------------------");
        byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
        if(ResponseDto.SC_OK == response.getStatusLine().getStatusCode()) {
            if(type != null) return JSON.readValue(responseBytes, type);
            else return JSON.readValue(responseBytes, typeReference);
        } else {
            MessageDto messageDto = null;
            String responseStr = null;
            if(responseContentType != null && responseContentType.contains(MediaType.JSON)) messageDto =
                    JSON.readValue(responseBytes, MessageDto.class);
            else responseStr = new String(responseBytes, "UTF-8");
            switch (response.getStatusLine().getStatusCode()) {
                case ResponseDto.SC_NOT_FOUND: throw new NotFoundExceptionVS(responseStr, messageDto);
                case ResponseDto.SC_ERROR_REQUEST_REPEATED: throw new RequestRepeatedExceptionVS(responseStr, messageDto);
                case ResponseDto.SC_ERROR_REQUEST: throw new BadRequestExceptionVS(responseStr, messageDto);
                case ResponseDto.SC_ERROR: throw new ServerExceptionVS(EntityUtils.toString(response.getEntity()), messageDto);
                default:throw new ExceptionVS(responseStr, messageDto);
            }
        }
    }

    public <T> T getData(TypeReference type, String serverURL, String mediaType) throws Exception {
        return getData(null, type, serverURL, mediaType);
    }

    public <T> T getData(Class<T> type, String serverURL, String mediaType) throws Exception {
        return getData(type, null, serverURL, mediaType);
    }

    public <T> T sendData(TypeReference type, byte[] data, String serverURL, String mediaType) throws Exception {
        return sendData(null, type, data, mediaType, serverURL);
    }

    public <T> T sendData1(Class<T> type, byte[] data, String serverURL, String mediaType) throws Exception {
        return sendData(type, null, data, mediaType, serverURL);
    }

    public <T> T sendData(Class<T> type, TypeReference typeReference, byte[] data,
            String contentType, String serverURL, String... headerNames) throws IOException, ExceptionVS {
        HttpPost httpPost = new HttpPost(serverURL);
        LOGD(TAG + ".sendData" , "serverURL: " + serverURL + " - contentType: " + contentType);
        HttpResponse response = null;
        String responseContentType = null;
        ResponseDto responseDto = null;
        ByteArrayEntity reqEntity = new ByteArrayEntity(data);
        if(contentType != null) reqEntity.setContentType(contentType);
        httpPost.setEntity(reqEntity);
        httpPost.setEntity(reqEntity);
        response = httpclient.execute(httpPost, httpContext);
        Header header = response.getFirstHeader("Content-Type");
        if(header != null) responseContentType = header.getValue();
        LOGD(TAG + ".sendData" ,"----------------------------------------");
        LOGD(TAG + ".sendData" , response.getStatusLine().toString() + " - " +
                response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
        LOGD(TAG + ".sendData" , "----------------------------------------");
        byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
        if(ResponseDto.SC_OK == response.getStatusLine().getStatusCode()) {
            if(type != null) return JSON.readValue(responseBytes, type);
            else return JSON.readValue(responseBytes, typeReference);
        } else {
            MessageDto messageDto = null;
            String responseStr = null;
            if(responseContentType != null && responseContentType.contains(MediaType.JSON)) messageDto =
                    JSON.readValue(responseBytes, MessageDto.class);
            else responseStr = new String(responseBytes, "UTF-8");
            switch (response.getStatusLine().getStatusCode()) {
                case ResponseDto.SC_NOT_FOUND: throw new NotFoundExceptionVS(responseStr, messageDto);
                case ResponseDto.SC_ERROR_REQUEST_REPEATED: throw new RequestRepeatedExceptionVS(responseStr, messageDto);
                case ResponseDto.SC_ERROR_REQUEST: throw new BadRequestExceptionVS(responseStr, messageDto);
                case ResponseDto.SC_ERROR: throw new ServerExceptionVS(EntityUtils.toString(response.getEntity()), messageDto);
                default:throw new ExceptionVS(responseStr, messageDto);
            }
        }
    }

    public ResponseDto sendData(byte[] data, ContentType contentType,
                                String serverURL, String... headerNames) {
        LOGD(TAG + ".sendData" , "serverURL: " + serverURL + " - contentType: " + contentType);
        HttpPost httpPost = new HttpPost(serverURL);
        HttpResponse response = null;
        ResponseDto responseDto = null;
        ContentType responseContentType = null;
        try {
            ByteArrayEntity reqEntity = new ByteArrayEntity(data);
            if(contentType != null) reqEntity.setContentType(contentType.getName());
            httpPost.setEntity(reqEntity);
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost, httpContext);
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentType.getByName(header.getValue());
            LOGD(TAG + ".sendData" ,"----------------------------------------");
            LOGD(TAG + ".sendData" , response.getStatusLine().toString() + " - " +
                    response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
            LOGD(TAG + ".sendData" , "----------------------------------------");
            responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),
                    EntityUtils.toByteArray(response.getEntity()), responseContentType);
            if(headerNames != null && headerNames.length > 0) {
                List<String> headerValues = new ArrayList<>();
                for(String headerName: headerNames) {
                    Header headerValue = response.getFirstHeader(headerName);
                    if(headerValue != null) headerValues.add(headerValue.getValue());
                }
                responseDto.setData(headerValues);
            }
        } catch(ConnectTimeoutException ex) {
            responseDto = new ResponseDto(ResponseDto.SC_CONNECTION_TIMEOUT, ex.getMessage());
        }  catch(Exception ex) {
            ex.printStackTrace();
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
        }
        return responseDto;
    }

    public ResponseDto sendFile (File file, String serverURL) {
        ResponseDto responseDto = null;
        ContentType responseContentType = null;
        try {
            HttpPost httpPost = new HttpPost(serverURL);
            LOGD(TAG + ".sendFile" , "serverURL: " + httpPost.getURI()
                    + " - file: " + file.getAbsolutePath());
            FileBody fileBody = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart(SIGNED_FILE_NAME, fileBody);
            httpPost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httpPost, httpContext);
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentType.getByName(header.getValue());
            LOGD(TAG + ".sendFile" , "----------------------------------------");
            LOGD(TAG + ".sendFile" , response.getStatusLine().toString() + " - " +
                    response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
            LOGD(TAG + ".sendFile" , "----------------------------------------");
            if(ResponseDto.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),responseBytes,
                        responseContentType);
            } else {
                responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()), responseContentType);
            }
        } catch(ConnectTimeoutException ex) {
            responseDto = new ResponseDto(ResponseDto.SC_CONNECTION_TIMEOUT, ex.getMessage());
        }  catch(Exception ex) {
            ex.printStackTrace();
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
        }
        return responseDto;
    }
     
     public ResponseDto sendObjectMap(
             Map<String, Object> fileMap, String serverURL) throws Exception {
    	 LOGD(TAG + ".sendObjectMap" , "serverURL: " + serverURL);
         ResponseDto responseDto = null;
         ContentType responseContentType = null;
         if(fileMap == null || fileMap.isEmpty()) throw new Exception("Empty Map");
         HttpPost httpPost = new HttpPost(serverURL);
         HttpResponse response = null;
         try {
             Set<String> fileNames = fileMap.keySet();
             MultipartEntity reqEntity = new MultipartEntity();
             for(String objectName: fileNames){
                 Object objectToSend = fileMap.get(objectName);
                 if(objectToSend instanceof File) {
                     File file = (File)objectToSend;
                     LOGD(TAG + ".sendObjectMap" , ".sendObjectMap - fileName: " + objectName +
                             " - filePath: " + file.getAbsolutePath());  
                     FileBody  fileBody = new FileBody(file);
                     reqEntity.addPart(objectName, fileBody);
                 } else if (objectToSend instanceof byte[]) {
                     byte[] byteArray = (byte[])objectToSend;
                     reqEntity.addPart(
                             objectName, new ByteArrayBody(byteArray, objectName));
                 }
             }
             httpPost.setEntity(reqEntity);
             response = httpclient.execute(httpPost, httpContext);
             Header header = response.getFirstHeader("Content-Type");
             if(header != null) responseContentType = ContentType.getByName(header.getValue());
             LOGD(TAG + ".sendObjectMap" ,"----------------------------------------");
             LOGD(TAG + ".sendObjectMap" ,response.getStatusLine().toString() + " - " +
                     response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
             LOGD(TAG + ".sendObjectMap" ,"----------------------------------------");
             byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
             responseDto = new ResponseDto(response.getStatusLine().getStatusCode(), responseBytes,
                     responseContentType);
         } catch(ConnectTimeoutException ex) {
             ex.printStackTrace();
             responseDto = new ResponseDto(ResponseDto.SC_CONNECTION_TIMEOUT, ex.getMessage());
         }  catch(Exception ex) {
             ex.printStackTrace();
        	 String statusLine = null;
             if(response != null) {
                 statusLine = response.getStatusLine().toString();
             }
             responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
             httpPost.abort();
         }
         return responseDto;
     }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) return true;
        return false;
    }
}
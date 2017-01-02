package org.votingsystem.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.Constants;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.votingsystem.util.LogUtils.LOGD;

public class HttpConn {

    public static final String TAG = HttpConn.class.getSimpleName();

    private static HttpConn INSTANCE;
    private OkHttpClient client;

    private HttpConn() {
        if (Constants.IS_DEBUG_SESSION) {
            LOGD(TAG + ".HttpConn", "DEBUG_SESSION, Allowing unsafe connections!!!");
            client = getUnsafeOkHttpClient();
        } else
            client = new OkHttpClient();
    }

    public static HttpConn getInstance() {
        if (INSTANCE == null) INSTANCE = new HttpConn();
        return INSTANCE;
    }

    public ResponseDto doGetRequest(String url, ContentType reqContentType) {
        if (reqContentType == null) reqContentType = ContentType.TEXT;
        LOGD(TAG + ".doGetRequest", "url: " + url + " - reqContentType: " + reqContentType);
        ResponseDto responseDto = null;
        Request request = new Request.Builder().url(url)
                .addHeader("Content-Type", reqContentType.getName()).build();
        try {
            Response response = client.newCall(request).execute();
            ContentType respContentType = ContentType.getByName(response.header("Content-Type"));
            LOGD(TAG + ".doGetRequest", "status: " + response.code() + " - respContentType: " +
                    respContentType);
            responseDto = new ResponseDto(response.code(), response.body().bytes(), respContentType);
        } catch (IOException ex) {
            ex.printStackTrace();
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
        }
        return responseDto;
    }

    public ResponseDto doPostMultipartRequest(Map<String, String> fileMap, String targetURL) throws Exception {
        MultipartBody.Builder requestBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            requestBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }
        RequestBody requestBody = requestBuilder.build();
        Request request = new Request.Builder().url(targetURL).post(requestBody).build();
        ResponseDto responseDto;
        try {
            Response response = client.newCall(request).execute();
            ContentType respContentType = ContentType.getByName(response.header("Content-Type"));
            LOGD(TAG + ".doPostMultipartRequest", "status: " + response.code() +
                    " - respContentType: " + respContentType);
            responseDto = new ResponseDto(response.code(), response.body().bytes(), respContentType);
        } catch (IOException ex) {
            ex.printStackTrace();
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
        }
        return responseDto;
    }

    public ResponseDto doPostRequest(byte[] requestBytes, ContentType reqContentType, String url) {
        if (reqContentType == null)
            reqContentType = ContentType.TEXT;
        LOGD(TAG + ".doPostRequest", "url: " + url + " - reqContentType: " + reqContentType);
        RequestBody body = RequestBody.create(reqContentType.getMediaOkhttp3Type(), requestBytes);
        Request request = new Request.Builder().url(url)
                .addHeader("Content-Type", reqContentType.getName()).post(body).build();
        ResponseDto responseDto = null;
        try {
            Response response = client.newCall(request).execute();
            ContentType respContentType = ContentType.getByName(response.header("Content-Type"));
            LOGD(TAG + ".doPostRequest", "status: " + response.code() + " - respContentType: " + respContentType);
            responseDto = new ResponseDto(response.code(), response.body().bytes(), respContentType);
        } catch (IOException ex) {
            ex.printStackTrace();
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
        }
        return responseDto;
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) return true;
        return false;
    }

    public void shutdown() {
        client.dispatcher().executorService().shutdown();
    }

}

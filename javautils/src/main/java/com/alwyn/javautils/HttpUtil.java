/**
 * Implement of GET and POST request for HTTP and HTTPS
 */
package com.alwyn.javautils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtil.class);
    private static final int TIMEOUT = 30000;
    public static final String ENCODING = "UTF-8";

    /**
     * GET request for HTTP and HTTPS
     * @param address
     * @param headers
     * @param parameters
     * @param isSSL  -  true for HTTPS, false for HTTP
     * @return
     * @throws Exception
     */
    public static String get(
            String address, Map<String, String> headers, Map<String, String> parameters, boolean isSSL
    ) throws Exception {
        String url = address;
        if (parameters != null && parameters.size() > 0) {
            url = address + "?" + formatMapContent(parameters, true);
        }
        return proxyHttpRequest(url, "GET", headers, "", isSSL);
    }

    /**
     * POST reqeust for HTTP and HTTPS
     * @param address
     * @param headers
     * @param parameters  -  parameter appended in the url
     * @param requestBody  -  request body
     * @param isSSL  -  true for HTTPS, false for HTTP
     * @return
     * @throws Exception
     */
    public static String post(
            String address, Map<String, String> headers, Map<String, String> parameters,
            Map<String, String> requestBody, boolean isSSL
    ) throws Exception {
        String url = address;
        if (parameters != null && parameters.size() > 0) {
            url = address + "?" + formatMapContent(parameters, true);
        }
        String body = formatMapContent(requestBody, false);
        return proxyHttpRequest(url, "POST", headers, body, isSSL);
    }

    /**
     * Process the request
     * @param url
     * @param method
     * @param headers
     * @param body
     * @param isSSL
     * @return
     */
    private static String proxyHttpRequest(
            String url, String method, Map<String, String> headers, String body, boolean isSSL) throws Exception{
        String result = null;
        HttpURLConnection httpConnection = null;

        try{
            httpConnection = createConnection(url, method, headers, body, isSSL);
            String encoding = ENCODING;
            if (httpConnection.getContentType() != null && httpConnection.getContentType().indexOf("charset=") >= 0 ) {
                encoding = httpConnection.getContentType().substring(httpConnection.getContentType().indexOf("charset=") + 8);
            }
            result = inputStream2String(httpConnection.getInputStream(), encoding);
            LOG.info("result:");
            LOG.info(result);
        } catch (Exception e) {
            throw e;
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        return result;
    }



    /**
     * create Http connection
     *
     * @param url
     * @param method
     * @param headers
     * @param body
     *
     * @return
     * @throws Exception
     */
    private static HttpURLConnection createConnection(
            String url, String method, Map<String, String> headers, String body, boolean isSSL
    ) throws Exception {

        URL Url = new URL(url);

        // for HTTPS
        if (isSSL) {
            trustAllHttpsCertificates();
        }

        HttpURLConnection httpURLConnection = (HttpURLConnection) Url.openConnection();
        // config the connection
        httpURLConnection.setConnectTimeout(TIMEOUT);
        httpURLConnection.setRequestMethod(method);
        if (headers != null) {
            Iterator<String> iteratorHeader = headers.keySet().iterator();
            while (iteratorHeader.hasNext()) {
                String key = iteratorHeader.next();
                httpURLConnection.setRequestProperty(key, headers.get(key));
            }
        }
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + ENCODING);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);

        // write query data, only suitable for x-www-form-urlencoded now
        if (!(body == null || body.trim().equals(""))) {
            OutputStream writer = httpURLConnection.getOutputStream();
            try {
                writer.write(body.getBytes(ENCODING));
            } finally {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }
        }

        // response result
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception(responseCode + ":" + inputStream2String(httpURLConnection.getErrorStream(), ENCODING));
        }

        return httpURLConnection;
    }

    /**
     * Convert the Map to String following x-www-form-urlencoded Format
     * @param params
     * @param urlEncode
     * @return
     */
    public static String formatMapContent(Map<String, String> params, boolean urlEncode) {
        StringBuilder content = new StringBuilder();

        Iterator<String> stringIterator = params.keySet().iterator();
        while(stringIterator.hasNext()) {
            String key = stringIterator.next();
            String value = params.get(key);

            if (urlEncode) {
                try {
                    content.append(URLEncoder.encode(key, ENCODING) + "=" + URLEncoder.encode(value, ENCODING) + "&");
                } catch (UnsupportedEncodingException e) {
                    LOG.error(Arrays.toString(e.getStackTrace()));
                }
            } else {
                content.append(key + "=" + value + "&");
            }
        }

        if (content.length() == 0) {
            return "";
        }
        return content.substring(0, content.length() - 1);
    }

    /**
     *
     * @param input
     * @param encoding
     * @return
     * @throws IOException
     */
    private static String inputStream2String(InputStream input, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, encoding));
        StringBuilder result = new StringBuilder();
        String temp;
        while ((temp = reader.readLine()) != null) {
            result.append(temp);
        }

        return result.toString();
    }

    /**
     * Config for HTTPS
     * @throws Exception
     */
    private static void trustAllHttpsCertificates() throws Exception {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String str, SSLSession session) {
                return true;
            }
        });
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
                .getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
                .getSocketFactory());
    }

    /**
     *Config the Request certificate
     */
    static class miTM implements javax.net.ssl.TrustManager,javax.net.ssl.X509TrustManager {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(
                java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(
                java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }


    //====================================================================
    //=============================   TEST  ==============================
    //====================================================================
    public static void main(String[] args) {
        try {

            // the url to query info of telephone number by taobao
            String address = "https://tcc.taobao.com/cc/json/mobile_tel_segment.htm";

            Map<String, String> params = new HashMap<String, String>();
            params.put("tel", "188xxxxxxxx");

            String res = get(address, null, params, false);
            //  String res = get(address, null, params, true);
            System.out.println(res);

        } catch (Exception e) {
            LOG.error(Arrays.toString(e.getStackTrace()));
        }
    }
}

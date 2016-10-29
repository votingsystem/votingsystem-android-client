package org.votingsystem.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.bouncycastle2.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Random;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class StringUtils {
	
	public static final String TAG = StringUtils.class.getSimpleName();


    public static String getStringFromInputStream(InputStream input) throws IOException {
    	ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buf =new byte[4096];
        int len;
        while((len = input.read(buf)) > 0){
            output.write(buf,0,len);
        }
        output.close();
        input.close();
        return new String(output.toByteArray());
    }
    
    public static String randomLowerString(long seed, int size) {
        StringBuffer tmp = new StringBuffer();
        Random random = new Random(seed);
        for (int i = 0; i < size; i++) {
            long newSeed = random.nextLong();
            int currInt = (int) (26 * random.nextFloat());
            currInt += 97;
            random = new Random(newSeed);
            tmp.append((char) currInt);
        }
        return tmp.toString();
    }

    public static String truncate(String string, int length) {
        if(string == null) return null;
        else {
            if(string.length() <= length) return string;
            else return  string.substring(0, length) + "...";
        }
    }

    public static String decodeString(String string) {
    	if(string == null) return null;
    	String result = null;
        try {
        	result = URLDecoder.decode(string, Constants.UTF_8.name());
		} catch (UnsupportedEncodingException ex) {
			Log.e(TAG, ex.getMessage(), ex);
		}
    	return result;
    }

    public static String normalize (String string) {
        StringBuilder sb = new StringBuilder(string.length());
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        for (char c : string.toCharArray()) {
            if (c <= '\u007F') sb.append(c);
        }
        return sb.toString();
    }

	public static String getNormalized(String string) {
            if(string == null) return null;
            else return normalize(string).replaceAll(" ", "_").replaceAll("[\\/:.]", "");
	}

    public static String getDomain(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return  domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public static String checkURL(String url) {
        if(url == null) return null;
        url = url.trim();
        String result = null;
        if(url.startsWith("http://") || url.startsWith("https://")) result = url;
        else result = "http://" + url;
        while(result.endsWith("/")) {
            result = result.substring(0, result.length() -1);
        }
        return result;
    }

    public static String parseResource(Context context, int resource) throws IOException {
        InputStream is = context.getResources().openRawResource(resource);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            is.close();
        }
        return writer.toString();
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static String getHashBase64 (String originStr, String digestAlgorithm)
            throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
        byte[] resultDigest =  sha.digest(originStr.getBytes());
        return Base64.encodeToString(resultDigest, Base64.NO_WRAP);
    }

    public static String toHex(String base64Str) throws UnsupportedEncodingException {
        if (base64Str == null) return null;
        byte[] hexBytes = Hex.encode(base64Str.getBytes());
        return new String(hexBytes, Constants.UTF_8);
    }

    public static String getCurrencySymbol(String currencyCode) {
        switch (currencyCode) {
            case "EUR": return "€";
            case "USD": return "$";
            case "CNY":
            case "JPY":
                return "¥";
        }
        return currencyCode;
    }

}

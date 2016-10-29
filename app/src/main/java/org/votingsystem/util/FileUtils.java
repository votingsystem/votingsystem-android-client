package org.votingsystem.util;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class FileUtils {

    public static final String TAG = FileUtils.class.getSimpleName();
	
    public static byte[] getBytesFromFile(File file) throws IOException {
        byte[] b = new byte[(int) file.length()];
        FileInputStream fs = new FileInputStream(file);
        fs.read(b);
        fs.close();
        return b;
    }
    
    public static byte[] getBytesFromInputStream(InputStream input) throws IOException {
    	ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buf =new byte[4096];
        int len;
        while((len = input.read(buf)) > 0){
            output.write(buf,0,len);
        }
        output.close();
        input.close();
        return output.toByteArray();
    }

   public static File copyFileToFile(File inputFile, File outputFile) throws Exception {
       FileInputStream fs = new FileInputStream(inputFile);
       return copyStreamToFile(fs, outputFile);
    }

    public static File copyStreamToFile(InputStream input, File outputFile) throws Exception {
        OutputStream output = new FileOutputStream(outputFile);
        byte[] buf =new byte[1024];
        int len;
        while((len = input.read(buf)) > 0){
            output.write(buf,0,len);
        }
        output.close();
        input.close();
        return outputFile;
    }
	
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[(int) src.length()];
        in.read(buf);
        out.write(buf);
        in.close();
        out.close();
    }
     
    public static String getStringFromFile (File file) throws FileNotFoundException, IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        }
        finally {
            stream.close();
        }
     }

    public static void save(String content, String filePath, String fileExtension) {
        if (!(fileExtension == null || fileExtension.equals("")))
            filePath = filePath + "." + fileExtension;
        try {
            FileWriter out = new FileWriter(new File(filePath));
            out.write(content);
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static FileOutputStream openFileOutputStream(String filename, Context context) {
        LOGD(TAG + ".openFileOutputStream", "filename: " + filename);
        FileOutputStream fout = null;
        try {
            fout = context.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return fout;
    }

    public static File getFile(String filename, Context context) {
        File file = null;
        try {
            //File sdCard = Environment.getExternalStorageDirectory();
            file = new File(context.getFilesDir(), filename);
            LOGD(TAG + ".getFile", "file.getAbsolutePath(): " + file.getAbsolutePath());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return file;
    }

    public static List<File> searchFiles(String path, String fileName) {
        LOGD(TAG + ".searchFiles", "path: " + path + " - fileName: " + fileName);
        List<File> result = new ArrayList<File>();
        File root = new File(path);
        File[] list = root.listFiles();
        if (list == null) return result;
        for (File f : list ) {
            if (f.isDirectory()) {
                //Log.d(TAG + ".searchFiles", "path: " + f.getAbsoluteFile());
                result.addAll(searchFiles(f.getAbsolutePath(), fileName));
            } else {
                //Log.d(TAG + ".searchFiles", "file: " + f.getAbsoluteFile());
                if(f.getName().contains(fileName)) {
                    result.add(f);
                }
            }
        }
        return result;
    }

}

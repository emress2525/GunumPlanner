package com.tevkil.radar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** On-device ML Kit OCR. No image bytes are uploaded to our own server. */
public final class ImageTextReader {
    private ImageTextReader() {}

    /** Call on background thread. The returned text may be wrong; always let user review it. */
    public static String readLocal(Context context, Uri uri) throws Exception {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("Yalnızca Android tarafından paylaşılan görseller kabul edilir");
        InputImage input = InputImage.fromFilePath(context, uri);
        return recognize(input);
    }

    /** Only accepts images hosted by Meta CDNs, with bounded redirects and byte limits. */
    public static String readInstagramImage(String original) throws Exception {
        String current = original;
        for (int redirect = 0; redirect < 3; redirect++) {
            URL url = new URL(current);
            if (!"https".equalsIgnoreCase(url.getProtocol()) || !metaCdn(url.getHost()))
                throw new SecurityException("Görsel adresi Meta CDN alanından değil");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(14000);
            connection.setRequestProperty("Accept", "image/*");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status <= 308) {
                    String next = connection.getHeaderField("Location");
                    if (next == null || next.isEmpty()) throw new IllegalStateException("Eksik yönlendirme adresi");
                    current = new URL(url, next).toString();
                    continue;
                }
                if (status != 200) throw new IllegalStateException("Görsele erişilemiyor: HTTP " + status);
                String mime = connection.getContentType();
                if (mime == null || !mime.toLowerCase(Locale.ROOT).startsWith("image/"))
                    throw new IllegalStateException("Gelen içerik görsel değil");
                int contentLength = connection.getContentLength();
                if (contentLength > 8*1024*1024) throw new IllegalStateException("Görsel boyutu sınırı aşıldı");
                byte[] bytes = readBounded(connection.getInputStream(), 8*1024*1024);
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IllegalStateException("Görsel çözümlenemedi");
                BitmapFactory.Options options = new BitmapFactory.Options();
                int sample = 1;
                while (Math.max(bounds.outWidth/sample, bounds.outHeight/sample) > 2048) sample *= 2;
                options.inSampleSize = sample;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                if (bitmap == null) throw new IllegalStateException("Görsel okunamadı");
                try { return recognize(InputImage.fromBitmap(bitmap, 0)); }
                finally { bitmap.recycle(); }
            } finally { connection.disconnect(); }
        }
        throw new IllegalStateException("Çok fazla görsel yönlendirmesi");
    }

    private static byte[] readBounded(InputStream stream, int limit) throws Exception {
        try (InputStream input=stream; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int count;
            while ((count=input.read(buffer))!=-1) {
                if (out.size()+count>limit) throw new IllegalStateException("Görsel 8 MB sınırını aşıyor");
                out.write(buffer,0,count);
            }
            return out.toByteArray();
        }
    }

    private static boolean metaCdn(String host) {
        if (host == null) return false;
        String value = host.toLowerCase(Locale.ROOT);
        return value.equals("cdninstagram.com") || value.endsWith(".cdninstagram.com") ||
                value.equals("fbcdn.net") || value.endsWith(".fbcdn.net");
    }

    private static String recognize(InputImage input) throws Exception {
        try (TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)) {
            return Tasks.await(recognizer.process(input)).getText();
        }
    }
}

package com.corner.takecontrol.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;
import android.widget.ImageView;

public class ImageLoader {

    private static final LruCache<String, Bitmap> memoryCache;

    static {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static void loadProfileImage(String encryptedPhoto, String photoUrl, ImageView imageView, int placeholderRes) {
        // 1. Check if we have an encrypted photo in Firestore (preferred)
        // TODO: Add support for downloading and decrypting from Firebase Storage if tier is upgraded.
        if (encryptedPhoto != null && !encryptedPhoto.isEmpty()) {
            Bitmap cached = memoryCache.get(encryptedPhoto);
            if (cached != null) {
                imageView.setImageBitmap(cached);
                return;
            }

            try {
                byte[] encryptedData = Base64.decode(encryptedPhoto, Base64.NO_WRAP);
                byte[] decrypted = SecurityUtil.decryptData(encryptedData);
                if (decrypted != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decrypted, 0, decrypted.length);
                    if (bitmap != null) {
                        memoryCache.put(encryptedPhoto, bitmap);
                        imageView.setImageBitmap(bitmap);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Fallback to legacy photoUrl if it's a local URI (for immediate preview)
        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("content://")) {
            try {
                imageView.setImageURI(android.net.Uri.parse(photoUrl));
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 3. Final fallback to placeholder
        imageView.setImageResource(placeholderRes);
    }
}

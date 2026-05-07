package com.raez.storage;

import java.io.File;
import java.io.IOException;

public interface ImageStorage {
    String upload(File file) throws IOException;

    void delete(String publicId) throws IOException;

    String getPublicIdFromUrl(String url);

    static String thumbnail(String url, int w, int h) {
        if (url == null || url.isBlank()) return url;
        if (!url.contains("/upload/")) return url;
        String transform = "c_fill,w_" + w + ",h_" + h + ",q_auto,f_auto";
        return url.replace("/upload/", "/upload/" + transform + "/");
    }
}

package com.raez.util;

import com.cloudinary.Cloudinary;
import com.raez.db.DBConnection;
import com.raez.storage.CloudinaryImageStorage;
import com.raez.storage.ImageStorage;
import com.raez.storage.ImageStorageFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recovery utility — re-populates products.imageUrl + products.imagePublicId
 * from Cloudinary when the DB has been wiped but the cloud assets still exist.
 *
 * Pairs cloud assets (oldest → newest by Cloudinary {@code created_at}) with
 * the tail of the current DB products (lowest → highest productID). Works
 * because the original migration uploaded in DB-insertion order, and the
 * seed re-creates products in the same insertion order even when productIDs
 * shift.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass=com.raez.util.RestoreImagesFromCloud [-Dexec.args=--dry-run]
 */
public final class RestoreImagesFromCloud {

    private static final Logger log = LoggerFactory.getLogger(RestoreImagesFromCloud.class);

    private RestoreImagesFromCloud() {}

    public static void main(String[] args) throws Exception {
        boolean dryRun = args.length > 0 && "--dry-run".equalsIgnoreCase(args[0]);
        ImageStorage storage = ImageStorageFactory.create();
        if (!(storage instanceof CloudinaryImageStorage)) {
            log.error("Cloudinary not active (got {}). Check ~/.raez/config.properties.",
                    storage.getClass().getSimpleName());
            System.exit(2);
        }
        Cloudinary cloudinary = extractCloudinary((CloudinaryImageStorage) storage);

        List<Asset> cloudAssets = listCloudAssetsByCreatedAt(cloudinary);
        log.info("Cloudinary assets in raez/products/: {}", cloudAssets.size());
        if (cloudAssets.isEmpty()) {
            log.error("No assets found under raez/products/ — nothing to restore.");
            System.exit(2);
        }

        // Match cloud assets positionally to the TAIL of current DB products
        // ordered by productID. The original migration uploaded products in
        // insertion order, so the Nth-oldest cloud asset corresponds to the
        // Nth-from-last currently-seeded product (the seed re-creates products
        // in the same insertion order, even if productIDs differ).
        Connection conn = DBConnection.getInstance().getConnection();
        List<DbProduct> dbProducts = loadDbProducts(conn);
        log.info("DB products: {}", dbProducts.size());

        if (dbProducts.size() < cloudAssets.size()) {
            log.warn("DB has {} products but {} cloud assets — using the OLDEST {} cloud assets " +
                     "(skipping {} extras presumed from later uploads).",
                     dbProducts.size(), cloudAssets.size(), dbProducts.size(),
                     cloudAssets.size() - dbProducts.size());
            cloudAssets = cloudAssets.subList(0, dbProducts.size());
        }
        // Pair the LAST N DB products with the N cloud assets in order.
        int n = cloudAssets.size();
        List<DbProduct> tail = dbProducts.subList(dbProducts.size() - n, dbProducts.size());

        int updated = 0;
        for (int i = 0; i < n; i++) {
            DbProduct p = tail.get(i);
            Asset asset = cloudAssets.get(i);
            log.info("MATCH productID={} ({}) → {} [{}]",
                    p.id, p.name, asset.publicId, asset.createdAt);
            if (dryRun) continue;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET imageUrl = ?, imagePublicId = ? WHERE productID = ?")) {
                ps.setString(1, asset.secureUrl);
                ps.setString(2, asset.publicId);
                ps.setInt(3, p.id);
                if (ps.executeUpdate() > 0) updated++;
            }
        }

        if (dryRun) {
            log.info("Dry run complete. Re-run without --dry-run to write {} updates.", n);
        } else {
            log.info("Done. {} restored.", updated);
        }
    }

    private record DbProduct(int id, String name) {}

    private static List<DbProduct> loadDbProducts(Connection conn) throws java.sql.SQLException {
        List<DbProduct> out = new ArrayList<>();
        try (var st = conn.createStatement();
             var rs = st.executeQuery("SELECT productID, name FROM products ORDER BY productID ASC")) {
            while (rs.next()) {
                out.add(new DbProduct(rs.getInt("productID"), rs.getString("name")));
            }
        }
        return out;
    }

    private record Asset(String publicId, String secureUrl, String createdAt) {}

    /**
     * Lists every asset under {@code raez/products/} sorted ASC by Cloudinary's
     * {@code created_at}. Positional ordering is the recovery key — the
     * original migration uploaded products in DB-insertion order, so the
     * Nth-oldest asset corresponds to the Nth product (from the bottom) in
     * the current DB ordered by productID.
     */
    private static List<Asset> listCloudAssetsByCreatedAt(Cloudinary cloudinary) throws Exception {
        List<Asset> out = new ArrayList<>();
        String nextCursor = null;
        do {
            Map<String, Object> params = new HashMap<>();
            params.put("type", "upload");
            params.put("prefix", "raez/products/");
            params.put("max_results", 500);
            if (nextCursor != null) params.put("next_cursor", nextCursor);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = cloudinary.api().resources(params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resources =
                    (List<Map<String, Object>>) resp.getOrDefault("resources", List.of());
            for (Map<String, Object> r : resources) {
                String publicId = String.valueOf(r.get("public_id"));
                String secureUrl = String.valueOf(r.getOrDefault("secure_url", r.get("url")));
                String createdAt = r.get("created_at") == null ? "" : String.valueOf(r.get("created_at"));
                out.add(new Asset(publicId, secureUrl, createdAt));
            }
            nextCursor = resp.get("next_cursor") == null ? null : String.valueOf(resp.get("next_cursor"));
        } while (nextCursor != null);
        out.sort(Comparator.comparing(a -> a.createdAt));
        return out;
    }

    /**
     * Pulls the underlying Cloudinary client out of CloudinaryImageStorage.
     * Avoids changing CloudinaryImageStorage's public surface just for this util.
     */
    private static Cloudinary extractCloudinary(CloudinaryImageStorage storage) throws Exception {
        Field f = CloudinaryImageStorage.class.getDeclaredField("cloudinary");
        f.setAccessible(true);
        return (Cloudinary) f.get(storage);
    }
}

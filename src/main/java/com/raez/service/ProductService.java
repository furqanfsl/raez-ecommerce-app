package com.raez.service;

import com.raez.dao.CategoryDAO;
import com.raez.dao.ImageDAO;
import com.raez.dao.InventoryDAO;
import com.raez.dao.ProductDAO;
import com.raez.model.Category;
import com.raez.model.Product;
import com.raez.model.ProductImage;
import com.raez.storage.ImageStorage;
import com.raez.storage.ImageStorageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductService {

    private final ProductDAO   productDAO   = new ProductDAO();
    private final CategoryDAO  categoryDAO  = new CategoryDAO();
    private final ImageDAO     imageDAO     = new ImageDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private static final ImageStorage IMAGE_STORAGE = ImageStorageFactory.create();

    // ── Read ─────────────────────────────────────────────

    public List<Product> getAll() {
        List<Product> products = productDAO.getAll();
        attachDetails(products);
        return products;
    }

    public List<Product> getActive() {
        List<Product> products = productDAO.getActive();
        attachDetails(products);
        return products;
    }

    public Product getById(int id) {
        Product p = productDAO.getById(id);
        if (p != null) attachDetails(List.of(p));
        return p;
    }

    public boolean isEmpty() {
        return productDAO.isEmpty();
    }

    // ── Write ────────────────────────────────────────────

    public Product add(Product p, List<String> categoryNames,
                       List<String> imageUrls) throws Exception {
        validate(p);
        p.imagePath = firstImagePath(imageUrls);

        if (p.sku == null || p.sku.isEmpty())
            p.sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        int id = productDAO.insert(p);
        if (id < 0) throw new Exception("Failed to insert product.");
        p.productID = id;

        saveCategories(id, categoryNames);
        saveImages(id, imageUrls);
        inventoryDAO.setStock(id, p.stock);

        return getById(id);
    }

    public Product update(Product p, List<String> categoryNames,
                          List<String> imageUrls) throws Exception {
        validate(p);
        Product existing = productDAO.getById(p.productID);
        String oldPublicIdToDelete = null;
        if (imageUrls != null && imageUrls.stream().anyMatch(s -> s != null && !s.trim().isEmpty())) {
            p.imagePath = firstImagePath(imageUrls);
            // Cloud-asset replacement: stage old publicId for deletion only after the new is committed.
            if (existing != null
                && existing.imagePublicId != null && !existing.imagePublicId.isBlank()
                && p.imagePublicId != null && !p.imagePublicId.isBlank()
                && !existing.imagePublicId.equals(p.imagePublicId)) {
                oldPublicIdToDelete = existing.imagePublicId;
            }
        } else {
            p.imagePath = existing != null ? existing.imagePath : null;
        }

        if (!productDAO.update(p))
            throw new Exception("Failed to update product.");

        if (oldPublicIdToDelete != null) {
            try {
                IMAGE_STORAGE.delete(oldPublicIdToDelete);
            } catch (Exception ex) {
                System.err.println("Old image delete failed (publicId=" + oldPublicIdToDelete
                    + "): " + ex.getMessage());
            }
        }

        categoryDAO.unlinkAllForProduct(p.productID);
        saveCategories(p.productID, categoryNames);

        // Preserve existing image rows when no image payload is provided from the form.
        if (imageUrls != null && imageUrls.stream().anyMatch(s -> s != null && !s.trim().isEmpty())) {
            imageDAO.deleteAllForProduct(p.productID);
            saveImages(p.productID, imageUrls);
        }
        inventoryDAO.setStock(p.productID, p.stock);

        // Auto-deactivate if stock is 0, re-activate if stock restored
        autoCheckStatus(p.productID, p.stock, p.name);

        return getById(p.productID);
    }

    public boolean delete(int id) {
        inventoryDAO.deleteForProduct(id);
        return productDAO.delete(id);
    }

    public boolean toggleStatus(int id, String currentStatus) {
        String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";
        return productDAO.setStatus(id, newStatus);
    }

    // ── Auto status logic ────────────────────────────────

    /**
     * If stock == 0  → force INACTIVE
     * If stock  > 0  → force ACTIVE (restores products when stock is added back)
     */
    private void autoCheckStatus(int productId, int stock, String name) {
        if (stock == 0) {
            productDAO.setStatus(productId, "INACTIVE");
            System.out.println("Auto-INACTIVE: '" + name + "' (stock = 0)");
        } else {
            productDAO.setStatus(productId, "ACTIVE");
            System.out.println("Auto-ACTIVE: '" + name + "' (stock = " + stock + ")");
        }
    }

    // ── Seed Data ────────────────────────────────────────

    public void loadSeedData() {
        ensureCategory("Home Assistants", "Robots that help around the home");
        ensureCategory("Security Bots",   "Robots for home and business security");
        ensureCategory("Educational",     "Robots designed for learning");
        ensureCategory("Companions",      "Social and companion robots");
        ensureCategory("Industrial",      "Industrial and professional robots");

        for (Product p : productDAO.getAll()) {
            inventoryDAO.deleteForProduct(p.productID);
            productDAO.delete(p.productID);
        }

        // {sku, name, description, price, stock, category, imageUrl}
        Object[][] seeds = {
            {"ARIA-2000",  "Aria Home Assistant",     "Advanced home assistant robot with voice control and AI learning.",             299.99, 15, "Home Assistants", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"BOLT-X1",    "Bolt Security Bot",       "Professional security robot with 360° camera and night vision.",               599.99, 8,  "Security Bots",   "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"SPARK-EDU",  "Spark Educational Robot", "Interactive educational robot for children aged 6-14.",                        149.99, 22, "Educational",     "https://images.unsplash.com/photo-1561144257-e32e8506bda8?w=400"},
            {"BUDDY-V3",   "Buddy Companion Robot",   "Social companion robot with emotional recognition and conversational AI.",      449.99, 11, "Companions",      "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"TITAN-IND",  "Titan Industrial Bot",    "Heavy-duty industrial robot for warehouse and manufacturing.",                1299.99, 4,  "Industrial",      "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"NOVA-HOME",  "Nova Smart Home Bot",     "Smart home automation robot with app connectivity.",                           199.99, 30, "Home Assistants", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"GUARD-PRO",  "Guardian Pro Security",   "Enterprise-grade security robot with facial recognition.",                     899.99, 6,  "Security Bots",   "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"LEARN-BOT",  "LearnBot Junior",         "Fun robot that teaches coding and STEM concepts.",                              99.99, 45, "Educational",     "https://images.unsplash.com/photo-1561144257-e32e8506bda8?w=400"},
            {"ECHO-C1",    "Echo Companion",          "Emotionally intelligent companion robot for elderly care.",                    549.99, 9,  "Companions",      "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"FORGE-X",    "Forge Industrial X",      "Precision industrial robot for assembly line automation.",                    1599.99, 3,  "Industrial",      "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"CASA-BOT",   "CasaBot Home Helper",     "Compact home assistant that manages chores and schedules.",                    349.99, 18, "Home Assistants", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"SENTRY-V2",  "Sentry V2",               "Outdoor patrol robot with weatherproof design and GPS.",                       749.99, 7,  "Security Bots",   "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"ROBO-STEM",  "RoboSTEM Pro",            "Advanced educational robot for university STEM programs.",                      249.99, 14, "Educational",     "https://images.unsplash.com/photo-1561144257-e32e8506bda8?w=400"},
            {"PALS-BOT",   "PalsBot Social",          "Friendly social robot for children with autism support.",                      399.99, 12, "Companions",      "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"MECH-ARM",   "MechArm Pro",             "6-axis robotic arm for precision industrial tasks.",                            999.99, 5,  "Industrial",      "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"HELPA-1",    "Helpa Home One",          "Entry-level home assistant with basic automation features.",                    149.99, 25, "Home Assistants", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"WATCH-BOT",  "WatchBot Home Security",  "Compact home security robot with live streaming.",                             299.99, 20, "Security Bots",   "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"CODE-BOT",   "CodeBot Educator",        "Robot that teaches programming through play.",                                  179.99, 33, "Educational",     "https://images.unsplash.com/photo-1561144257-e32e8506bda8?w=400"},
            {"VERA-3",     "Vera Companion III",      "Third-gen AI companion with memory and personality learning.",                  649.99, 8,  "Companions",      "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"LIFT-BOT",   "LiftBot Warehouse",       "Automated warehouse robot for picking and packing.",                            799.99, 10, "Industrial",      "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"SMART-H2",   "SmartHome 2.0",           "Second-gen home robot with improved obstacle avoidance.",                       279.99, 16, "Home Assistants", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"PATROL-X",   "PatrolBot X",             "AI-powered patrol robot with anomaly detection.",                               499.99, 13, "Security Bots",   "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"TINY-BOT",   "TinyBot Kids",            "Child-safe educational robot with storytelling.",                                79.99, 50, "Educational",     "https://images.unsplash.com/photo-1561144257-e32e8506bda8?w=400"},
            {"SOC-BOT",    "SocBot Companion",        "Social robot for mental health support.",                                       479.99, 9,  "Companions",      "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
            {"DELTA-IND",  "Delta Industrial Pro",    "High-speed delta robot for food and pharmaceutical industries.",               1199.99, 4,  "Industrial",      "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"},
            {"ZARA-HOME",  "Zara Smart Assistant",    "Premium home assistant with multilingual support.",                             399.99, 21, "Home Assistants", "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=400"},
        };

        for (Object[] s : seeds) {
            try {
                Product p = new Product(
                    (String) s[0], (String) s[1], (String) s[2],
                    (double) s[3], 0.0, "active", null
                );
                p.stock = (int) s[4];
                add(p, List.of((String) s[5]), List.of((String) s[6]));
            } catch (Exception e) {
                System.err.println("Seed failed for " + s[1] + ": " + e.getMessage());
            }
        }
        System.out.println("Seed data loaded successfully.");
    }

    // ── Private helpers ──────────────────────────────────

    private void attachDetails(List<Product> products) {
        java.util.Map<Integer, double[]> ratingsMap = productDAO.getRatingsMap();
        for (Product p : products) {
            p.categories = categoryDAO.getByProduct(p.productID);
            p.images     = imageDAO.getByProduct(p.productID);
            p.stock      = inventoryDAO.getStock(p.productID);
            double[] rating = ratingsMap.get(p.productID);
            if (rating != null) {
                p.avgRating   = rating[0];
                p.reviewCount = (int) rating[1];
            }
        }
    }

    private void saveCategories(int productId, List<String> categoryNames) {
        if (categoryNames == null) return;
        for (String name : categoryNames) {
            if (name == null || name.trim().isEmpty()) continue;
            Category cat = categoryDAO.findByName(name);
            if (cat == null) {
                int catId = categoryDAO.insert(new Category(name, "", 1));
                categoryDAO.linkProductCategory(productId, catId);
            } else {
                categoryDAO.linkProductCategory(productId, cat.categoryID);
            }
        }
    }

    private void saveImages(int productId, List<String> imageUrls) {
        if (imageUrls == null) return;
        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            if (url == null || url.trim().isEmpty()) continue;
            imageDAO.insert(new ProductImage(productId, url, i == 0 ? 1 : 0));
        }
    }

    private String firstImagePath(List<String> imageUrls) {
        if (imageUrls == null) return null;
        for (String image : imageUrls) {
            if (image != null && !image.trim().isEmpty()) return image.trim();
        }
        return null;
    }

    private void ensureCategory(String name, String description) {
        if (categoryDAO.findByName(name) == null)
            categoryDAO.insert(new Category(name, description, 1));
    }

    private void validate(Product p) throws Exception {
        List<String> errors = new ArrayList<>();
        if (p.name == null || p.name.trim().isEmpty())
            errors.add("Product name is required.");
        if (p.price <= 0)
            errors.add("Price must be a positive number.");
        if (p.stock < 0)
            errors.add("Stock must be a non-negative integer.");
        if (!errors.isEmpty())
            throw new Exception(String.join("\n", errors));
    }
}

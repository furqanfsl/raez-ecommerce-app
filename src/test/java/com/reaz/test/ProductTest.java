package com.reaz.test;

import com.reaz.dao.InventoryDAO;
import com.reaz.dao.ProductDAO;
import com.reaz.model.Product;
import com.reaz.service.ProductService;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * JUnit 5 Tests for RAEZ Product Component
 * Covers T1–T13 of the test plan
 *
 * Setup in Eclipse:
 * 1. Add JUnit 5 to pom.xml (see pom_junit_additions.xml)
 * 2. Place this file in src/test/java/com/reaz/test/
 * 3. Right-click → Run As → JUnit Test
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductTest {

    private static ProductService  service;
    private static ProductDAO      productDAO;
    private static InventoryDAO    inventoryDAO;
    private static int             savedId = -1;

    @BeforeAll
    static void setup() {
        service      = new ProductService();
        productDAO   = new ProductDAO();
        inventoryDAO = new InventoryDAO();
    }

    // ── T1: Add valid product ─────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("T1 - Add product with valid data")
    void T1_addValidProduct() throws Exception {
        Product p  = new Product();
        p.name     = "JUnit Test Robot";
        p.price    = 299.99;
        p.stock    = 10;
        p.status   = "ACTIVE";

        Product saved = service.add(p, List.of("Home Assistants"), List.of("https://example.com/img.jpg"));

        assertNotNull(saved);
        assertTrue(saved.id > 0);
        assertEquals("JUnit Test Robot", saved.name);
        assertEquals(299.99, saved.price, 0.01);
        assertEquals(10, saved.stock);
        assertFalse(saved.sku.isEmpty());

        savedId = saved.id;
        System.out.println("T1 PASS — Product added with ID: " + savedId);
    }

    // ── T2: Reject empty name ─────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("T2 - Reject product with empty name")
    void T2_rejectEmptyName() {
        Product p = new Product();
        p.name    = "";
        p.price   = 99.99;
        p.stock   = 5;

        Exception ex = assertThrows(Exception.class, () ->
            service.add(p, List.of("Educational"), List.of())
        );

        assertTrue(ex.getMessage().contains("name is required"));
        System.out.println("T2 PASS — Empty name rejected: " + ex.getMessage());
    }

    // ── T3: Reject negative price ─────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("T3 - Reject product with negative price")
    void T3_rejectNegativePrice() {
        Product p = new Product();
        p.name    = "Bad Robot";
        p.price   = -50.00;
        p.stock   = 5;

        Exception ex = assertThrows(Exception.class, () ->
            service.add(p, List.of("Industrial"), List.of())
        );

        assertTrue(ex.getMessage().contains("positive"));
        System.out.println("T3 PASS — Negative price rejected: " + ex.getMessage());
    }

    // ── T4: Reject negative stock ─────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("T4 - Reject product with negative stock")
    void T4_rejectNegativeStock() {
        Product p = new Product();
        p.name    = "Bad Stock Robot";
        p.price   = 99.99;
        p.stock   = -1;

        Exception ex = assertThrows(Exception.class, () ->
            service.add(p, List.of("Security Bots"), List.of())
        );

        assertTrue(ex.getMessage().contains("non-negative"));
        System.out.println("T4 PASS — Negative stock rejected: " + ex.getMessage());
    }

    // ── T5: Auto-inactive when stock = 0 ─────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("T5 - Stock = 0 auto-sets product INACTIVE")
    void T5_autoInactiveWhenStockZero() throws Exception {
        Product p = new Product();
        p.name    = "Zero Stock Robot";
        p.price   = 149.99;
        p.stock   = 0;

        Product saved = service.add(p, List.of("Companions"), List.of());

        assertEquals("INACTIVE", saved.status);
        assertEquals(0, saved.stock);

        service.delete(saved.id);
        System.out.println("T5 PASS — Stock=0 product auto-set INACTIVE");
    }

    // ── T6: Edit product ──────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("T6 - Edit product updates all fields")
    void T6_editProduct() throws Exception {
        assertTestProductExists();

        Product updated      = new Product();
        updated.id           = savedId;
        updated.name         = "Updated JUnit Robot";
        updated.description  = "Updated via JUnit test";
        updated.price        = 399.99;
        updated.stock        = 20;
        updated.status       = "ACTIVE";

        Product result = service.update(updated,
            List.of("Security Bots"),
            List.of("https://example.com/updated.jpg"));

        assertEquals("Updated JUnit Robot", result.name);
        assertEquals(399.99, result.price, 0.01);
        assertEquals(20, result.stock);
        System.out.println("T6 PASS — Product updated successfully");
    }

    // ── T7: Delete product ────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("T7 - Delete product removes from database")
    void T7_deleteProduct() throws Exception {
        // Create a separate product to delete
        Product p = new Product();
        p.name    = "Delete Me Robot";
        p.price   = 99.99;
        p.stock   = 5;
        Product saved = service.add(p, List.of("Educational"), List.of());

        boolean deleted = service.delete(saved.id);
        assertTrue(deleted);

        Product fetched = service.getById(saved.id);
        assertNull(fetched);
        System.out.println("T7 PASS — Product deleted and not retrievable");
    }

    // ── T8: Toggle status ─────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("T8 - Toggle ACTIVE to INACTIVE and back")
    void T8_toggleStatus() throws Exception {
        assertTestProductExists();

        productDAO.setStatus(savedId, "ACTIVE");

        service.toggleStatus(savedId, "ACTIVE");
        assertEquals("INACTIVE", service.getById(savedId).status);

        service.toggleStatus(savedId, "INACTIVE");
        assertEquals("ACTIVE", service.getById(savedId).status);

        System.out.println("T8 PASS — Status toggled both ways correctly");
    }

    // ── T9: Multiple categories ───────────────────────────────────────────
    @Test
    @Order(9)
    @DisplayName("T9 - Assign product to multiple categories")
    void T9_multipleCategories() throws Exception {
        Product p = new Product();
        p.name    = "Multi-Cat Robot";
        p.price   = 199.99;
        p.stock   = 5;

        Product saved = service.add(p,
            List.of("Home Assistants", "Security Bots"), List.of());

        assertEquals(2, saved.categories.size());

        List<String> names = saved.categories.stream().map(c -> c.name).toList();
        assertTrue(names.contains("Home Assistants"));
        assertTrue(names.contains("Security Bots"));

        service.delete(saved.id);
        System.out.println("T9 PASS — Product assigned to 2 categories");
    }

    // ── T10: Multiple images ──────────────────────────────────────────────
    @Test
    @Order(10)
    @DisplayName("T10 - Multiple images stored, first is primary")
    void T10_multipleImages() throws Exception {
        Product p = new Product();
        p.name    = "Multi-Image Robot";
        p.price   = 299.99;
        p.stock   = 8;

        Product saved = service.add(p, List.of("Industrial"), List.of(
            "https://example.com/img1.jpg",
            "https://example.com/img2.jpg",
            "https://example.com/img3.jpg"
        ));

        assertEquals(3, saved.images.size());
        assertTrue(saved.images.get(0).isPrimary);
        assertFalse(saved.images.get(1).isPrimary);
        assertEquals("https://example.com/img1.jpg", saved.getPrimaryImage());

        service.delete(saved.id);
        System.out.println("T10 PASS — 3 images stored, first is primary");
    }

    // ── T11: Insert and retrieve by ID ────────────────────────────────────
    @Test
    @Order(11)
    @DisplayName("T11 - Insert returns valid ID, getById retrieves it")
    void T11_insertAndGetById() {
        Product p = new Product("JUNIT-SKU-001", "DAO Robot", "desc", 149.99, "ACTIVE");
        int id    = productDAO.insert(p);

        assertTrue(id > 0);

        Product fetched = productDAO.getById(id);
        assertNotNull(fetched);
        assertEquals("DAO Robot", fetched.name);
        assertEquals(149.99, fetched.price, 0.01);

        productDAO.delete(id);
        System.out.println("T11 PASS — Product inserted with ID " + id + " and retrieved");
    }

    // ── T12: getActive only returns ACTIVE ────────────────────────────────
    @Test
    @Order(12)
    @DisplayName("T12 - getActive() returns only ACTIVE products")
    void T12_getActiveOnly() {
        List<Product> active = service.getActive();

        assertNotNull(active);
        for (Product p : active) {
            assertEquals("ACTIVE", p.status,
                "Found non-ACTIVE product: " + p.name + " = " + p.status);
        }
        System.out.println("T12 PASS — getActive() returned " + active.size() + " ACTIVE products");
    }

    // ── T13: Stock in inventory_record ────────────────────────────────────
    @Test
    @Order(13)
    @DisplayName("T13 - Stock stored and retrieved from inventory_record")
    void T13_stockInInventory() throws Exception {
        Product p = new Product();
        p.name    = "Stock Test Robot";
        p.price   = 99.99;
        p.stock   = 42;

        Product saved = service.add(p, List.of("Educational"), List.of());

        assertEquals(42, saved.stock);
        assertEquals(42, inventoryDAO.getStock(saved.id));

        service.delete(saved.id);
        System.out.println("T13 PASS — Stock=42 correctly stored in inventory_record");
    }

    // ── Cleanup ───────────────────────────────────────────────────────────
    @AfterAll
    static void cleanup() {
        if (savedId > 0) {
            service.delete(savedId);
            System.out.println("Cleanup — test product deleted");
        }
    }

    private void assertTestProductExists() {
        if (savedId <= 0) fail("T1 must run first to create the test product.");
    }
}
package com.raez;

import com.raez.dao.ProductDAO;
import com.raez.model.Product;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductDaoTest {

    private final ProductDAO dao = new ProductDAO();

    @BeforeAll
    static void boot() {
        TestDb.init();
    }

    @Test
    void findByCategory_returnsOnlyMatching() {
        // category 1 = Home Assistants (per seed). Multiple products live there.
        List<Product> homeAssistants = dao.findByCategory(1);
        assertFalse(homeAssistants.isEmpty(), "seed should put at least one product in category 1");
        for (Product p : homeAssistants) {
            assertEquals(Integer.valueOf(1), p.categoryID,
                    "findByCategory(1) returned a product with categoryID=" + p.categoryID + " (" + p.name + ")");
        }
    }
}

package com.raez.model;

import com.raez.dao.FavouritesDAO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Singleton that holds the user's favourited products.
 * In-memory for guests; DB-backed when a customer is logged in.
 */
public class FavouritesManager {

    private static FavouritesManager instance;

    private final Map<Integer, Product>            favourites = new LinkedHashMap<>();
    private final List<Consumer<List<Product>>>    listeners  = new ArrayList<>();
    private final FavouritesDAO                    dao        = new FavouritesDAO();

    private int currentCustomerId = -1;

    private FavouritesManager() {}

    public static FavouritesManager getInstance() {
        if (instance == null) instance = new FavouritesManager();
        return instance;
    }

    // ── Session management ─────────────────────────────────────────────────

    /** Called when a customer logs in. Loads their saved favourites from the DB. */
    public void loadForCustomer(int customerID) {
        this.currentCustomerId = customerID;
        favourites.clear();
        List<Product> saved = dao.loadFavouriteProducts(customerID);
        for (Product p : saved) favourites.put(p.productID, p);
        notifyListeners();
    }

    /** Called on logout — clears in-memory state without touching the DB. */
    public void clearUser() {
        this.currentCustomerId = -1;
        favourites.clear();
        notifyListeners();
    }

    // ── Core operations ────────────────────────────────────────────────────

    public void add(Product p) {
        favourites.put(p.productID, p);
        notifyListeners();
    }

    public void remove(int productId) {
        favourites.remove(productId);
        notifyListeners();
    }

    /** Toggle — returns true if product is now favourited, false if removed. */
    public boolean toggle(Product p) {
        boolean nowFav;
        if (favourites.containsKey(p.productID)) {
            remove(p.productID);
            nowFav = false;
        } else {
            add(p);
            nowFav = true;
        }
        // Persist to DB when customer is logged in
        if (currentCustomerId >= 0) {
            if (nowFav) dao.add(currentCustomerId, p.productID);
            else        dao.remove(currentCustomerId, p.productID);
        }
        return nowFav;
    }

    public boolean isFavourite(int productId) {
        return favourites.containsKey(productId);
    }

    public List<Product> getAll() {
        return new ArrayList<>(favourites.values());
    }

    public int getCount() { return favourites.size(); }

    public void addListener(Consumer<List<Product>> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<List<Product>> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        List<Product> all = getAll();
        for (Consumer<List<Product>> l : listeners) l.accept(all);
    }
}

package com.reaz.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Singleton that holds the user's favourited products.
 * Controllers subscribe to changes via addListener().
 */
public class FavouritesManager {

    private static FavouritesManager instance;

    // productId → Product (LinkedHashMap keeps insertion order)
    private final Map<Integer, Product> favourites = new LinkedHashMap<>();

    // Listeners notified whenever favourites change
    private final List<Consumer<List<Product>>> listeners = new ArrayList<>();

    private FavouritesManager() {}

    public static FavouritesManager getInstance() {
        if (instance == null) instance = new FavouritesManager();
        return instance;
    }

    /** Add a product to favourites */
    public void add(Product p) {
        favourites.put(p.productID, p);
        notifyListeners();
    }

    /** Remove a product from favourites */
    public void remove(int productId) {
        favourites.remove(productId);
        notifyListeners();
    }

    /** Toggle — add if not present, remove if present */
    public boolean toggle(Product p) {
        if (favourites.containsKey(p.productID)) {
            remove(p.productID);
            return false; // now not favourited
        } else {
            add(p);
            return true; // now favourited
        }
    }

    /** Check if a product is favourited */
    public boolean isFavourite(int productId) {
        return favourites.containsKey(productId);
    }

    /** Get all favourited products */
    public List<Product> getAll() {
        return new ArrayList<>(favourites.values());
    }

    /** Get count of favourites */
    public int getCount() {
        return favourites.size();
    }

    /** Subscribe to changes */
    public void addListener(Consumer<List<Product>> listener) {
        listeners.add(listener);
    }

    /** Unsubscribe */
    public void removeListener(Consumer<List<Product>> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        List<Product> all = getAll();
        for (Consumer<List<Product>> l : listeners) l.accept(all);
    }
}
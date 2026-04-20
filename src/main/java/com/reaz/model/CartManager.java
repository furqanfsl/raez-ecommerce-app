package com.reaz.model;

import java.util.*;
import java.util.function.Consumer;

public class CartManager {
    private static CartManager instance;
    private final Map<Integer, CartItem> items = new LinkedHashMap<>();
    private final List<Consumer<Map<Integer, CartItem>>> listeners = new ArrayList<>();

    private CartManager() {}

    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    public void addItem(int productId, String productName, double price) {
        if (items.containsKey(productId)) {
            items.get(productId).quantity++;
        } else {
            items.put(productId, new CartItem(productId, productName, price, 1));
        }
        notifyListeners();
    }

    public void removeItem(int productId) {
        items.remove(productId);
        notifyListeners();
    }

    public void clear() {
        items.clear();
        notifyListeners();
    }

    public Map<Integer, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public int getTotalCount() {
        return items.values().stream().mapToInt(i -> i.quantity).sum();
    }

    public boolean isEmpty() { return items.isEmpty(); }

    public void addListener(Consumer<Map<Integer, CartItem>> listener) {
        listeners.add(listener);
        listener.accept(Collections.unmodifiableMap(new LinkedHashMap<>(items)));
    }

    private void notifyListeners() {
        Map<Integer, CartItem> snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(items));
        listeners.forEach(l -> l.accept(snapshot));
    }

    public static class CartItem {
        public final int    productId;
        public final String productName;
        public final double price;
        public int          quantity;

        public CartItem(int productId, String productName, double price, int quantity) {
            this.productId   = productId;
            this.productName = productName;
            this.price       = price;
            this.quantity    = quantity;
        }
    }
}

package com.raez.controllers;

import com.raez.model.NavigationRouter;
import javafx.fxml.FXML;

public class AppFooterController {

    @FXML
    private void handleAdminLogin() {
        AdminLoginModalLauncher.show();
    }

    @FXML private void handleShopAll() {
        NavigationRouter.getInstance().navigateTo("/fxml/ProductHomepage.fxml");
    }

    @FXML private void handleShopRobots() {
        com.raez.controllers.ProductListController.setPendingCategory("Robots");
        NavigationRouter.getInstance().navigateTo("/fxml/ProductListPage.fxml");
    }

    @FXML private void handleShopMiniRobots() {
        com.raez.controllers.ProductListController.setPendingCategory("Mini Robots");
        NavigationRouter.getInstance().navigateTo("/fxml/ProductListPage.fxml");
    }

    @FXML private void handleShopAccessories() {
        com.raez.controllers.ProductListController.setPendingCategory("Accessories");
        NavigationRouter.getInstance().navigateTo("/fxml/ProductListPage.fxml");
    }

    @FXML private void handleShopServices() {
        com.raez.controllers.ProductListController.setPendingCategory("Services");
        NavigationRouter.getInstance().navigateTo("/fxml/ProductListPage.fxml");
    }
}

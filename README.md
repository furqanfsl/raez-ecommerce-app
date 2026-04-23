# 🚀 RAEZ E-Commerce & ERP - Internal Developer Guide

Welcome to the central repository for the RAEZ Desktop Application! 

This document outlines our internal development workflow, architecture rules, and Git strategy. Because we are integrating 7 different modules into *one single application*, it is critical that everyone follows these guidelines to prevent merge conflicts and crashes.

---

## 🏗️ 1. Architecture Overview
We are building a unified JavaFX desktop application. It consists of two main sides, powered by a single SQLite database:
1. *The Customer Storefront:* The default view when the app launches. No login required for browsing.
2. *The Admin/Back-Office Shell:* Accessed via a hidden login button. It uses Role-Based Access Control (RBAC) to load specific dashboards (Finance, Inventory, HR, etc.) based on the user's role.

---

## 🌳 2. Git Workflow (How to Contribute)
The main branch is locked to prevent accidental overwrites. *You cannot push directly to main.* To add your code to the master project, follow this workflow:

1. *Pull the latest master:* git pull origin main
2. *Create a new branch for your work:* git checkout -b feature/your-module-name (e.g., feature/inventory-dashboard)
3. *Write and test your code locally.*
4. *Stage and Commit:* git add . then git commit -m "feat: added inventory views"
5. *Push your branch:* git push origin feature/your-module-name
6. *Open a Pull Request (PR):* Go to GitHub, open a PR to merge your branch into main, and request a review. Once approved, your code will be merged!

---

## 🗄️ 3. Database Rules
We are using a single company_master.db SQLite file. 
* *DO NOT commit .db files to GitHub.* Our .gitignore is set up to block them so we don't overwrite each other's test data.
* *How to set up your DB:* Pull the latest code, find the master_schema.sql file in the root directory, and run it locally to generate your own empty .db file on your machine.
* *Need to change a table?* Do not edit the DB directly. Update the master_schema.sql file in your branch and note the change in your Pull Request.

---

## 🧩 4. Integration Coding Standards
To ensure your module snaps perfectly into the Master Shell, follow these rules:

### A. Namespacing & File Names
To prevent Java from crashing due to duplicate file names, everything you write must be specific to your module.
* *Java Packages:* Place your controllers, DAOs, and models in your specific package (e.g., com.raez.inventory.controller).
* *FXML Files:* Prefix your FXML files with your module name (e.g., InventoryDashboard.fxml, NOT Dashboard.fxml).

### B. Use the Shared Singletons
Do not create your own database connection or login logic.
* *Database:* Use the global DatabaseConnection.getInstance() to run your queries.
* *Session/Auth:* Use SessionManager.getCurrentUser() to check who is logged in and what their role is.

### C. FXML Margins (Scene Builder Bug)
*Important:* Do NOT use <VBox.margin> or <HBox.margin> directly inside <StackPane>, <Circle>, or <SVGPath> elements. This triggers a known bug that crashes Scene Builder. Use an empty <Region> or translateY for spacing instead.

---

## 🎯 Current Phase: Integration (Phase 2)
*Immediate Action Items for all members:*
1. Clone this repository.
2. Create your feature branch.
3. Migrate your local .java and .fxml files into the correct package structure in this repo.
4. Update your DAOs to use the new unified Users table and the shared DatabaseConnection.
5. Open your first Pull Request!

If you are stuck on Git commands or how to wire your module into the Master Shell, ask in the Teams channel!

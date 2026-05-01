package com.raez.finance.controller;

// ── All existing imports preserved ──────────────────────────────────────
import com.raez.finance.dao.FinanceAlertDao;
import com.raez.finance.dao.FinanceCustomerDao;
import com.raez.finance.dao.FinanceCustomerDaoInterface;
import com.raez.finance.dao.FinanceFinancialAnomalyDao;
import com.raez.finance.dao.FinanceInventorySupplierDao;
import com.raez.finance.dao.FinanceOrderDao;
import com.raez.finance.dao.FinanceOrderDaoInterface;
import com.raez.finance.dao.FinanceProductDao;
import com.raez.finance.dao.FinanceProductDaoInterface;
import com.raez.finance.dao.FinanceRevenueVatDao;
import com.raez.finance.model.FinanceCustomerReportRow;
import com.raez.finance.model.FinanceOrderReportRow;
import com.raez.finance.model.FinanceProductReportRow;
import com.raez.finance.model.FinanceTopBuyerRow;
import com.raez.finance.service.FinanceDashboardService;
import com.raez.finance.service.FinanceExportService;
import com.raez.finance.service.FinanceSessionManager;
import com.raez.finance.util.FinanceCurrencyUtil;
import com.raez.finance.util.FinanceStageNavigator;
import com.raez.finance.util.FinanceUiAutoRefreshable;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinanceMainLayoutController {
    private static final Logger log = LoggerFactory.getLogger(FinanceMainLayoutController.class);


    private static final String VIEW_PATH = "/com/raez/finance/view/";

    /**
     * OS / JavaFX often deliver {@code MOUSE_MOVED} while the cursor appears still, which
     * prevented idle timeout with the window focused. Only count movement after this distance² in scene coords.
     */
    private static final double SESSION_MOUSE_MOVE_EPSILON_SQ = 5.0 * 5.0;

    public static void queueStartupToast(String type, String message) {
        pendingToastType    = type;
        pendingToastMessage = message;
    }

    private static volatile String pendingToastType;
    private static volatile String pendingToastMessage;

    private FinanceTopBarController topBarController;
    private final AtomicBoolean sessionWarningLatch = new AtomicBoolean(false);

    private ProgressBar inactivityProgressBar;
    private Timeline      sessionUiTicker;
    private ScheduledService<Void> autoRefreshService;
    private boolean sessionWarningBuilt;

    /** Last scene position used for idle detection (NaN = not set / reset after mouse leaves window). */
    private double sessionMouseAnchorX = Double.NaN;
    private double sessionMouseAnchorY = Double.NaN;

    // ── FXML ────────────────────────────────────────────────────────────
    @FXML private VBox      sidebarContainer;
    @FXML private VBox      topBarContainer;
    @FXML private StackPane contentArea;
    @FXML private VBox      footerContainer;
    @FXML private StackPane sessionWarningOverlay;

    // ══════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        log.info("{}", "[FinanceMainLayout] initialize() started");
        safeLoad("FinanceSidebar",   this::loadSidebar);
        safeLoad("FinanceTopBar",    this::loadTopBar);
        safeLoad("FinanceFooter",    this::loadFooter);
        safeLoad("Dashboard", this::loadDashboard);

        FinanceSessionManager.setOnTimeoutCallback(() -> {
            log.info("{}", "[FinanceMainLayout] Session timed out — redirecting to login.");
            handleLogout();
        });

        Platform.runLater(() -> {
            attachActivityListeners();
            startSessionUiTicker();
            startAutoRefresh();
            refreshNotificationBadge();
            String pt = pendingToastType;
            String pm = pendingToastMessage;
            if (pm != null && !pm.isBlank()) {
                pendingToastType    = null;
                pendingToastMessage = null;
                showToast(pt != null ? pt : "success", pm);
            }
        });
        log.info("{}", "[FinanceMainLayout] initialize() completed");
    }

    // ── Safe load wrapper ────────────────────────────────────────────────

    @FunctionalInterface
    private interface Loader { void load() throws Exception; }

    private void safeLoad(String componentName, Loader loader) {
        try {
            log.info("{}", "[FinanceMainLayout] Loading " + componentName + "...");
            loader.load();
            log.info("{}", "[FinanceMainLayout] " + componentName + " loaded OK");
        } catch (Exception e) {
            log.error("");
            log.error("{}", "╔══════════════════════════════════════════════════════╗");
            log.error("{}", "║  MAINLAYOUT COMPONENT FAILED: " + componentName);
            log.error("{}", "╠══════════════════════════════════════════════════════╣");
            Throwable t = e; int depth = 0;
            while (t != null) {
                log.error("{}", "║  [" + depth + "] " + t.getClass().getSimpleName() + ": " + t.getMessage());
                t = t.getCause(); depth++;
            }
            log.error("{}", "╚══════════════════════════════════════════════════════╝");
            log.error("Error", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMPONENT LOADERS — unchanged
    // ══════════════════════════════════════════════════════════════════════

    private void loadSidebar() throws Exception {
        URL url = getClass().getResource(VIEW_PATH + "FinanceSidebar.fxml");
        if (url == null) throw new IllegalStateException("FinanceSidebar.fxml not found");
        FXMLLoader loader = new FXMLLoader(url);
        Node root = loader.load();
        FinanceSidebarController ctrl = loader.getController();
        if (ctrl != null) ctrl.setMainLayoutController(this);
        sidebarContainer.getChildren().setAll(root);
        VBox.setVgrow(root, Priority.ALWAYS);
    }

    private void loadTopBar() throws Exception {
        URL url = getClass().getResource(VIEW_PATH + "FinanceTopBar.fxml");
        if (url == null) throw new IllegalStateException("FinanceTopBar.fxml not found");
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        FinanceTopBarController ctrl = loader.getController();
        this.topBarController = ctrl;
        if (ctrl != null) ctrl.setMainLayoutController(this);
        topBarContainer.getChildren().setAll(root);
    }

    private void loadFooter() throws Exception {
        URL url = getClass().getResource(VIEW_PATH + "FinanceFooter.fxml");
        if (url == null) { log.info("{}", "[FinanceMainLayout] FinanceFooter.fxml not found — skipping"); return; }
        Parent root = FXMLLoader.load(url);
        footerContainer.getChildren().setAll(root);
    }

    private void loadDashboard() throws Exception {
        URL url = getClass().getResource(VIEW_PATH + "FinanceOverview.fxml");
        if (url == null) throw new IllegalStateException("FinanceOverview.fxml not found");
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        FinanceOverviewController ctrl = loader.getController();
        if (ctrl != null) { ctrl.setMainLayoutController(this); root.setUserData(ctrl); }
        contentArea.getChildren().setAll(root);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ACTIVITY LISTENERS — real idle vs phantom MOUSE_MOVED
    // ══════════════════════════════════════════════════════════════════════

    private void attachActivityListeners() {
        if (contentArea == null || contentArea.getScene() == null) return;
        Node sceneRoot = contentArea.getScene().getRoot();
        sceneRoot.addEventFilter(MouseEvent.ANY, this::handleSessionMouseFilter);
        sceneRoot.addEventFilter(KeyEvent.KEY_PRESSED, e -> onDefiniteUserActivity());
        sceneRoot.addEventFilter(ScrollEvent.ANY, e -> onDefiniteUserActivity());
    }

    private void handleSessionMouseFilter(MouseEvent e) {
        if (isSessionWarningDialogOpen()) {
            return;
        }
        var t = e.getEventType();
        if (t == MouseEvent.MOUSE_EXITED) {
            sessionMouseAnchorX = Double.NaN;
            sessionMouseAnchorY = Double.NaN;
            return;
        }
        if (t == MouseEvent.MOUSE_DRAGGED) {
            snapSessionMouseAnchor(e);
            onDefiniteUserActivity();
            return;
        }
        if (t == MouseEvent.MOUSE_CLICKED || t == MouseEvent.MOUSE_PRESSED) {
            snapSessionMouseAnchor(e);
            onDefiniteUserActivity();
            return;
        }
        if (t == MouseEvent.MOUSE_MOVED) {
            double x = e.getSceneX();
            double y = e.getSceneY();
            if (Double.isNaN(sessionMouseAnchorX)) {
                sessionMouseAnchorX = x;
                sessionMouseAnchorY = y;
                return;
            }
            double dx = x - sessionMouseAnchorX;
            double dy = y - sessionMouseAnchorY;
            if (dx * dx + dy * dy >= SESSION_MOUSE_MOVE_EPSILON_SQ) {
                sessionMouseAnchorX = x;
                sessionMouseAnchorY = y;
                onDefiniteUserActivity();
            }
        }
    }

    private void snapSessionMouseAnchor(MouseEvent e) {
        sessionMouseAnchorX = e.getSceneX();
        sessionMouseAnchorY = e.getSceneY();
    }

    /**
     * While the session timeout dialog is open, mouse/keys must not extend the session or close it —
     * only {@link #dismissInactivityWarning()} (Stay Logged In) does.
     */
    private boolean isSessionWarningDialogOpen() {
        return sessionWarningOverlay != null && sessionWarningOverlay.isVisible();
    }

    private void onDefiniteUserActivity() {
        if (isSessionWarningDialogOpen()) {
            return;
        }
        FinanceSessionManager.touchSessionFromUserInput();
        long remaining = FinanceSessionManager.getRemainingSeconds();
        if (remaining > FinanceSessionManager.SESSION_WARNING_LEAD_SECONDS) {
            sessionWarningLatch.set(false);
            hideInactivityWarningUi();
        }
    }

    private void ensureSessionWarningBuilt() {
        if (sessionWarningBuilt || sessionWarningOverlay == null) return;
        sessionWarningBuilt = true;

        sessionWarningOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Region scrim = new Region();
        scrim.getStyleClass().add("session-warning-scrim");
        scrim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scrim.prefWidthProperty().bind(sessionWarningOverlay.widthProperty());
        scrim.prefHeightProperty().bind(sessionWarningOverlay.heightProperty());

        Label title = new Label("Session timeout");
        title.getStyleClass().add("session-warning-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(TextAlignment.CENTER);

        Label msg = new Label("You will be logged out in 10 seconds due to inactivity.");
        msg.getStyleClass().add("session-warning-message");
        msg.setMaxWidth(Double.MAX_VALUE);
        msg.setAlignment(Pos.CENTER);
        msg.setTextAlignment(TextAlignment.CENTER);

        inactivityProgressBar = new ProgressBar(1);
        inactivityProgressBar.getStyleClass().add("session-warning-progress");

        Button stay = new Button("Stay Logged In");
        stay.setDefaultButton(true);
        stay.getStyleClass().setAll("session-warning-stay");
        stay.setOnAction(e -> dismissInactivityWarning());

        VBox card = new VBox(18, title, msg, inactivityProgressBar, stay);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);
        card.getStyleClass().add("session-warning-card");

        StackPane cardHost = new StackPane(card);
        cardHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cardHost.setPickOnBounds(false);
        StackPane.setAlignment(card, Pos.CENTER);

        sessionWarningOverlay.getChildren().addAll(scrim, cardHost);
    }

    private void startSessionUiTicker() {
        stopSessionUiTicker();
        sessionUiTicker = new Timeline(new KeyFrame(Duration.millis(250), e -> tickInactivityWarningUi()));
        sessionUiTicker.setCycleCount(Timeline.INDEFINITE);
        sessionUiTicker.play();
    }

    private void stopSessionUiTicker() {
        if (sessionUiTicker != null) {
            sessionUiTicker.stop();
            sessionUiTicker = null;
        }
    }

    private void tickInactivityWarningUi() {
        if (sessionWarningOverlay == null || !FinanceSessionManager.isLoggedIn()) return;
        long rem = FinanceSessionManager.getRemainingSeconds();
        long lead = FinanceSessionManager.SESSION_WARNING_LEAD_SECONDS;

        if (rem > lead) {
            sessionWarningLatch.set(false);
            hideInactivityWarningUi();
            return;
        }
        if (rem <= 0) return;

        ensureSessionWarningBuilt();
        if (!sessionWarningLatch.getAndSet(true)) {
            sessionWarningOverlay.setVisible(true);
            sessionWarningOverlay.setManaged(true);
            sessionWarningOverlay.setPickOnBounds(true);
            sessionWarningOverlay.toFront();
        }
        if (inactivityProgressBar != null && lead > 0)
            inactivityProgressBar.setProgress(Math.max(0, Math.min(1, rem / (double) lead)));
    }

    private void hideInactivityWarningUi() {
        if (sessionWarningOverlay == null) return;
        sessionWarningOverlay.setVisible(false);
        sessionWarningOverlay.setManaged(false);
        sessionWarningOverlay.setPickOnBounds(false);
        sessionWarningLatch.set(false);
    }

    private void dismissInactivityWarning() {
        FinanceSessionManager.extendSession();
        hideInactivityWarningUi();
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshService = new ScheduledService<>() {
            @Override protected Task<Void> createTask() {
                return new Task<>() {
                    @Override protected Void call() {
                        return null;
                    }
                };
            }
        };
        autoRefreshService.setPeriod(Duration.seconds(30));
        autoRefreshService.setExecutor(Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ui-auto-refresh");
            t.setDaemon(true);
            return t;
        }));
        autoRefreshService.setOnSucceeded(e -> Platform.runLater(this::triggerVisibleViewRefresh));
        autoRefreshService.setRestartOnFailure(true);
        autoRefreshService.start();
    }

    private void stopAutoRefresh() {
        if (autoRefreshService != null) {
            autoRefreshService.cancel();
            autoRefreshService = null;
        }
    }

    private void triggerVisibleViewRefresh() {
        if (contentArea == null || contentArea.getChildren().isEmpty()) return;
        FinanceSessionManager.beginAutomatedDataRefresh();
        try {
            Object ud = contentArea.getChildren().get(0).getUserData();
            if (ud instanceof FinanceUiAutoRefreshable r) {
                try {
                    r.refreshVisibleData();
                } catch (Exception ex) {
                    log.error("{}", "[FinanceMainLayout] auto-refresh failed: " + ex.getMessage());
                }
            }
        } finally {
            FinanceSessionManager.endAutomatedDataRefresh();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONTENT SWITCHING — unchanged
    // ══════════════════════════════════════════════════════════════════════

    public void setContent(Node node) {
        if (contentArea == null) return;

        if (!contentArea.getChildren().isEmpty()) {
            Object ud = contentArea.getChildren().get(0).getUserData();
            if      (ud instanceof FinanceOverviewController c)             c.shutdown();
            else if (ud instanceof FinanceDetailedReportsController c)      c.shutdown();
            else if (ud instanceof FinanceGlobalSearchResultsController c)  c.shutdown();
            else if (ud instanceof FinanceInvoicesController c)             c.shutdown();
            else if (ud instanceof FinanceNotificationsAlertsController c) c.shutdown();
            else if (ud instanceof FinanceAuditLogController c)             c.shutdown();
            else if (ud instanceof FinanceAiInsightsController c)           c.shutdown();
            else if (ud instanceof FinanceCustomerInsightsController c)     c.shutdown();
            else if (ud instanceof FinanceProductProfitabilityController c) c.shutdown();
            else if (ud instanceof FinanceRevenueVatSummaryController c)    c.shutdown();
            else if (ud instanceof FinanceInventorySupplierController c)     c.shutdown();
            else if (ud instanceof FinanceSettingsController c)             c.shutdown();
        }

        contentArea.getChildren().setAll(node);
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        refreshNotificationBadge();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOGOUT — ONLY CHANGE IN THIS FILE
    //  Previously used inline FXMLLoader + manual scene swap which caused
    //  the window-shrink bug. Now delegates to FinanceStageNavigator.navigateToLogin()
    //  which handles the false→true maximize toggle correctly.
    // ══════════════════════════════════════════════════════════════════════

    public void handleLogout() {
        stopSessionUiTicker();
        stopAutoRefresh();
        hideInactivityWarningUi();
        FinanceSessionManager.logout();
        if (contentArea == null || contentArea.getScene() == null) return;
        Stage stage = (Stage) contentArea.getScene().getWindow();
        FinanceStageNavigator.navigateToLogin(stage);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NOTIFICATION BADGE — unchanged
    // ══════════════════════════════════════════════════════════════════════

    public void updateNotificationBadge(int count) {
        Platform.runLater(() -> {
            if (topBarController != null) topBarController.setNotificationBadgeCount(count);
        });
    }

    public void refreshNotificationBadge() {
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                int a = new FinanceAlertDao().countUnresolved();
                int f = new FinanceFinancialAnomalyDao().countUnresolved();
                return a + f;
            }
        };
        task.setOnSucceeded(e -> updateNotificationBadge(task.getValue() != null ? task.getValue() : 0));
        task.setOnFailed(ev  -> updateNotificationBadge(0));
        new Thread(task, "notification-badge").start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GLOBAL SEARCH — unchanged
    // ══════════════════════════════════════════════════════════════════════

    public void showGlobalSearch(String query) {
        if (query == null || query.isBlank()) return;
        try {
            URL url = getClass().getResource(VIEW_PATH + "FinanceGlobalSearchResults.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            FinanceGlobalSearchResultsController ctrl = loader.getController();
            if (ctrl != null) { ctrl.setMainLayoutController(this); ctrl.setQuery(query); root.setUserData(ctrl); }
            setContent(root);
        } catch (Exception e) {
            log.error("{}", "[FinanceMainLayout] showGlobalSearch failed:"); log.error("Error", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NAVIGATE TO DETAILED REPORTS — unchanged
    // ══════════════════════════════════════════════════════════════════════

    public void navigateToDetailedReportsWithSearch(String tab, String searchText) {
        try {
            URL url = getClass().getResource(VIEW_PATH + "FinanceDetailedReports.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            FinanceDetailedReportsController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setMainLayoutController(this);
                root.setUserData(ctrl);
                ctrl.openWithContext(tab != null ? tab : "orders", searchText);
            }
            setContent(root);
        } catch (Exception e) {
            log.error("{}", "[FinanceMainLayout] navigateToDetailedReportsWithSearch failed:"); log.error("Error", e);
        }
    }

    public void navigateToAiInsights() {
        try {
            URL url = getClass().getResource(VIEW_PATH + "FinanceAiInsights.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            FinanceAiInsightsController ctrl = loader.getController();
            if (ctrl != null) { ctrl.setMainLayoutController(this); root.setUserData(ctrl); }
            setContent(root);
        } catch (Exception e) {
            log.error("{}", "[FinanceMainLayout] navigateToAiInsights failed:"); log.error("Error", e);
        }
    }

    public void navigateToReportsAndExport(String reportType, String format) {
        try {
            URL url = getClass().getResource(VIEW_PATH + "FinanceDetailedReports.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            FinanceDetailedReportsController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setMainLayoutController(this);
                root.setUserData(ctrl);
                ctrl.setAfterLoadCallback(() -> {
                    if ("pdf".equalsIgnoreCase(format)) ctrl.performExportPDF();
                    else ctrl.performExportCSV();
                });
            }
            setContent(root);
            if (ctrl != null) ctrl.switchToTab(reportType != null ? reportType : "orders");
        } catch (Exception e) {
            log.error("{}", "[FinanceMainLayout] navigateToReportsAndExport failed:"); log.error("Error", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TOAST — unchanged
    // ══════════════════════════════════════════════════════════════════════

    public void showToast(String type, String message) {
        if (contentArea == null) return;
        try {
            URL url = getClass().getResource(VIEW_PATH + "FinanceNotificationToast.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Node toast = loader.load();
            FinanceNotificationToastController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setCompact(type, message, () -> {
                    if (contentArea.getChildren().contains(toast))
                        contentArea.getChildren().remove(toast);
                });
            }
            contentArea.getChildren().add(toast);
            StackPane.setAlignment(toast, Pos.TOP_RIGHT);
            StackPane.setMargin(toast, new Insets(12, 12, 0, 0));
        } catch (Exception e) {
            log.error("{}", "[FinanceMainLayout] showToast failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ALL EXPORT METHODS — unchanged from document 16 (FinanceMainLayoutController)
    //  Paste the full export methods here from your existing FinanceMainLayoutController.
    //  Only handleLogout() needed changing for this PR.
    // ══════════════════════════════════════════════════════════════════════

    public void exportMergedCustomerReport() {
        if (!FinanceSessionManager.isAdmin()) return;
        Window window = getWindow();
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception {
                return buildMergedCustomerRows();
            }
        };
        finishMergedExport(task, "RAEZ Finance — Customer Report", "customer_merged_report", window);
    }

    private List<String[]> buildMergedCustomerRows() throws Exception {
        LocalDate to = LocalDate.now();
        return buildRichCustomerReportRows(to.minusDays(90), to);
    }

    private List<String[]> buildRichCustomerReportRows(LocalDate from, LocalDate to) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        FinanceCustomerDao cDao = new FinanceCustomerDao();
        int total = cDao.getTotalCustomerCount();
        int companies = cDao.getCompanyCustomerCount();
        int individuals = total - companies;
        double totalRev = cDao.getTotalRevenue();
        double avgSpend = total > 0 ? totalRev / total : 0;
        List<FinanceTopBuyerRow> topBuyers = cDao.findTopBuyers(20);
        List<com.raez.finance.dao.FinanceCustomerDao.MonthlyCount> monthly = cDao.findMonthlyOrderCounts(from, to);
        List<String> refunds = cDao.findRefundAlerts();
        List<String> issues = cDao.findProductIssueAlerts();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"__COVER__", "Customer Report", "Customer List & Insights — Combined", date});
        rows.add(new String[]{"__SECTION__", "Customer Summary", "Lifetime aggregates"});
        rows.add(new String[]{"__KPI__",
            "Total Customers", String.valueOf(total), "Companies", String.valueOf(companies),
            "Individuals", String.valueOf(individuals), "Avg Spend", FinanceCurrencyUtil.formatCurrency(avgSpend)});
        rows.add(new String[]{"__SECTION__", "Monthly orders Volume", "Orders per month"});
        List<String> chartRow = new ArrayList<>(); chartRow.add("__BARCHART__"); chartRow.add("Orders per Month");
        for (var m : monthly) { chartRow.add(m.month); chartRow.add(String.valueOf(m.count)); }
        rows.add(chartRow.toArray(new String[0]));
        rows.add(new String[]{"__BARCHART__", "Customer Types", "Companies", String.valueOf(companies), "Individuals", String.valueOf(individuals)});
        rows.add(new String[]{"__SECTION__", "Top Buyers", "Ranked by lifetime spending"});
        rows.add(new String[]{"__TABLEHEADER__", "Rank", "Customer", "Type", "Total Spent", "Orders", "Avg orders", "Last Purchase"});
        for (FinanceTopBuyerRow r : topBuyers)
            rows.add(new String[]{String.valueOf(r.getRank()), r.getName(), r.getType(),
                FinanceCurrencyUtil.formatCurrency(r.getTotalSpent()), String.valueOf(r.getTotalOrders()),
                FinanceCurrencyUtil.formatCurrency(r.getAvgOrderValue()), r.getLastPurchase() != null ? r.getLastPurchase() : "—"});
        if (!refunds.isEmpty() || !issues.isEmpty()) {
            rows.add(new String[]{"__SECTION__", "Alerts", "finance_refunds patterns and product issues"});
            rows.add(new String[]{"__TABLEHEADER__", "finance_alerts Type", "Detail"});
            for (String r : refunds) rows.add(new String[]{"finance_refunds Pattern", r});
            for (String r : issues)  rows.add(new String[]{"products Issue", r});
        }
        return rows;
    }

    public void exportMergedOrderReport() {
        if (!FinanceSessionManager.isAdmin()) return;
        Window window = getWindow();
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception { return buildMergedOrderRows(); }
        };
        finishMergedExport(task, "RAEZ Finance — orders Report", "order_merged_report", window);
    }

    private List<String[]> buildMergedOrderRows() throws Exception {
        LocalDate to = LocalDate.now(), from = to.minusDays(90);
        String date = to.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        FinanceDashboardService ds = new FinanceDashboardService(); FinanceOrderDao od = new FinanceOrderDao();
        double totalSales = ds.getTotalSales(from, to, null);
        int totalOrders = ds.getTotalOrders(from, to, null);
        double aov = ds.getAverageOrderValue(from, to, null);
        double outstanding = ds.getOutstandingPayments(from, to, null);
        String popular = ds.getMostPopularProductName(from, to, null);
        List<FinanceOrderReportRow> allOrders = od.findReportRows(from, to, "All Status", null, null, 200, 0);
        Map<String, Long> byStatus = allOrders.stream()
            .collect(Collectors.groupingBy(r -> r.getStatus() != null ? r.getStatus() : "Unknown", Collectors.counting()));
        Map<String, Double> byMonth = allOrders.stream()
            .filter(r -> r.getDate() != null && r.getDate().length() >= 7)
            .collect(Collectors.groupingBy(r -> r.getDate().substring(0, 7), Collectors.summingDouble(FinanceOrderReportRow::getAmount)));
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"__COVER__", "orders Report", "orders Data & Revenue Analysis — Last 90 Days", date});
        rows.add(new String[]{"__SECTION__", "orders Summary", "Last 90 days"});
        rows.add(new String[]{"__KPI__",
            "Total Revenue", FinanceCurrencyUtil.formatCurrency(totalSales),
            "Total Orders", String.valueOf(totalOrders),
            "Avg orders Value", FinanceCurrencyUtil.formatCurrency(aov),
            "Outstanding", FinanceCurrencyUtil.formatCurrency(outstanding)});
        if (popular != null) rows.add(new String[]{"__KPI__", "Most Popular products", popular, "", "", "", "", ""});
        rows.add(new String[]{"__SECTION__", "orders Status Breakdown", "Count by status"});
        List<String> sc = new ArrayList<>(); sc.add("__BARCHART__"); sc.add("Orders by Status");
        byStatus.forEach((s, c) -> { sc.add(s); sc.add(String.valueOf(c)); });
        rows.add(sc.toArray(new String[0]));
        rows.add(new String[]{"__SECTION__", "Monthly Revenue Trend", "Revenue by month"});
        List<String> mc = new ArrayList<>(); mc.add("__BARCHART__"); mc.add("Revenue per Month");
        byMonth.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(e -> { mc.add(e.getKey()); mc.add(String.valueOf(e.getValue().longValue())); });
        rows.add(mc.toArray(new String[0]));
        rows.add(new String[]{"__SECTION__", "orders Detail", "Individual orders"});
        rows.add(new String[]{"__TABLEHEADER__", "orders ID", "Customer", "products", "Amount", "Date", "Status"});
        for (FinanceOrderReportRow r : allOrders)
            rows.add(new String[]{r.getOrderId(), r.getCustomer(), r.getProduct(),
                FinanceCurrencyUtil.formatCurrency(r.getAmount()), r.getDate(), r.getStatus()});
        return rows;
    }

    public void exportMergedProductReport() {
        if (!FinanceSessionManager.isAdmin()) return;
        Window window = getWindow();
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception { return buildRichProductReportRows(LocalDate.now().minusDays(90), LocalDate.now()); }
        };
        finishMergedExport(task, "RAEZ Finance — products Report", "product_merged_report", window);
    }

    private List<String[]> buildRichProductReportRows(LocalDate from, LocalDate to) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        FinanceProductDao pd = new FinanceProductDao();
        List<FinanceProductReportRow> allProducts = pd.findReportRows(from, to, "All Categories", null, 200, 0);
        List<FinanceProductDao.CategoryRevenueProfit> catData = pd.findCategoryRevenueProfit(from, to);
        double totalRev = allProducts.stream().mapToDouble(FinanceProductReportRow::getRevenue).sum();
        double totalProfit = allProducts.stream().mapToDouble(FinanceProductReportRow::getProfit).sum();
        double avgMargin = allProducts.isEmpty() ? 0 : allProducts.stream().mapToDouble(FinanceProductReportRow::getMarginPercent).average().orElse(0);
        long lowCount = allProducts.stream().filter(p -> p.getMarginPercent() < 35).count();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"__COVER__", "products Report", "products Performance & Profitability — Last 90 Days", date});
        rows.add(new String[]{"__SECTION__", "products Summary", "Revenue, profit and margin"});
        rows.add(new String[]{"__KPI__",
            "Total Revenue", FinanceCurrencyUtil.formatCurrency(totalRev), "Total Profit", FinanceCurrencyUtil.formatCurrency(totalProfit),
            "Avg Margin", String.format("%.1f%%", avgMargin), "Low Margin Items", String.valueOf(lowCount)});
        List<String> rc = new ArrayList<>(); rc.add("__BARCHART__"); rc.add("Category Revenue");
        for (var c : catData) { rc.add(c.category); rc.add(String.valueOf((long) c.revenue)); }
        rows.add(rc.toArray(new String[0]));
        rows.add(new String[]{"__SECTION__", "products Analysis", "Detailed margin and revenue"});
        rows.add(new String[]{"__TABLEHEADER__", "products", "Category", "Units Sold", "Revenue", "Profit", "Margin %"});
        for (FinanceProductReportRow p : allProducts)
            rows.add(new String[]{p.getName(), p.getCategory(), String.valueOf(p.getUnitsSold()),
                FinanceCurrencyUtil.formatCurrency(p.getRevenue()), FinanceCurrencyUtil.formatCurrency(p.getProfit()),
                String.format("%.1f%%", p.getMarginPercent())});
        return rows;
    }

    public void exportFullMergedReportPdf() {
        if (!FinanceSessionManager.isAdmin()) return;
        Window window = getWindow();
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception { return buildMergedReportRows(); }
        };
        finishMergedExport(task, "RAEZ Finance — Full Report", "full_report", window);
    }

    private List<String[]> buildMergedReportRows() throws Exception {
        LocalDate to = LocalDate.now(), from = to.minusDays(30);
        String date = to.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        FinanceDashboardService ds = new FinanceDashboardService();
        FinanceCustomerDao cDao = new FinanceCustomerDao(); FinanceOrderDao oDao = new FinanceOrderDao();
        FinanceProductDao pDao = new FinanceProductDao(); FinanceRevenueVatDao vDao = new FinanceRevenueVatDao();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"__COVER__", "RAEZ Finance — Full Report",
            "Dashboard · Orders · Customers · Products · Revenue & VAT", date});
        rows.add(new String[]{"__SECTION__", "Dashboard FinanceOverview", "Key financial indicators — last 30 days"});
        double totalSales = ds.getTotalSales(from, to, null);
        double netProfit = ds.getTotalProfit(from, to, null);
        double outstanding = ds.getOutstandingPayments(from, to, null);
        double vat = ds.getTotalVatCollected(from, to, null);
        int orders = ds.getTotalOrders(from, to, null);
        int customers = ds.getTotalCustomers();
        double aov = ds.getAverageOrderValue(from, to, null);
        String popular = ds.getMostPopularProductName(from, to, null);
        rows.add(new String[]{"__KPI__",
            "Total Revenue", FinanceCurrencyUtil.formatCurrency(totalSales),
            "Net Profit", FinanceCurrencyUtil.formatCurrency(netProfit),
            "Outstanding", FinanceCurrencyUtil.formatCurrency(outstanding),
            "VAT Collected", FinanceCurrencyUtil.formatCurrency(vat)});
        rows.add(new String[]{"__KPI__",
            "Total Orders", String.valueOf(orders), "Customers", String.valueOf(customers),
            "Avg orders Value", FinanceCurrencyUtil.formatCurrency(aov), "Top products", popular != null ? popular : "—"});
        List<FinanceProductDao.CategoryRevenueProfit> catData = pDao.findCategoryRevenueProfit(from, to);
        List<String> cc = new ArrayList<>(); cc.add("__BARCHART__"); cc.add("Revenue by Category");
        for (var c : catData) { cc.add(c.category); cc.add(String.valueOf((long) c.revenue)); }
        rows.add(cc.toArray(new String[0]));
        rows.add(new String[]{"__PAGEBREAK__"});
        rows.add(new String[]{"__SECTION__", "orders Report", "Orders in the last 30 days"});
        List<FinanceOrderReportRow> allOrders = oDao.findReportRows(from, to, "All Status", null, null, 100, 0);
        Map<String, Long> sm = allOrders.stream()
            .collect(Collectors.groupingBy(r -> r.getStatus() != null ? r.getStatus() : "Unknown", Collectors.counting()));
        List<String> sc = new ArrayList<>(); sc.add("__BARCHART__"); sc.add("Orders by Status");
        sm.forEach((s, c) -> { sc.add(s); sc.add(String.valueOf(c)); });
        rows.add(sc.toArray(new String[0]));
        rows.add(new String[]{"__TABLEHEADER__", "orders ID", "Customer", "products", "Amount", "Date", "Status"});
        for (FinanceOrderReportRow r : allOrders)
            rows.add(new String[]{r.getOrderId(), r.getCustomer(), r.getProduct(),
                FinanceCurrencyUtil.formatCurrency(r.getAmount()), r.getDate(), r.getStatus()});
        rows.add(new String[]{"__PAGEBREAK__"});
        rows.add(new String[]{"__SECTION__", "Customer Report", "Customer overview and top buyers"});
        int total = cDao.getTotalCustomerCount(); int cos = cDao.getCompanyCustomerCount();
        rows.add(new String[]{"__KPI__",
            "Total Customers", String.valueOf(total), "Companies", String.valueOf(cos),
            "Individuals", String.valueOf(total - cos),
            "Avg Spend", FinanceCurrencyUtil.formatCurrency(total > 0 ? cDao.getTotalRevenue() / total : 0)});
        rows.add(new String[]{"__BARCHART__", "Customer Type Split", "Companies", String.valueOf(cos), "Individuals", String.valueOf(total - cos)});
        List<FinanceTopBuyerRow> topBuyers = cDao.findTopBuyers(15);
        rows.add(new String[]{"__TABLEHEADER__", "Rank", "Customer", "Type", "Total Spent", "Orders", "Last Purchase"});
        for (FinanceTopBuyerRow r : topBuyers)
            rows.add(new String[]{String.valueOf(r.getRank()), r.getName(), r.getType(),
                FinanceCurrencyUtil.formatCurrency(r.getTotalSpent()), String.valueOf(r.getTotalOrders()),
                r.getLastPurchase() != null ? r.getLastPurchase() : "—"});
        rows.add(new String[]{"__PAGEBREAK__"});
        List<FinanceProductReportRow> allProd = pDao.findReportRows(from, to, "All Categories", null, 100, 0);
        double tR = allProd.stream().mapToDouble(FinanceProductReportRow::getRevenue).sum();
        double tP = allProd.stream().mapToDouble(FinanceProductReportRow::getProfit).sum();
        double tM = allProd.isEmpty() ? 0 : allProd.stream().mapToDouble(FinanceProductReportRow::getMarginPercent).average().orElse(0);
        rows.add(new String[]{"__SECTION__", "products Profitability", "Margin and revenue analysis"});
        rows.add(new String[]{"__KPI__",
            "Revenue", FinanceCurrencyUtil.formatCurrency(tR), "Profit", FinanceCurrencyUtil.formatCurrency(tP),
            "Avg Margin", String.format("%.1f%%", tM), "Products", String.valueOf(allProd.size())});
        List<String> pc = new ArrayList<>(); pc.add("__BARCHART__"); pc.add("Category Profit");
        for (var c : catData) { pc.add(c.category); pc.add(String.valueOf((long) c.profit)); }
        rows.add(pc.toArray(new String[0]));
        rows.add(new String[]{"__TABLEHEADER__", "products", "Category", "Revenue", "Profit", "Margin %", "Units"});
        for (FinanceProductReportRow p : allProd)
            rows.add(new String[]{p.getName(), p.getCategory(),
                FinanceCurrencyUtil.formatCurrency(p.getRevenue()), FinanceCurrencyUtil.formatCurrency(p.getProfit()),
                String.format("%.1f%%", p.getMarginPercent()), String.valueOf(p.getUnitsSold())});
        rows.add(new String[]{"__PAGEBREAK__"});
        rows.add(new String[]{"__SECTION__", "Revenue & VAT Summary", "Gross vs net and VAT liabilities"});
        double gross = totalSales, vatAmt = vat, net = gross - vatAmt;
        double cogs = ds.getTotalCogs(from, to, null);
        double margin = net > 0 ? ((net - cogs) / net) * 100 : 0;
        rows.add(new String[]{"__KPI__",
            "Gross Revenue", FinanceCurrencyUtil.formatCurrency(gross),
            "VAT Collected", FinanceCurrencyUtil.formatCurrency(vatAmt),
            "Net Revenue", FinanceCurrencyUtil.formatCurrency(net),
            "Gross Margin", String.format("%.1f%%", margin)});
        List<String> vc = new ArrayList<>(); vc.add("__BARCHART__"); vc.add("Gross Revenue by Category");
        for (var r : vDao.findCategoryVatRows(from, to)) { vc.add(r.category()); vc.add(String.valueOf((long) r.gross())); }
        rows.add(vc.toArray(new String[0]));
        rows.add(new String[]{"__TABLEHEADER__", "Category", "Orders", "Gross", "VAT", "Net", "Margin %"});
        for (var r : vDao.findCategoryVatRows(from, to)) {
            double rNet = r.gross() - r.vat();
            double rMar = rNet > 0 ? ((rNet - r.cogs()) / rNet) * 100 : 0;
            rows.add(new String[]{r.category(), String.valueOf(r.orders()),
                FinanceCurrencyUtil.formatCurrency(r.gross()), FinanceCurrencyUtil.formatCurrency(r.vat()),
                FinanceCurrencyUtil.formatCurrency(rNet), String.format("%.1f%%", rMar)});
        }
        return rows;
    }

    public void exportProfileReportPdf(String reportKey) {
        Window window = getWindow(); if (window == null) return;
        LocalDate to = LocalDate.now(), from = to.minusDays(30);
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception {
                return buildProfileExportRows(reportKey, from, to);
            }
        };
        String title = switch (reportKey) {
            case "customer_insights"    -> "Customer Insights";
            case "product_profitability"-> "products Profitability";
            case "revenue_vat"          -> "Revenue & VAT Summary";
            case "inventory"            -> "Inventory & Suppliers";
            default -> "Report";
        };
        task.setOnSucceeded(e -> {
            List<String[]> data = task.getValue();
            if (data == null || data.isEmpty()) { showToast("warning", "No data to export."); return; }
            File file = pickFile(window, title, reportKey.replace('_', '-'), "pdf");
            if (file == null) return;
            try { FinanceExportService.exportMergedReport(title, data, file); showToast("success", "Exported: " + file.getName()); }
            catch (Exception ex) { showToast("error", "Export failed: " + ex.getMessage()); }
        });
        task.setOnFailed(ev -> showToast("error", "Export failed."));
        new Thread(task, "profile-export").start();
    }

    private List<String[]> buildProfileExportRows(String settingKey, LocalDate from, LocalDate to) throws Exception {
        return switch (settingKey) {
            case "customer_insights"    -> buildRichCustomerReportRows(from, to);
            case "product_profitability"-> buildRichProductReportRows(from, to);
            default -> new ArrayList<>();
        };
    }

    public void exportDashboardReport(String format) {
        Window window = getWindow(); if (window == null) return;
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception {
                FinanceDashboardService ds = new FinanceDashboardService();
                LocalDate to = LocalDate.now(), from = to.minusDays(30);
                List<String[]> rows = new ArrayList<>();
                rows.add(new String[]{"Metric", "Value"});
                rows.add(new String[]{"Total Sales",      FinanceCurrencyUtil.formatCurrency(ds.getTotalSales(from, to, null))});
                rows.add(new String[]{"Total Profit",     FinanceCurrencyUtil.formatCurrency(ds.getTotalProfit(from, to, null))});
                rows.add(new String[]{"Outstanding",      FinanceCurrencyUtil.formatCurrency(ds.getOutstandingPayments(from, to, null))});
                rows.add(new String[]{"Total Customers",  String.valueOf(ds.getTotalCustomers())});
                rows.add(new String[]{"Total Orders",     String.valueOf(ds.getTotalOrders(from, to, null))});
                rows.add(new String[]{"Avg orders Value",  FinanceCurrencyUtil.formatCurrency(ds.getAverageOrderValue(from, to, null))});
                String pop = ds.getMostPopularProductName(from, to, null);
                rows.add(new String[]{"Most Popular products", pop != null ? pop : "—"});
                return rows;
            }
        };
        finishExport(task, "Dashboard Summary", "dashboard_summary", format, window);
    }

    public void exportOrderReport(String format)    { doExport("orders",    "orders Report",    "order_report",    format); }
    public void exportProductReport(String format)  { doExport("products",  "products Report",  "product_report",  format); }
    public void exportCustomerReport(String format) { doExport("customers", "Customer Report", "customer_report", format); }

    private void doExport(String type, String title, String fileName, String format) {
        Window window = getWindow(); if (window == null) return;
        LocalDate to = LocalDate.now(), from = to.minusDays(30);
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception {
                List<String[]> rows = new ArrayList<>();
                switch (type) {
                    case "orders" -> {
                        List<FinanceOrderReportRow> list = new FinanceOrderDao().findReportRows(from, to, "All Status", null, null, 0, 0);
                        rows.add(new String[]{"orders ID","Customer","products","Amount","Date","Status"});
                        for (FinanceOrderReportRow r : list)
                            rows.add(new String[]{r.getOrderId(), r.getCustomer(), r.getProduct(),
                                FinanceCurrencyUtil.formatCurrency(r.getAmount()), r.getDate(), r.getStatus()});
                    }
                    case "products" -> {
                        List<FinanceProductReportRow> list = new FinanceProductDao().findReportRows(from, to, "All Categories", null, 0, 0);
                        rows.add(new String[]{"products ID","Name","Category","Cost","Sale Price","Profit","Units","Revenue"});
                        for (FinanceProductReportRow r : list)
                            rows.add(new String[]{r.getProductId(), r.getName(), r.getCategory(),
                                FinanceCurrencyUtil.formatCurrency(r.getCost()), FinanceCurrencyUtil.formatCurrency(r.getSalePrice()),
                                FinanceCurrencyUtil.formatCurrency(r.getProfit()), String.valueOf(r.getUnitsSold()),
                                FinanceCurrencyUtil.formatCurrency(r.getRevenue())});
                    }
                    default -> {
                        List<FinanceCustomerReportRow> list = new FinanceCustomerDao().findReportRows(null, null, "All", "All", null, null, 0, 0);
                        rows.add(new String[]{"Customer ID","Name","Type","Country","Orders","Spent","Avg orders","Last Purchase"});
                        for (FinanceCustomerReportRow r : list)
                            rows.add(new String[]{r.getCustomerId(), r.getName(), r.getType(), r.getCountry(),
                                String.valueOf(r.getTotalOrders()), FinanceCurrencyUtil.formatCurrency(r.getTotalSpent()),
                                FinanceCurrencyUtil.formatCurrency(r.getAvgOrderValue()), r.getLastPurchase()});
                    }
                }
                return rows;
            }
        };
        finishExport(task, title, fileName, format, window);
    }

    private void finishMergedExport(Task<List<String[]>> task, String title, String fileName, Window window) {
        task.setOnSucceeded(e -> {
            List<String[]> data = task.getValue();
            if (data == null || data.isEmpty()) { showToast("warning", "No data to export."); return; }
            File file = pickFile(window, title, fileName, "pdf");
            if (file == null) return;
            try { FinanceExportService.exportMergedReport(title, data, file); showToast("success", "Exported: " + file.getName()); }
            catch (Exception ex) { showToast("error", "Export failed: " + ex.getMessage()); }
        });
        task.setOnFailed(ev -> { if (task.getException() != null) log.error("Error", task.getException()); showToast("error", "Export failed."); });
        new Thread(task, "merged-export").start();
    }

    private void finishExport(Task<List<String[]>> task, String title, String fileName, String format, Window window) {
        task.setOnSucceeded(e -> {
            List<String[]> data = task.getValue(); if (data == null || data.isEmpty()) return;
            File file = pickFile(window, title, fileName, format); if (file == null) return;
            try {
                if ("pdf".equalsIgnoreCase(format)) FinanceExportService.exportRowsToPDF(title, data, file);
                else FinanceExportService.exportRowsToCSV(data, file);
                showToast("success", "Exported: " + file.getName());
            } catch (Exception ex) { showToast("error", "Export failed: " + ex.getMessage()); }
        });
        task.setOnFailed(ev -> showToast("error", "Export failed."));
        new Thread(task, "export-worker").start();
    }

    private File pickFile(Window window, String title, String defaultName, String format) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export " + title);
        if ("pdf".equalsIgnoreCase(format)) {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName(defaultName + ".pdf");
        } else {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fc.setInitialFileName(defaultName + ".csv");
        }
        return fc.showSaveDialog(window);
    }

    private Window getWindow() {
        return (contentArea != null && contentArea.getScene() != null)
            ? contentArea.getScene().getWindow() : null;
    }
}
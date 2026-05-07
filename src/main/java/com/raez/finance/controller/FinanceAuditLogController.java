package com.raez.finance.controller;

import com.raez.finance.dao.FinanceAuditLogDao;
import com.raez.finance.service.FinanceExportService;
import com.raez.finance.service.FinanceSessionManager;
import com.raez.finance.util.FinanceUiAutoRefreshable;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinanceAuditLogController implements FinanceUiAutoRefreshable {
    private static final Logger log = LoggerFactory.getLogger(FinanceAuditLogController.class);


    private final FinanceAuditLogDao auditLogDao = new FinanceAuditLogDao();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ObservableList<FinanceAuditLogDao.MergedAuditRow> items = FXCollections.observableArrayList();

    private FinanceMainLayoutController mainLayoutController;

    @FXML private Label lblAccessDenied;
    @FXML private VBox toolbarBox;
    @FXML private VBox tableBox;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<String> cmbSource;
    @FXML private Button btnApply;
    @FXML private MenuButton exportMenuButton;
    @FXML private MenuItem exportCsvItem;
    @FXML private MenuItem exportPdfItem;

    @FXML private TableView<FinanceAuditLogDao.MergedAuditRow> tblAudit;
    @FXML private TableColumn<FinanceAuditLogDao.MergedAuditRow, String> colDate;
    @FXML private TableColumn<FinanceAuditLogDao.MergedAuditRow, String> colSource;
    @FXML private TableColumn<FinanceAuditLogDao.MergedAuditRow, String> colUser;
    @FXML private TableColumn<FinanceAuditLogDao.MergedAuditRow, String> colAction;
    @FXML private TableColumn<FinanceAuditLogDao.MergedAuditRow, String> colEntity;
    @FXML private TableColumn<FinanceAuditLogDao.MergedAuditRow, String> colDetails;

    public void setMainLayoutController(FinanceMainLayoutController c) {
        this.mainLayoutController = c;
    }

    @FXML
    public void initialize() {
        boolean admin = FinanceSessionManager.isAdmin();
        if (lblAccessDenied != null) {
            lblAccessDenied.setVisible(!admin);
            lblAccessDenied.setManaged(!admin);
        }
        if (toolbarBox != null) {
            toolbarBox.setVisible(admin);
            toolbarBox.setManaged(admin);
        }
        if (tableBox != null) {
            tableBox.setVisible(admin);
            tableBox.setManaged(admin);
        }
        if (!admin) return;

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(1);
        if (dpFrom != null) dpFrom.setValue(from);
        if (dpTo != null) dpTo.setValue(to);

        if (cmbSource != null) {
            cmbSource.setItems(FXCollections.observableArrayList(
                    "All sources", "Customer updates", "Stock movements"));
            cmbSource.setValue("All sources");
        }

        if (exportMenuButton != null && !FinanceSessionManager.isAdmin()) {
            exportMenuButton.setVisible(false);
            exportMenuButton.setManaged(false);
        }

        bindTable();
        tblAudit.setItems(items);
        tblAudit.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        if (btnApply != null) btnApply.setOnAction(e -> loadData());
        if (cmbSource != null) cmbSource.valueProperty().addListener((o, a, b) -> loadData());
        if (dpFrom != null) dpFrom.valueProperty().addListener((o, a, b) -> loadData());
        if (dpTo != null) dpTo.valueProperty().addListener((o, a, b) -> loadData());

        loadData();
    }

    private void bindTable() {
        colDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().occurredAt()));
        colSource.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().source()));
        colUser.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().userLabel()));
        colAction.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().action()));
        colEntity.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().entity()));
        colDetails.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().details()));
    }

    private String mapSourceFilter() {
        String v = cmbSource != null && cmbSource.getValue() != null ? cmbSource.getValue() : "All sources";
        return switch (v) {
            case "Customer updates" -> "Customer";
            case "Stock movements" -> "Stock";
            default -> "All";
        };
    }

    private void loadData() {
        LocalDate from = dpFrom != null ? dpFrom.getValue() : null;
        LocalDate to = dpTo != null ? dpTo.getValue() : null;
        String filter = mapSourceFilter();

        Task<List<FinanceAuditLogDao.MergedAuditRow>> task = new Task<>() {
            @Override
            protected List<FinanceAuditLogDao.MergedAuditRow> call() throws Exception {
                return auditLogDao.findMerged(from, to, filter);
            }
        };
        task.setOnSucceeded(e -> {
            List<FinanceAuditLogDao.MergedAuditRow> list = task.getValue();
            items.setAll(list != null ? list : List.of());
        });
        task.setOnFailed(ev -> {
            if (task.getException() != null) log.error("Error", task.getException());
            Platform.runLater(() -> items.clear());
        });
        executor.execute(task);
    }

    @FXML
    private void handleExportCsv() {
        export("csv");
    }

    @FXML
    private void handleExportPdf() {
        export("pdf");
    }

    private void export(String format) {
        if (!FinanceSessionManager.isAdmin()) return;
        Window w = tblAudit != null && tblAudit.getScene() != null
                ? tblAudit.getScene().getWindow() : null;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export audit log");
        String ext = "csv".equals(format) ? "*.csv" : "*.pdf";
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "csv".equals(format) ? "CSV Files" : "PDF Files", ext));
        fc.setInitialFileName("audit_log." + format);
        File file = w != null ? fc.showSaveDialog(w) : null;
        if (file == null) return;
        try {
            List<String[]> data = buildExportData();
            if ("csv".equals(format)) FinanceExportService.exportRowsToCSV(data, file);
            else FinanceExportService.exportRowsToPDF("Audit Log", data, file);
            toast("success", format.toUpperCase() + " exported: " + file.getName());
        } catch (Exception ex) {
            toast("error", "Export failed: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown"));
        }
    }

    private List<String[]> buildExportData() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Time", "Source", "FinanceUser", "Action", "Entity", "Details"});
        for (FinanceAuditLogDao.MergedAuditRow r : items) {
            rows.add(new String[]{
                    r.occurredAt(), r.source(), r.userLabel(), r.action(), r.entity(), r.details()
            });
        }
        return rows;
    }

    private void toast(String type, String msg) {
        if (mainLayoutController != null) mainLayoutController.showToast(type, msg);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void refreshVisibleData() {
        loadData();
    }
}

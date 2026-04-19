package Controllers;

import com.reaz.customer.dao.CustomerAdminDAO;
import com.reaz.customer.dao.CustomerOrderDAO;
import com.reaz.customer.dao.CustomerPreferenceDAO;
import com.reaz.customer.model.CustomerOrder;
import com.reaz.customer.model.CustomerPreference;
import com.reaz.customer.model.CustomerProfile;
import com.reaz.customer.dao.CustomerDAO;
import com.reaz.customer.model.CustomerUser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomerStaffEditController {

    @FXML private Label         customerNameLabel;
    @FXML private Label         customerIdLabel;
    @FXML private Label         customerSpentLabel;
    @FXML private Label         customerStatusLabel;

    @FXML private Button        tabProfile;
    @FXML private Button        tabPreferences;
    @FXML private Button        tabOrders;

    @FXML private VBox          profilePane;
    @FXML private VBox          preferencesPane;
    @FXML private VBox          ordersPane;

    // Profile tab (staff: read-only email/idcard)
    @FXML private Label         profileMessageLabel;
    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     phoneField;
    @FXML private Label         phoneErrorLabel;
    @FXML private TextArea      addressField;
    @FXML private TextField     emailField;
    @FXML private Label         idCardLabel;
    @FXML private Button        toggleStatusBtn;

    // Preferences tab
    @FXML private Label         preferencesMessageLabel;
    @FXML private FlowPane      categoriesPane;
    @FXML private ComboBox<String> notificationsCombo;
    @FXML private TextArea      deliveryInstructionsField;

    // Orders tab
    @FXML private TableView<CustomerOrder>          ordersTable;
    @FXML private TableColumn<CustomerOrder, Integer> colOrderId;
    @FXML private TableColumn<CustomerOrder, String>  colRobotType;
    @FXML private TableColumn<CustomerOrder, String>  colDate;
    @FXML private TableColumn<CustomerOrder, String>  colStatus;
    @FXML private TableColumn<CustomerOrder, String>  colAmount;

    private final CustomerDAO           customerDAO   = new CustomerDAO();
    private final CustomerAdminDAO      adminDAO      = new CustomerAdminDAO();
    private final CustomerPreferenceDAO prefDAO       = new CustomerPreferenceDAO();
    private final CustomerOrderDAO      orderDAO      = new CustomerOrderDAO();

    private CustomerUser adminUser;
    private CustomerUser targetUser;

    private static final List<String> CATEGORIES = Arrays.asList(
        "Home Assistants", "Security Bots", "Educational", "Companions", "Industrial");
    private static final List<String> NOTIF_OPTIONS = Arrays.asList("EMAIL", "SMS", "NONE");

    public void setContext(CustomerUser admin, CustomerUser target) {
        this.adminUser  = admin;
        this.targetUser = target;
        loadData();
    }

    @FXML
    public void initialize() {
        notificationsCombo.setItems(FXCollections.observableArrayList(NOTIF_OPTIONS));
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colRobotType.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("formattedAmount"));
        buildCategoryCheckboxes(new HashSet<>());
    }

    private void loadData() {
        customerNameLabel.setText(targetUser.getName());
        customerIdLabel.setText("ID: " + targetUser.getId());
        boolean active = targetUser.isActive();
        customerStatusLabel.setText(active ? "Active" : "Inactive");
        toggleStatusBtn.setText(active ? "Deactivate" : "Activate");

        try {
            double spent = orderDAO.getTotalSpentByUserId(targetUser.getId());
            customerSpentLabel.setText("Total Spent: £" + String.format("%,.2f", spent));
        } catch (Exception e) { customerSpentLabel.setText("Total Spent: £0.00"); }

        String[] parts = targetUser.getName().split(" ", 2);
        firstNameField.setText(parts[0]);
        lastNameField.setText(parts.length > 1 ? parts[1] : "");
        emailField.setText(targetUser.getEmail());

        try {
            CustomerProfile profile = customerDAO.getProfile(targetUser.getId());
            if (profile != null) {
                phoneField.setText(nvl(profile.getPhone()));
                addressField.setText(nvl(profile.getAddress()));
                idCardLabel.setText(profile.getIdCardFileName());
            }
        } catch (Exception e) { e.printStackTrace(); }

        try {
            CustomerPreference pref = prefDAO.getByUserId(targetUser.getId());
            if (pref != null) {
                Set<String> sel = new HashSet<>(Arrays.asList(nvl(pref.getPreferredCategories()).split(",")));
                buildCategoryCheckboxes(sel);
                if (pref.getNotificationSettings() != null)
                    notificationsCombo.setValue(pref.getNotificationSettings());
                deliveryInstructionsField.setText(nvl(pref.getDeliveryInstructions()));
            }
        } catch (Exception e) { e.printStackTrace(); }

        try {
            ordersTable.setItems(FXCollections.observableArrayList(
                orderDAO.getOrdersByUserId(targetUser.getId())));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleTabProfile() {
        setActive(profilePane, tabProfile);
        setInactive(preferencesPane, tabPreferences);
        setInactive(ordersPane, tabOrders);
    }
    @FXML private void handleTabPreferences() {
        setActive(preferencesPane, tabPreferences);
        setInactive(profilePane, tabProfile);
        setInactive(ordersPane, tabOrders);
    }
    @FXML private void handleTabOrders() {
        setActive(ordersPane, tabOrders);
        setInactive(profilePane, tabProfile);
        setInactive(preferencesPane, tabPreferences);
    }

    @FXML
    private void handleSaveProfile() {
        profileMessageLabel.setText("");
        phoneErrorLabel.setText("");
        try {
            int adminId = adminUser != null ? adminUser.getId() : 0;
            adminDAO.updateProfile(
                targetUser.getId(),
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                phoneField.getText().trim(),
                addressField.getText().trim(),
                adminId
            );
            profileMessageLabel.setStyle("-fx-text-fill: green;");
            profileMessageLabel.setText("Profile saved.");
        } catch (Exception e) {
            profileMessageLabel.setStyle("-fx-text-fill: red;");
            profileMessageLabel.setText("Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleStatus() {
        try {
            int adminId = adminUser != null ? adminUser.getId() : 0;
            adminDAO.toggleCustomerStatus(targetUser.getId(), adminId);
            boolean nowActive = !targetUser.isActive();
            targetUser.setStatus(nowActive ? "ACTIVE" : "INACTIVE");
            customerStatusLabel.setText(nowActive ? "Active" : "Inactive");
            toggleStatusBtn.setText(nowActive ? "Deactivate" : "Activate");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSavePreferences() {
        preferencesMessageLabel.setText("");
        try {
            StringBuilder sel = new StringBuilder();
            for (javafx.scene.Node n : categoriesPane.getChildren()) {
                if (n instanceof CheckBox cb && cb.isSelected()) {
                    if (sel.length() > 0) sel.append(",");
                    sel.append(cb.getText());
                }
            }
            String notif = notificationsCombo.getValue() != null ? notificationsCombo.getValue() : "NONE";
            prefDAO.savePreferences(targetUser.getId(), sel.toString(), notif,
                deliveryInstructionsField.getText().trim());
            preferencesMessageLabel.setStyle("-fx-text-fill: green;");
            preferencesMessageLabel.setText("Preferences saved.");
        } catch (Exception e) {
            preferencesMessageLabel.setStyle("-fx-text-fill: red;");
            preferencesMessageLabel.setText("Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerAdminDashboard.fxml"));
            Stage stage = (Stage) customerNameLabel.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            CustomerAdminDashboardController ctrl = loader.getController();
            ctrl.setUser(adminUser);
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerWelcome.fxml"));
            Stage stage = (Stage) customerNameLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), stage.getWidth(), stage.getHeight()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void buildCategoryCheckboxes(Set<String> selected) {
        categoriesPane.getChildren().clear();
        for (String cat : CATEGORIES) {
            CheckBox cb = new CheckBox(cat);
            cb.setSelected(selected.contains(cat));
            cb.setStyle("-fx-padding: 4 10; -fx-border-color: #E5E7EB; -fx-border-radius: 4; " +
                        "-fx-background-color: white; -fx-background-radius: 4;");
            categoriesPane.getChildren().add(cb);
        }
    }

    private void setActive(VBox p, Button b) {
        p.setVisible(true); p.setManaged(true);
        b.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand;");
    }
    private void setInactive(VBox p, Button b) {
        p.setVisible(false); p.setManaged(false);
        b.setStyle("-fx-background-color: transparent; -fx-border-color: #E5E7EB; -fx-padding: 8 20; -fx-cursor: hand;");
    }
    private String nvl(String s) { return s != null ? s : ""; }
}

package lk.ijse.dep7.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import lk.ijse.dep7.dbutils.SingleConnectionDataSource;
import lk.ijse.dep7.dto.ItemDTO;
import lk.ijse.dep7.exception.FailedOperationException;
import lk.ijse.dep7.exception.NotFoundException;
import lk.ijse.dep7.service.CustomerService;
import lk.ijse.dep7.service.ItemService;
import lk.ijse.dep7.util.OrderDetailTM;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;

public class PlaceOrderFormController {

    private final CustomerService customerService = new CustomerService(SingleConnectionDataSource.getInstance().getConnection());
    private final ItemService itemService = new ItemService(SingleConnectionDataSource.getInstance().getConnection());
    public AnchorPane root;
    public JFXButton btnPlaceOrder;
    public JFXTextField txtCustomerName;
    public JFXTextField txtDescription;
    public JFXTextField txtQtyOnHand;
    public JFXButton btnSave;
    public TableView<OrderDetailTM> tblOrderDetails;
    public JFXTextField txtUnitPrice;
    public JFXComboBox<String> cmbCustomerId;
    public JFXComboBox<String> cmbItemCode;
    public JFXTextField txtQty;
    public Label lblId;
    public Label lblDate;
    public Label lblTotal;

    public void initialize() throws FailedOperationException {

        tblOrderDetails.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("code"));
        tblOrderDetails.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("description"));
        tblOrderDetails.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("qty"));
        tblOrderDetails.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        tblOrderDetails.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("total"));
        TableColumn<OrderDetailTM, Button> lastCol = (TableColumn<OrderDetailTM, Button>) tblOrderDetails.getColumns().get(5);
        lastCol.setCellValueFactory(param -> {
            Button btnDelete = new Button("Delete");
            btnDelete.setOnAction(event -> {
               tblOrderDetails.getItems().remove(param.getValue());
            });

            return new ReadOnlyObjectWrapper<>(btnDelete);
        });

        /* Todo: We need to generate and set a new order id */

        lblDate.setText(LocalDate.now().toString());
        btnPlaceOrder.setDisable(true);
        txtCustomerName.setFocusTraversable(false);
        txtCustomerName.setEditable(false);
        txtDescription.setFocusTraversable(false);
        txtDescription.setEditable(false);
        txtUnitPrice.setFocusTraversable(false);
        txtUnitPrice.setEditable(false);
        txtQtyOnHand.setFocusTraversable(false);
        txtQtyOnHand.setEditable(false);
        txtQty.setOnAction(event -> btnSave.fire());
        txtQty.setEditable(false);
        btnSave.setDisable(true);

        cmbCustomerId.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {

                try {
                    txtCustomerName.setText(customerService.findCustomer(newValue).getName());
                } catch (NotFoundException e) {
                    e.printStackTrace();    // This can't be happened with our UI design
                } catch (FailedOperationException e) {
                    new Alert(Alert.AlertType.ERROR, "Failed to load customer information").show();
                    throw new RuntimeException(e);
                }
            }
        });

        cmbItemCode.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newItemCode) -> {
            txtQty.setEditable(newItemCode != null);
            btnSave.setDisable(newItemCode == null);

            if (newItemCode != null) {

                try {
                    ItemDTO item = itemService.findItem(newItemCode);

                    txtDescription.setText(item.getDescription());
                    txtUnitPrice.setText(item.getUnitPrice().setScale(2).toString());
                    txtQtyOnHand.setText(item.getQtyOnHand() + "");

                } catch (NotFoundException e) {
                    e.printStackTrace();    // This can't be happened with our UI design
                } catch (FailedOperationException e) {
                    new Alert(Alert.AlertType.ERROR, "Failed to load item details").show();
                    throw new RuntimeException(e);
                }
            }else{
                txtDescription.clear();
                txtQty.clear();
                txtQtyOnHand.clear();
                txtUnitPrice.clear();
            }
        });

        loadAllCustomerIds();
        loadAllItemCodes();
    }

    private void loadAllCustomerIds() throws FailedOperationException {
        try {
            customerService.findAllCustomers().forEach(dto -> cmbCustomerId.getItems().add(dto.getId()));
        } catch (FailedOperationException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load customer ids").show();
            throw e;
        }
    }

    private void loadAllItemCodes() throws FailedOperationException {
        try {
            itemService.findAllItems().forEach(dto -> cmbItemCode.getItems().add(dto.getCode()));
        } catch (FailedOperationException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load item codes").show();
            throw e;
        }
    }

    @FXML
    private void navigateToHome(MouseEvent event) throws IOException {
        URL resource = this.getClass().getResource("/view/main-form.fxml");
        Parent root = FXMLLoader.load(resource);
        Scene scene = new Scene(root);
        Stage primaryStage = (Stage) (this.root.getScene().getWindow());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        Platform.runLater(() -> primaryStage.sizeToScene());
    }

    public void btnAdd_OnAction(ActionEvent actionEvent) {
        if (!txtQty.getText().matches("\\d+") || Integer.parseInt(txtQty.getText()) <= 0 ||
                Integer.parseInt(txtQty.getText()) > Integer.parseInt(txtQtyOnHand.getText())) {
            new Alert(Alert.AlertType.ERROR, "Invalid qty").show();
            txtQty.requestFocus();
            txtQty.selectAll();
            return;
        }

        String itemCode = cmbItemCode.getSelectionModel().getSelectedItem();
        String description = txtDescription.getText();
        BigDecimal unitPrice = new BigDecimal(txtUnitPrice.getText()).setScale(2);
        int qty = Integer.parseInt(txtQty.getText());
        BigDecimal total = unitPrice.multiply(new BigDecimal(qty)).setScale(2);

        tblOrderDetails.getItems().add(new OrderDetailTM(itemCode, description,qty, unitPrice, total));
        cmbItemCode.getSelectionModel().clearSelection();
        cmbItemCode.requestFocus();
    }

    public void txtQty_OnAction(ActionEvent actionEvent) {
    }

    public void btnPlaceOrder_OnAction(ActionEvent actionEvent) {
    }
}

package lk.ijse.dep7.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lk.ijse.dep7.dbutils.SingleConnectionDataSource;
import lk.ijse.dep7.dto.CustomerDTO;
import lk.ijse.dep7.exception.DuplicateIdentifierException;
import lk.ijse.dep7.exception.FailedOperationException;
import lk.ijse.dep7.exception.NotFoundException;
import lk.ijse.dep7.service.CustomerService;
import lk.ijse.dep7.util.CustomerTM;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ManageCustomersFormController {
    public AnchorPane root;
    public JFXTextField txtCustomerName;
    public JFXTextField txtCustomerId;
    public JFXButton btnDelete;
    public JFXButton btnSave;
    public JFXTextField txtCustomerAddress;
    public TableView<CustomerTM> tblCustomers;
    public JFXButton btnAddNewCustomer;
    private CustomerService customerService = new CustomerService(SingleConnectionDataSource.getInstance().getConnection());

    public void initialize() throws FailedOperationException {
        tblCustomers.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("id"));
        tblCustomers.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("name"));
        tblCustomers.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("address"));

        initUI();

        tblCustomers.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            btnDelete.setDisable(newValue == null);
            btnSave.setText(newValue != null? "Update": "Save");
            btnSave.setDisable(newValue == null);

            if (newValue != null) {
                txtCustomerId.setText(newValue.getId());
                txtCustomerName.setText(newValue.getName());
                txtCustomerAddress.setText(newValue.getAddress());

                txtCustomerId.setDisable(false);
                txtCustomerName.setDisable(false);
                txtCustomerAddress.setDisable(false);
            }
        });

        txtCustomerAddress.setOnAction(event -> btnSave.fire());
        loadAllCustomers();
    }

    private void loadAllCustomers() throws FailedOperationException {

        tblCustomers.getItems().clear();
        try {
            customerService.findAllCustomers().forEach(dto-> tblCustomers.getItems().add(new CustomerTM(dto.getId(), dto.getName(), dto.getAddress())));
        } catch (FailedOperationException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            throw e;
        }

    }

    private void initUI(){
        txtCustomerId.clear();
        txtCustomerName.clear();
        txtCustomerAddress.clear();
        txtCustomerId.setDisable(true);
        txtCustomerName.setDisable(true);
        txtCustomerAddress.setDisable(true);
        txtCustomerId.setEditable(false);
        btnSave.setDisable(true);
        btnDelete.setDisable(true);
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

    public void btnAddNew_OnAction(ActionEvent actionEvent) throws FailedOperationException {
        txtCustomerId.setDisable(false);
        txtCustomerName.setDisable(false);
        txtCustomerAddress.setDisable(false);
        txtCustomerId.clear();
        txtCustomerId.setText(generateNewId());
        txtCustomerName.clear();
        txtCustomerAddress.clear();
        txtCustomerName.requestFocus();
        btnSave.setDisable(false);
        btnSave.setText("Save");
        tblCustomers.getSelectionModel().clearSelection();
    }

    public void btnSave_OnAction(ActionEvent actionEvent) throws FailedOperationException {
        String id = txtCustomerId.getText();
        String name = txtCustomerName.getText();
        String address = txtCustomerAddress.getText();

        if (!name.matches("[A-Za-z ]+")){
            new Alert(Alert.AlertType.ERROR, "Invalid name").show();
            txtCustomerName.requestFocus();
            return;
        }else if (!address.matches(".{3,}")){
            new Alert(Alert.AlertType.ERROR, "Address should be at least 3 characters long").show();
            txtCustomerAddress.requestFocus();
            return;
        }

        try {

            if (btnSave.getText().equalsIgnoreCase("save")) {

                try {
                    customerService.saveCustomer(new CustomerDTO(id, name, address));
                    tblCustomers.getItems().add(new CustomerTM(id, name, address));
                } catch (DuplicateIdentifierException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }

            } else {

                try {
                    customerService.updateCustomer(new CustomerDTO(id, name, address));
                    CustomerTM selectedCustomer = tblCustomers.getSelectionModel().getSelectedItem();
                    selectedCustomer.setName(name);
                    selectedCustomer.setAddress(address);
                    tblCustomers.refresh();
                } catch (NotFoundException e) {
                    // This is never going to happen
                    e.printStackTrace();
                }
            }

        }catch (FailedOperationException  e){
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            throw e;
        }

        btnAddNewCustomer.fire();
    }

    public void btnDelete_OnAction(ActionEvent actionEvent) throws FailedOperationException {

        try {
            customerService.deleteCustomer(tblCustomers.getSelectionModel().getSelectedItem().getId());
            tblCustomers.getItems().remove(tblCustomers.getSelectionModel().getSelectedItem());
            tblCustomers.getSelectionModel().clearSelection();
            initUI();
        } catch (NotFoundException e) {
            e.printStackTrace();        // This is never going to happen with our UI design
        } catch (FailedOperationException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            throw e;
        }
    }

    private String generateNewId() throws FailedOperationException {
        try {
            return customerService.generateNewCustomerId();
        } catch (FailedOperationException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            throw e;
        }
//        if (tblCustomers.getItems().isEmpty()){
//            return "C001";
//        }else{
//            String id = getLastCustomerId();
//            int newCustomerId = Integer.parseInt(id.replace("C", "")) + 1;
//            return String.format("C%03d", newCustomerId);
//        }
    }

    private String getLastCustomerId(){
        List<CustomerTM> tempCustomersList = new ArrayList<>(tblCustomers.getItems());
        Collections.sort(tempCustomersList);
        return tempCustomersList.get(tempCustomersList.size() - 1).getId();
    }

}

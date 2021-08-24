package lk.ijse.dep7.service;

import lk.ijse.dep7.dto.ItemDTO;
import lk.ijse.dep7.dto.OrderDTO;
import lk.ijse.dep7.dto.OrderDetailDTO;
import lk.ijse.dep7.exception.DuplicateIdentifierException;
import lk.ijse.dep7.exception.FailedOperationException;
import lk.ijse.dep7.exception.NotFoundException;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class OrderService {

    private final Connection connection;

    public OrderService(Connection connection) {
        this.connection = connection;
    }

    public void saveOrder(String orderId, LocalDate orderDate, String customerId, List<OrderDetailDTO> orderDetails) throws FailedOperationException, DuplicateIdentifierException, NotFoundException {

        final CustomerService customerService = new CustomerService(connection);
        final ItemService itemService = new ItemService(connection);

        try {
            PreparedStatement stm = connection.prepareStatement("SELECT id FROM `order` WHERE id=?");
            stm.setString(1, orderId);

            if (stm.executeQuery().next()) {
                throw new DuplicateIdentifierException(orderId + " already exists");
            }

            if (!customerService.existCustomer(customerId)) {
                throw new NotFoundException("Customer id doesn't exist");
            }

            connection.setAutoCommit(false);
            stm = connection.prepareStatement("INSERT INTO `order` (id, date, customer_id) VALUES (?,?,?)");
            stm.setString(1, orderId);
            stm.setDate(2, Date.valueOf(orderDate));
            stm.setString(3, customerId);

            if (stm.executeUpdate() != 1) {
                throw new FailedOperationException("Failed to save the order");
            }

            stm = connection.prepareStatement("INSERT INTO order_detail (order_id, item_code, unit_price, qty) VALUES (?,?,?,?)");

            for (OrderDetailDTO detail : orderDetails) {
                stm.setString(1, orderId);
                stm.setString(2, detail.getItemCode());
                stm.setBigDecimal(3, detail.getUnitPrice());
                stm.setInt(4, detail.getQty());

                if (stm.executeUpdate() != 1) {
                    throw new FailedOperationException("Failed to save some order details");
                }

                ItemDTO item = itemService.findItem(detail.getItemCode());
                item.setQtyOnHand(item.getQtyOnHand() - detail.getQty());
                itemService.updateItem(item);
            }

            connection.commit();

        } catch (SQLException e) {
            failedOperationExecutionContext(connection::rollback);
        } catch (Throwable t) {
            failedOperationExecutionContext(connection::rollback);
            throw t;
        } finally {
            failedOperationExecutionContext(() -> connection.setAutoCommit(true));
        }

    }

    public List<OrderDTO> searchOrders(String query){
        return null;
    }

    public String generateNewOrderId() throws FailedOperationException {
        try {
            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SELECT id FROM `order` ORDER BY id DESC LIMIT 1;");

            return rst.next() ? String.format("OD%03d", (Integer.parseInt(rst.getString("id").replace("OD", "")) + 1)) : "OD001";
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to generate a new order id", e);
        }
    }

    private void failedOperationExecutionContext(ExecutionContext context) throws FailedOperationException {
        try {
            context.execute();
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to save the order", e);
        }
    }

    @FunctionalInterface
    interface ExecutionContext {
        void execute() throws SQLException;
    }

}

package sample.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import sample.database.DBConnection;
import sample.model.User;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private TextField textField;  // username field
    @FXML
    private PasswordField passwordField;  // password field
    @FXML
    private Label login_info;  // info label

    public static User user = new User();

    private boolean isUser = false;
    private ActionEvent EVENT;

    public static String user_id, full_name, user_role;
    public static int num_books = 0, num_students = 0, totalBorrowedBooks = 0, myBooks = 0;

    @FXML
    public void login(ActionEvent event) {
        EVENT = event;
        isUser = false;

        String enteredUserId = textField.getText().trim();
        String enteredPassword = passwordField.getText();

        if (!enteredUserId.isEmpty() && !enteredPassword.isEmpty()) {
            user.setUser_id(enteredUserId);
            user.setPassword(enteredPassword);

            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ? AND password = ?")) {

                statement.setString(1, user.getUser_id());
                statement.setString(2, user.getPassword());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        isUser = true;
                        setUserDetails(user.getUser_id());

                        // Open correct main page based on role
                        switch (user_role) {
                            case "Admin":
                                openMainPage("../pages/MainAdminPage.fxml");
                                break;
                            case "Librarian":
                                openMainPage("../pages/MainLibrarianPage.fxml");
                                break;
                            case "Student":
                                openMainPage("../pages/MainStudentPage.fxml");
                                break;
                            default:
                                login_info.setText("User role not recognized!");
                                login_info.setTextFill(Color.RED);
                        }
                    }
                }

                if (!isUser) {
                    login_info.setText("User not found or incorrect password!");
                    login_info.setTextFill(Color.RED);
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                login_info.setText("An error occurred during login.");
                login_info.setTextFill(Color.RED);
            }
        } else {
            login_info.setText("Please enter both username and password!");
            login_info.setTextFill(Color.RED);
        }
    }

    public void openMainPage(String page) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(page));
        Stage stage1 = new Stage();
        stage1.setTitle("Library Management System");
        stage1.setScene(new Scene(root, 900, 610));
        stage1.show();

        Button button = (Button) (EVENT).getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }

    /**
     * Load user details (name, role, stats) based on user_id.
     */
    public static void setUserDetails(String userId) throws SQLException {
        user_id = userId;
        // Reset counters
        num_books = 0;
        num_students = 0;
        totalBorrowedBooks = 0;
        myBooks = 0;

        try (Connection connection = DBConnection.getConnection()) {
            // Fetch user full name and role
            String queryUser = """
                    SELECT u.full_name, r.role_name 
                    FROM users u 
                    INNER JOIN user_roles ur ON ur.user_id = u.user_id 
                    INNER JOIN roles r ON r.role_id = ur.role_id 
                    WHERE u.user_id = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(queryUser)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        full_name = rs.getString("full_name");
                        user_role = rs.getString("role_name");
                    }
                }
            }

            // Count total students
            String queryStudents = """
                    SELECT COUNT(DISTINCT u.user_id) AS total_students
                    FROM users u
                    INNER JOIN user_roles ur ON ur.user_id = u.user_id
                    INNER JOIN roles r ON r.role_id = ur.role_id
                    WHERE r.role_name = 'Student'
                    """;
            try (PreparedStatement ps = connection.prepareStatement(queryStudents);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    num_students = rs.getInt("total_students");
                }
            }

            // Count total books
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) AS total_books FROM books");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    num_books = rs.getInt("total_books");
                }
            }

            // Count total borrowed books
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) AS total_borrowed FROM borrowed_books");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalBorrowedBooks = rs.getInt("total_borrowed");
                }
            }

            // Count this user's borrowed books if student
            if ("Student".equals(user_role)) {
                String queryMyBooks = "SELECT COUNT(*) AS my_books FROM borrowed_books WHERE user_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(queryMyBooks)) {
                    ps.setString(1, user_id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            myBooks = rs.getInt("my_books");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Any initialization code here (if needed)
    }
}

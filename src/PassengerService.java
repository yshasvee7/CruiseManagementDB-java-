import java.sql.*;
import java.util.Scanner;

public class PassengerService {
    private final Connection conn;
    private final Scanner sc;

    public PassengerService(Connection conn, Scanner sc) {
        this.conn = conn;
        this.sc = sc;
    }

    // 1) Add passenger (reads input inside)
    public void addPassenger() {
        try {
            System.out.print("Enter Name: ");
            String name = sc.nextLine().trim();

            System.out.print("Enter Age: ");
            int age = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter Gender (M/F/O): ");
            String gender = sc.nextLine().trim();

            System.out.print("Enter Nationality: ");
            String nationality = sc.nextLine().trim();

            String sql = "INSERT INTO passenger(name, age, gender, nationality) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, name);
                pst.setInt(2, age);
                pst.setString(3, gender);
                pst.setString(4, nationality);

                int rows = pst.executeUpdate();
                if (rows > 0) System.out.println("Passenger added successfully.");
                else System.out.println("⚠ Passenger not added.");
            }
        } catch (Exception e) {
            System.out.println("Error while adding passenger: " + e.getMessage());
        }
    }

    // 2) View all passengers
    public void viewPassengers() {
        String sql = "SELECT passenger_id, name, age, gender, nationality FROM passenger ORDER BY passenger_id";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.printf("%-5s %-25s %-5s %-8s %-15s%n", "ID", "Name", "Age", "Gender", "Nationality");
            System.out.println("------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-25s %-5d %-8s %-15s%n",
                        rs.getInt("passenger_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        rs.getString("nationality")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error while fetching passengers: " + e.getMessage());
        }
    }

    // 3) Search passenger by id (reads id inside)
    public void searchPassenger() {
        try {
            System.out.print("Enter Passenger ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            String sql = "SELECT passenger_id, name, age, gender, nationality FROM passenger WHERE passenger_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("\nPassenger found:");
                        System.out.println("ID: " + rs.getInt("passenger_id"));
                        System.out.println("Name: " + rs.getString("name"));
                        System.out.println("Age: " + rs.getInt("age"));
                        System.out.println("Gender: " + rs.getString("gender"));
                        System.out.println("Nationality: " + rs.getString("nationality"));
                    } else {
                        System.out.println("Passenger not found with ID: " + id);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error while searching passenger: " + e.getMessage());
        }
    }

    // 4) Update passenger (reads id & details inside)
    public void updatePassenger() {
        try {
            System.out.print("Enter Passenger ID to update: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            // Check exists
            String check = "SELECT passenger_id FROM passenger WHERE passenger_id = ?";
            try (PreparedStatement pcheck = conn.prepareStatement(check)) {
                pcheck.setInt(1, id);
                try (ResultSet rs = pcheck.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("No passenger with ID " + id);
                        return;
                    }
                }
            }

            System.out.print("Enter New Name: ");
            String newName = sc.nextLine().trim();
            System.out.print("Enter New Age: ");
            int newAge = Integer.parseInt(sc.nextLine().trim());

            String sql = "UPDATE passenger SET name = ?, age = ? WHERE passenger_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, newName);
                pst.setInt(2, newAge);
                pst.setInt(3, id);

                int rows = pst.executeUpdate();
                if (rows > 0) System.out.println("Passenger updated successfully.");
                else System.out.println("⚠ No changes made.");
            }
        } catch (Exception e) {
            System.out.println("Error while updating passenger: " + e.getMessage());
        }
    }

    // 5) Delete passenger (reads id inside)
    public void deletePassenger() {
        try {
            System.out.print("Enter Passenger ID to delete: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            String sql = "DELETE FROM passenger WHERE passenger_id = ?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, id);
                int rows = pst.executeUpdate();
                if (rows > 0) System.out.println("🗑 Passenger deleted successfully.");
                else System.out.println("No passenger found with ID: " + id);
            }
        } catch (Exception e) {
            System.out.println("Error while deleting passenger: " + e.getMessage());
        }
    }
}

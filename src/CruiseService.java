import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class CruiseService {
    private Connection conn;
    private Scanner sc = new Scanner(System.in);

    public CruiseService(Connection conn) {
        this.conn = conn;
    }

    public static void viewAllCruises() {
        String query = "SELECT c.cruise_id, s.name AS ship_name, c.start_date, c.end_date, c.status " +
                "FROM cruise c JOIN ship s ON c.ship_id = s.ship_id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\n=== Cruise Schedule ===");
            System.out.println("ID | Ship Name | Start Date | End Date | Status");
            System.out.println("--------------------------------------------------");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("cruise_id") + " | " +
                        rs.getString("ship_name") + " | " +
                        rs.getString("start_date") + " | " +
                        rs.getString("end_date") + " | " +
                        rs.getString("status")
                );
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}


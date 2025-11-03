import java.util.Scanner;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        DBConnection db = new DBConnection();
        Connection conn = db.getConnection();

        if (conn == null) {
            System.out.println("Database Connection Failed! Exiting...");
            return;
        }

        PassengerService passengerService = new PassengerService(conn, sc);
        BookingService bookingService = new BookingService(conn, sc);
        //CruiseService cruiseService = new CruiseService(conn);

        while (true) {
            System.out.println("\n========== Cruise Management System ==========");
            System.out.println("1. Add Passenger");
            System.out.println("2. View All Passengers");
            System.out.println("3. Search Passenger by ID");
            System.out.println("4. Update Passenger Details");
            System.out.println("5. Delete Passenger");
            System.out.println("6. View All Cruises");
            System.out.println("7. Book Ticket (book + pay + generate ticket)");
            System.out.println("8. View Bookings by Passenger");
            System.out.println("0. Exit");
            System.out.print("Enter Your Choice: ");

            int choice = Integer.parseInt(sc.nextLine().trim());

            switch (choice) {
                case 1 -> passengerService.addPassenger();
                case 2 -> passengerService.viewPassengers();
                case 3 -> passengerService.searchPassenger();
                case 4 -> passengerService.updatePassenger();
                case 5 -> passengerService.deletePassenger();
                case 6 -> CruiseService.viewAllCruises();
                case 7 -> bookingService.addBookingSingleFlow();
                case 8 -> bookingService.viewBookingsByPassenger();
                case 0 -> {
                    System.out.println("Exiting... Thank you!");
                    System.exit(0);
                }
                default -> System.out.println("Invalid Choice! Try again.");
            }
        }
    }
}

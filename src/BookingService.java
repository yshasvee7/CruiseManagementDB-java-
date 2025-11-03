// BookingService.java
import java.sql.*;
import java.util.Scanner;

public class BookingService {
    private final Connection conn;
    private final Scanner sc;

    public BookingService(Connection conn, Scanner sc) {
        this.conn = conn;
        this.sc = sc;
    }

    private double calculatePrice(String cabinClass) {
        return switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> 15000.00;
            case "STANDARD" -> 25000.00;
            case "LUXURY" -> 45000.00;
            default -> 0.00;
        };
    }

    private boolean passengerExists(int pid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM passenger WHERE passenger_id = ?")) {
            ps.setInt(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean cruiseExists(int cid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM cruise WHERE cruise_id = ?")) {
            ps.setInt(1, cid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Single-step: book, pay, generate ticket (auto)
     */
    public void addBookingSingleFlow() {
        try {
            System.out.print("Enter Passenger ID: ");
            int passengerId = Integer.parseInt(sc.nextLine().trim());
            if (!passengerExists(passengerId)) {
                System.out.println("Invalid Passenger ID. Please add passenger first or choose a valid ID.");
                return;
            }

            System.out.print("Enter Cruise ID: ");
            int cruiseId = Integer.parseInt(sc.nextLine().trim());
            if (!cruiseExists(cruiseId)) {
                System.out.println("Invalid Cruise ID. Choose a valid cruise from View All Cruises.");
                return;
            }

            System.out.print("Enter Cabin Class (ECONOMY/STANDARD/LUXURY): ");
            String cabinClass = sc.nextLine().trim().toUpperCase();

            double price = calculatePrice(cabinClass);
            if (price <= 0) {
                System.out.println("Invalid cabin class entered.");
                return;
            }

            // insert booking
            String insert = "INSERT INTO booking (passenger_id, cruise_id, cabin_class, price, status, payment_status) VALUES (?, ?, ?, ?, 'CONFIRMED', 'PENDING')";
            int bookingId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, passengerId);
                ps.setInt(2, cruiseId);
                ps.setString(3, cabinClass);
                ps.setDouble(4, price);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) bookingId = gk.getInt(1);
                    }
                }
            }

            if (bookingId == -1) {
                System.out.println("Booking failed.");
                return;
            }

            System.out.printf("Booking confirmed. Booking ID: %d  Price: ₹%.2f%n", bookingId, price);
            System.out.println("Proceeding to payment...");

            // process payment immediately (simulate success)
            String mode;
            System.out.println("Choose payment mode: 1-UPI 2-CARD 3-NETBANKING 4-CASH");
            int opt = Integer.parseInt(sc.nextLine().trim());
            switch (opt) {
                case 1 -> mode = "UPI";
                case 2 -> mode = "CARD";
                case 3 -> mode = "NETBANKING";
                default -> mode = "CASH";
            }

            String ins = "INSERT INTO payment (booking_id, amount, mode, status) VALUES (?, ?, ?, 'PAID')";
            int paymentId = -1;
            try (PreparedStatement ps2 = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps2.setInt(1, bookingId);
                ps2.setDouble(2, price);
                ps2.setString(3, mode);
                int r = ps2.executeUpdate();
                if (r > 0) {
                    try (ResultSet gk = ps2.getGeneratedKeys()) {
                        if (gk.next()) paymentId = gk.getInt(1);
                    }
                }
            }

            // update booking payment_status
            try (PreparedStatement up = conn.prepareStatement("UPDATE booking SET payment_status = 'PAID' WHERE booking_id = ?")) {
                up.setInt(1, bookingId);
                up.executeUpdate();
            }

            System.out.println("Payment successful. Payment ID: " + (paymentId == -1 ? "N/A" : paymentId));
            System.out.println("Generating ticket PDF...");

            // generate ticket
            TicketGenerator.generateTicketPDF(bookingId, conn);

        } catch (Exception e) {
            System.out.println("Error in booking/payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * legacy method kept: view bookings by passenger
     */
    public void viewBookingsByPassenger() {
        try {
            System.out.print("Enter Passenger ID: ");
            int passengerId = Integer.parseInt(sc.nextLine().trim());

            String sql = "SELECT b.booking_id, b.cruise_id, b.booking_date, b.cabin_class, b.price, b.payment_status FROM booking b WHERE b.passenger_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, passengerId);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("BookingID | CruiseID | BookingDate | Cabin | Price | PaymentStatus");
                    while (rs.next()) {
                        System.out.printf("%d | %d | %s | %s | ₹%.2f | %s%n",
                                rs.getInt("booking_id"),
                                rs.getInt("cruise_id"),
                                rs.getTimestamp("booking_date"),
                                rs.getString("cabin_class"),
                                rs.getDouble("price"),
                                rs.getString("payment_status")
                        );
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (You can keep or add your other booking-related methods here)
}


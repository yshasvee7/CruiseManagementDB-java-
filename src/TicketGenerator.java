// TicketGenerator.java
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

public class TicketGenerator {

    private static final String[] ASSET_PATHS = {"assets/", "../assets/"};
    private static final String LOGO_FILE = "logo.png";
    private static final String WATERMARK_FILE = "watermark.png";
    private static final String FONT_REGULAR = "Roboto-Regular.ttf";
    private static final String FONT_BOLD = "Roboto-Black.ttf";
    private static final String OUTPUT_DIR = "tickets";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private static File findAsset(String name) {
        for (String base : ASSET_PATHS) {
            File f = new File(base + name);
            if (f.exists()) return f.getAbsoluteFile();
        }
        return new File("assets/" + name);
    }

    public static void generateTicketPDF(int bookingId, Connection conn) {
        String sql = """
                SELECT b.booking_id, b.booking_date, b.cabin_class, b.price,
                       p.name AS passenger_name, p.age, p.gender, p.nationality,
                       s.name AS ship_name,
                       r1.port_id AS start_port_id, r1.departure AS departure_time,
                       r2.port_id AS dest_port_id, r2.estimated_arrival AS est_arrival, r2.duration_hours
                FROM booking b
                JOIN passenger p ON b.passenger_id = p.passenger_id
                JOIN cruise c ON b.cruise_id = c.cruise_id
                JOIN ship s ON c.ship_id = s.ship_id
                LEFT JOIN cruise_route r1 ON r1.cruise_id = c.cruise_id AND r1.stop_order = 1
                LEFT JOIN cruise_route r2 ON r2.cruise_id = c.cruise_id
                WHERE b.booking_id = ?;
                """;

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bookingId);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Booking not found: " + bookingId);
                    return;
                }

                String passengerName = rs.getString("passenger_name");
                int age = rs.getInt("age");
                String gender = rs.getString("gender");
                String nationality = rs.getString("nationality");
                String cabinClass = rs.getString("cabin_class");
                double price = rs.getDouble("price");

                Timestamp bookingTs = rs.getTimestamp("booking_date");
                String bookingDateStr = bookingTs != null ? bookingTs.toLocalDateTime().format(DT_FMT) : "TBD";

                String shipName = rs.getString("ship_name");

                Timestamp depTs = rs.getTimestamp("departure_time");
                Integer durationHours = rs.getInt("duration_hours");
                Timestamp estArrTs = rs.getTimestamp("est_arrival");

                LocalDateTime departure = (depTs != null) ? depTs.toLocalDateTime() : null;
                LocalDateTime arrival = (estArrTs != null) ? estArrTs.toLocalDateTime()
                        : (departure != null && durationHours > 0 ? departure.plusHours(durationHours) : null);

                String departureStr = (departure != null) ? departure.format(DT_FMT) : "TBD";
                String arrivalStr = (arrival != null) ? arrival.format(DT_FMT) : "TBD";

                String startPort = getPortName(conn, rs.getInt("start_port_id"));
                String destPort = getPortName(conn, rs.getInt("dest_port_id"));
                String txn = fetchLatestTxn(conn, bookingId);

                File outdir = new File(OUTPUT_DIR);
                if (!outdir.exists()) outdir.mkdirs();
                String outFile = OUTPUT_DIR + File.separator + "Ticket_" + bookingId + ".pdf";

                try (PDDocument doc = new PDDocument()) {
                    File fReg = findAsset(FONT_REGULAR);
                    File fBold = findAsset(FONT_BOLD);
                    PDFont fontRegular = PDType0Font.load(doc, fReg);
                    PDFont fontBold = PDType0Font.load(doc, fBold);

                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);

                    PDPageContentStream cs = new PDPageContentStream(doc, page);
                    PDRectangle rect = page.getMediaBox();

                    drawRoundedBorder(cs, 20, 20, rect.getWidth() - 40, rect.getHeight() - 40, 12, 2f);

                    try {
                        File fWm = findAsset(WATERMARK_FILE);
                        PDImageXObject wm = PDImageXObject.createFromFileByContent(fWm, doc);
                        float wmWidth = rect.getWidth() * 0.7f;
                        float scale = wmWidth / wm.getWidth();
                        float wmHeight = wm.getHeight() * scale;
                        cs.drawImage(wm, (rect.getWidth() - wmWidth) / 2f, (rect.getHeight() - wmHeight) / 2f, wmWidth, wmHeight);
                    } catch (Exception ignored) {}

                    // Enlarged logo by 33%
                    try {
                        File fLogo = findAsset(LOGO_FILE);
                        PDImageXObject logo = PDImageXObject.createFromFileByContent(fLogo, doc);
                        float logoW = 213f; // 160 * 1.33
                        float logoH = (logo.getHeight() * logoW) / logo.getWidth();
                        float logoX = (rect.getWidth() - logoW) / 2f;
                        float logoY = rect.getHeight() - logoH - 60f;
                        cs.drawImage(logo, logoX, logoY, logoW, logoH);
                    } catch (Exception ignored) {}

                    float left = 60f;
                    float y = rect.getHeight() - 200f;
                    float lead = 22f;

                    cs.beginText();
                    cs.setFont(fontBold, 14);
                    cs.newLineAtOffset(left, y);
                    cs.showText("PASSENGER DETAILS");
                    cs.endText();

                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Name: " + passengerName);
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Age: " + age + "    Gender: " + gender + "    Nationality: " + (nationality == null ? "N/A" : nationality));

                    y -= lead * 1.3f;

                    cs.beginText();
                    cs.setFont(fontBold, 14);
                    cs.newLineAtOffset(left, y);
                    cs.showText("CRUISE DETAILS");
                    cs.endText();

                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Ship: " + shipName);
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Route: " + startPort + " -> " + destPort);
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Departure: " + departureStr);
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Estimated Arrival: " + arrivalStr);

                    y -= lead * 1.3f;

                    cs.beginText();
                    cs.setFont(fontBold, 14);
                    cs.newLineAtOffset(left, y);
                    cs.showText("BOOKING & PAYMENT");
                    cs.endText();

                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Booking ID: " + bookingId);
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, String.format("Cabin: %s    Amount Paid: ₹ %.2f", cabinClass, price));
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Booking Date: " + bookingDateStr);
                    y -= lead;
                    writeLine(cs, fontRegular, 13, left, y, "Transaction: " + txn);

                    // QR moved to bottom-left (not extreme)
                    try {
                        String qrText = String.format("BOOKING|%d|%s|%s->%s|%s|%.2f", bookingId, passengerName, startPort, destPort, cabinClass, price);
                        qrText = qrText.replace("→", "->");
                        BufferedImage qrImg = generateQR(qrText, 300, 300);
                        if (qrImg != null) {
                            PDImageXObject qr = LosslessFactory.createFromImage(doc, qrImg);
                            float qrW = 160f;
                            float qrH = 160f;
                            float qrX = 80f;
                            float qrY = 80f;
                            cs.drawImage(qr, qrX, qrY, qrW, qrH);
                            writeLine(cs, fontRegular, 12, qrX, qrY - 18f, "Scan to Verify Booking Details");
                        }
                    } catch (Exception ignored) {}

                    cs.close();
                    doc.save(outFile);
                    System.out.println("Ticket created: " + outFile);
                    Desktop.getDesktop().open(new File(outFile));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void drawRoundedBorder(PDPageContentStream cs, float x, float y, float w, float h, float r, float lineWidth) throws Exception {
        final float K = 0.5522847498f;
        float k = K * r;

        cs.setLineWidth(lineWidth);
        cs.moveTo(x + r, y);
        cs.lineTo(x + w - r, y);
        cs.curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        cs.lineTo(x + w, y + h - r);
        cs.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        cs.lineTo(x + r, y + h);
        cs.curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        cs.lineTo(x, y + r);
        cs.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        cs.stroke();
    }

    private static void writeLine(PDPageContentStream cs, PDFont font, int size, float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text.replace("→", "->"));
        cs.endText();
    }

    private static String getPortName(Connection c, int portId) {
        if (portId <= 0) return "Unknown";
        try (PreparedStatement pst = c.prepareStatement("SELECT name FROM port WHERE port_id = ?")) {
            pst.setInt(1, portId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private static String fetchLatestTxn(Connection conn, int bookingId) {
        try (PreparedStatement pst = conn.prepareStatement("SELECT payment_id FROM payment WHERE booking_id = ? ORDER BY payment_date DESC LIMIT 1")) {
            pst.setInt(1, bookingId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return "TXN" + rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return "N/A";
    }

    private static BufferedImage generateQR(String text, int width, int height) throws WriterException {
        BitMatrix bm = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bm);
    }
}

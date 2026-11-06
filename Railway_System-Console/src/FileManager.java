import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path TRAINS_FILE = DATA_DIR.resolve("trains.csv");
    private static final Path BOOKINGS_FILE = DATA_DIR.resolve("bookings.csv");
    private static final String BOOKING_HEADER =
            "bookingId,trainId,travelDate,customerId,customerName,contact,seats,status";
    public List<Train> loadTrains() throws IOException {
        if (!Files.exists(TRAINS_FILE)) {
            throw new IOException("Missing data/trains.csv. Run the program from the project folder.");
        }
        List<Train> trains = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(TRAINS_FILE, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // column headings
            if (line == null) throw new IOException("Timetable file is empty.");
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (p.length != 9) throw new IOException("Invalid train CSV row " + lineNumber);
                try {
                    int capacity = Integer.parseInt(p[7].trim());
                    if (capacity < 1) throw new IllegalArgumentException();
                    trains.add(new Train(p[0].trim(), p[1].trim(), p[2].trim(),
                            p[3].trim(), p[4].trim(), LocalTime.parse(p[5].trim()),
                            LocalTime.parse(p[6].trim()), capacity, p[8].trim()));
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid train details on row " + lineNumber, ex);
                }
            }
        }
        if (trains.isEmpty()) throw new IOException("No trains found in data/trains.csv.");
        return trains;
    }
    public List<Booking> loadBookings() throws IOException {
        List<Booking> bookings = new ArrayList<>();
        Files.createDirectories(DATA_DIR);
        if (!Files.exists(BOOKINGS_FILE)) {
            Files.write(BOOKINGS_FILE, (BOOKING_HEADER + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8));
            return bookings;
        }
        try (BufferedReader reader = Files.newBufferedReader(BOOKINGS_FILE, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null || !line.equals(BOOKING_HEADER)) {
                throw new IOException("Invalid booking CSV header. Expected: " + BOOKING_HEADER);
            }
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (p.length != 8) throw new IOException("Invalid booking CSV row " + lineNumber);
                try {
                    int seats = Integer.parseInt(p[6].trim());
                    if (seats < 1 || seats > 4 || !(p[7].equals("ACTIVE") || p[7].equals("CANCELLED"))) {
                        throw new IllegalArgumentException();
                    }
                    bookings.add(new Booking(p[0], p[1], LocalDate.parse(p[2]),
                            p[3], p[4], p[5], seats, p[7]));
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid booking details on row " + lineNumber, ex);
                }
            }
        }
        return bookings;
    }
    public void saveBookings(List<Booking> bookings) throws IOException {
        Files.createDirectories(DATA_DIR);
        Path temp = Files.createTempFile(DATA_DIR, "bookings-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write(BOOKING_HEADER);
                writer.newLine();
                for (Booking b : bookings) {
                    writer.write(String.join(",", b.getBookingId(), b.getTrainId(),
                            b.getTravelDate().toString(), b.getCustomerId(),
                            b.getCustomerName(), b.getContact(),
                            Integer.toString(b.getSeats()), b.getStatus()));
                    writer.newLine();
                }
            }
            try {
                Files.move(temp, BOOKINGS_FILE, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, BOOKINGS_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
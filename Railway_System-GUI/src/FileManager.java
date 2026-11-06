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
import java.util.Arrays;
import java.util.List;

public class FileManager {
    private static final Path DATA = Paths.get("data");
    private static final Path TRAINS = DATA.resolve("trains.csv");
    private static final Path BOOKINGS = DATA.resolve("bookings.csv");
    private static final String TRAIN_HEADER =
            "trainId,name,route,from,to,departure,arrival,carriages,seatsPerCarriage,serviceDays";
    private static final String BOOKING_HEADER =
            "bookingId,trainId,travelDate,customerId,customerName,contact,seatIds,status";

    public List<Train> loadTrains() throws IOException {
        if (!Files.exists(TRAINS)) {
            throw new IOException("Missing data/trains.csv. Open the program from the project folder.");
        }
        List<Train> result = new ArrayList<>();
        try (BufferedReader in = Files.newBufferedReader(TRAINS, StandardCharsets.UTF_8)) {
            String header = in.readLine();
            if (!TRAIN_HEADER.equals(header)) throw new IOException("Incorrect trains.csv column headings.");
            String row;
            int line = 1;
            while ((row = in.readLine()) != null) {
                line++;
                if (row.trim().isEmpty()) continue;
                String[] p = row.split(",", -1);
                if (p.length != 10) throw new IOException("Incorrect train CSV row " + line);
                try {
                    Train train = new Train(p[0].trim(), p[1].trim(), p[2].trim(),
                            p[3].trim(), p[4].trim(), LocalTime.parse(p[5].trim()),
                            LocalTime.parse(p[6].trim()), Integer.parseInt(p[7].trim()),
                            Integer.parseInt(p[8].trim()), p[9].trim());
                    if (train.getId().isEmpty()) throw new IllegalArgumentException();
                    for (Train old : result) {
                        if (old.getId().equalsIgnoreCase(train.getId())) {
                            throw new IllegalArgumentException("Duplicate train ID");
                        }
                    }
                    result.add(train);
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid train details in CSV row " + line, ex);
                }
            }
        }
        if (result.isEmpty()) throw new IOException("No trains found in data/trains.csv.");
        return result;
    }

    public List<Booking> loadBookings() throws IOException {
        Files.createDirectories(DATA);
        if (!Files.exists(BOOKINGS)) {
            Files.write(BOOKINGS, (BOOKING_HEADER + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8));
        }
        List<Booking> result = new ArrayList<>();
        try (BufferedReader in = Files.newBufferedReader(BOOKINGS, StandardCharsets.UTF_8)) {
            if (!BOOKING_HEADER.equals(in.readLine())) {
                throw new IOException("Incorrect bookings.csv column headings.");
            }
            String row;
            int line = 1;
            while ((row = in.readLine()) != null) {
                line++;
                if (row.trim().isEmpty()) continue;
                String[] p = row.split(",", -1);
                if (p.length != 8) throw new IOException("Incorrect booking CSV row " + line);
                try {
                    List<String> seats = Arrays.asList(p[6].split("\\|", -1));
                    if (seats.size() < 1 || seats.size() > 4
                            || !(p[7].equals("ACTIVE") || p[7].equals("CANCELLED"))) {
                        throw new IllegalArgumentException();
                    }
                    Booking b = new Booking(p[0], p[1], LocalDate.parse(p[2]),
                            p[3], p[4], p[5], seats, p[7]);
                    for (Booking old : result) {
                        if (old.getBookingId().equalsIgnoreCase(b.getBookingId())) {
                            throw new IllegalArgumentException("Duplicate booking ID");
                        }
                    }
                    result.add(b);
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid booking details in CSV row " + line, ex);
                }
            }
        }
        return result;
    }

    public void saveBookings(List<Booking> bookings) throws IOException {
        Files.createDirectories(DATA);
        Path temp = Files.createTempFile(DATA, "bookings-", ".tmp");
        try {
            try (BufferedWriter out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                out.write(BOOKING_HEADER);
                out.newLine();
                for (Booking b : bookings) {
                    out.write(String.join(",", b.getBookingId(), b.getTrainId(),
                            b.getTravelDate().toString(), b.getCustomerId(),
                            b.getCustomerName(), b.getContact(),
                            String.join("|", b.getSeatIds()), b.getStatus()));
                    out.newLine();
                }
            }
            try {
                Files.move(temp, BOOKINGS, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, BOOKINGS, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}

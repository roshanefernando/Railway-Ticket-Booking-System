import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookingManager {
    private final FileManager files;
    private final List<Train> trains;
    private final List<Booking> bookings;

    public BookingManager(FileManager files) throws IOException {
        this.files = files;
        trains = files.loadTrains();
        bookings = files.loadBookings();
        validateLoadedBookings();
    }

    public List<Train> getTrains() { return new ArrayList<>(trains); }

    public Train findTrain(String id) {
        for (Train t : trains) if (t.getId().equalsIgnoreCase(id)) return t;
        return null;
    }

    public Booking findBooking(String id) {
        for (Booking b : bookings) if (b.getBookingId().equalsIgnoreCase(id)) return b;
        return null;
    }

    public List<Booking> getBookingsForTrain(String trainId, LocalDate dateOrNull) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getTrainId().equalsIgnoreCase(trainId)
                    && (dateOrNull == null || b.getTravelDate().equals(dateOrNull))) {
                result.add(b);
            }
        }
        result.sort(Comparator.comparing(Booking::getTravelDate)
                .thenComparing(Booking::getBookingId));
        return result;
    }

    public Set<String> getBookedSeats(Train train, LocalDate date, String excludedBookingId) {
        Set<String> result = new HashSet<>();
        for (Booking b : bookings) {
            if (b.isActive() && b.getTrainId().equalsIgnoreCase(train.getId())
                    && b.getTravelDate().equals(date)
                    && !b.getBookingId().equalsIgnoreCase(excludedBookingId)) {
                result.addAll(b.getSeatIds());
            }
        }
        return result;
    }

    public int getAvailableCount(Train train, LocalDate date) {
        return train.getCapacity() - getBookedSeats(train, date, "").size();
    }

    public void validateJourney(Train train, LocalDate date) {
        if (train == null) throw new IllegalArgumentException("Choose a valid train.");
        if (date == null || date.isBefore(LocalDate.now())
                || date.isAfter(LocalDate.now().plusDays(30))) {
            throw new IllegalArgumentException("Travel date must be within the next 30 days.");
        }
        if (LocalDateTime.of(date, train.getDeparture())
                .isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Book at least 2 hours before train departure.");
        }
    }

    private void validateCustomer(String id, String name, String contact) {
        if (id == null || !id.trim().matches("[A-Za-z0-9/-]{4,30}")) {
            throw new IllegalArgumentException("Enter a valid NIC/passport (4-30 letters or digits).");
        }
        if (name == null || name.trim().isEmpty() || name.length() > 70
                || name.matches("(?s).*[,|\"\\r\\n].*")) {
            throw new IllegalArgumentException("Enter a customer name without commas, quotes or |.");
        }
        if (contact == null || !contact.trim().matches("\\+?[0-9]{9,15}")) {
            throw new IllegalArgumentException("Contact must be 9-15 digits (optional +).");
        }
    }

    private void validateSeats(Train train, LocalDate date, String customerId,
                               List<String> seatIds, String excludedBookingId) {
        if (seatIds == null || seatIds.size() < 1 || seatIds.size() > 4) {
            throw new IllegalArgumentException("Select between 1 and 4 seats.");
        }
        Set<String> unique = new HashSet<>(seatIds);
        if (unique.size() != seatIds.size()) {
            throw new IllegalArgumentException("The same seat cannot be selected twice.");
        }
        Set<String> occupied = getBookedSeats(train, date, excludedBookingId);
        for (String seat : seatIds) {
            if (!train.hasSeat(seat)) {
                throw new IllegalArgumentException("Invalid seat for this train: " + seat);
            }
            if (occupied.contains(seat)) {
                throw new IllegalArgumentException("Seat " + seat + " has already been booked.");
            }
        }
        int previous = 0;
        for (Booking b : bookings) {
            if (b.isActive() && b.getTrainId().equalsIgnoreCase(train.getId())
                    && b.getTravelDate().equals(date)
                    && b.getCustomerId().equalsIgnoreCase(customerId)
                    && !b.getBookingId().equalsIgnoreCase(excludedBookingId)) {
                previous += b.getSeatCount();
            }
        }
        if (previous + seatIds.size() > 4) {
            throw new IllegalArgumentException("Customer already has " + previous
                    + " seats on this train/date. Maximum total is 4.");
        }
    }

    private String nextBookingId() {
        int largest = 0;
        for (Booking b : bookings) {
            if (b.getBookingId().matches("B[0-9]+")) {
                try {
                    largest = Math.max(largest,
                            Integer.parseInt(b.getBookingId().substring(1)));
                } catch (NumberFormatException ignored) { }
            }
        }
        return String.format("B%04d", largest + 1);
    }

    public Booking addBooking(Train train, LocalDate date, String id,
                              String name, String contact, List<String> seats) throws IOException {
        validateJourney(train, date);
        validateCustomer(id, name, contact);
        validateSeats(train, date, id.trim(), seats, "");
        Booking b = new Booking(nextBookingId(), train.getId(), date, id.trim(),
                name.trim(), contact.trim(), seats, "ACTIVE");
        bookings.add(b);
        try { files.saveBookings(bookings); }
        catch (IOException ex) { bookings.remove(b); throw ex; }
        return b;
    }

    public Booking updateBooking(Booking original, Train train, LocalDate date,
                                 String id, String name, String contact,
                                 List<String> seats) throws IOException {
        Booking current = original == null ? null : findBooking(original.getBookingId());
        if (current == null || !current.isActive()) {
            throw new IllegalArgumentException("Only active bookings can be changed.");
        }
        validateJourney(train, date);
        validateCustomer(id, name, contact);
        validateSeats(train, date, id.trim(), seats, current.getBookingId());
        Booking revised = current.changed(train.getId(), date, id.trim(), name.trim(),
                contact.trim(), seats);
        replace(current, revised);
        return revised;
    }

    public Booking cancelBooking(Booking original) throws IOException {
        Booking current = original == null ? null : findBooking(original.getBookingId());
        if (current == null || !current.isActive()) {
            throw new IllegalArgumentException("Booking was not found or is already cancelled.");
        }
        Booking cancelled = current.cancelled();
        replace(current, cancelled);
        return cancelled;
    }

    private void replace(Booking original, Booking replacement) throws IOException {
        int i = bookings.indexOf(original);
        if (i < 0) throw new IllegalArgumentException("Booking not found.");
        bookings.set(i, replacement);
        try { files.saveBookings(bookings); }
        catch (IOException ex) { bookings.set(i, original); throw ex; }
    }

    private void validateLoadedBookings() throws IOException {
        Set<String> used = new HashSet<>();
        for (Booking b : bookings) {
            Train train = findTrain(b.getTrainId());
            if (train == null) throw new IOException("Unknown train in bookings.csv: " + b.getTrainId());
            if (new HashSet<>(b.getSeatIds()).size() != b.getSeatCount()) {
                throw new IOException("Repeated seats in booking " + b.getBookingId());
            }
            for (String s : b.getSeatIds()) {
                if (!train.hasSeat(s)) throw new IOException("Invalid seat " + s + " in " + b.getBookingId());
                if (b.isActive() && !used.add(train.getId() + "/" + b.getTravelDate() + "/" + s)) {
                    throw new IOException("Two active bookings use the same seat: " + s);
                }
            }
        }
    }
}

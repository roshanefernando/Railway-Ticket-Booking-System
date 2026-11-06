import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BookingManager {
    private final List<Train> trains;
    private final List<Booking> bookings;
    private final FileManager files;
    public BookingManager(FileManager files) throws IOException {
        this.files = files;
        trains = files.loadTrains();
        bookings = files.loadBookings();
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
    public int availableSeats(Train train, LocalDate date, String excludedId) {
        int used = 0;
        for (Booking b : bookings) {
            if (b.isActive() && b.getTrainId().equalsIgnoreCase(train.getId())
                    && b.getTravelDate().equals(date)
                    && !b.getBookingId().equalsIgnoreCase(excludedId)) {
                used += b.getSeats();
            }
        }
        return train.getCapacity() - used;
    }
    private int customerBookedSeats(String trainId, LocalDate date, String customerId, String excludedId) {
        int seats = 0;
        for (Booking b : bookings) {
            if (b.isActive() && b.getTrainId().equalsIgnoreCase(trainId)&& b.getTravelDate().equals(date)
                    && b.getCustomerId().equalsIgnoreCase(customerId)&& !b.getBookingId().equalsIgnoreCase(excludedId)) {
                seats += b.getSeats();
            }
        }
        return seats;
    }
    public void validateJourney(Train train, LocalDate date) {
        if (train == null) throw new IllegalArgumentException("Please select a valid train ID.");
        if (date == null || date.isBefore(LocalDate.now())|| date.isAfter(LocalDate.now().plusDays(30))) {
            throw new IllegalArgumentException("Travel date must be today or within the next 30 days.");
        }
        if (LocalDateTime.of(date, train.getDeparture()).isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Booking must be at least 2 hours before departure.");
        }
    }
    private void validateSeats(Train train, LocalDate date, String customerId,
                               int requestedSeats, String excludedId) {
        if (requestedSeats < 1 || requestedSeats > 4) {
            throw new IllegalArgumentException("A booking must contain 1 to 4 seats.");
        }
        int available = availableSeats(train, date, excludedId);
        if (requestedSeats > available) {
            throw new IllegalArgumentException("Not enough seats. Only "
                    + Math.max(available, 0) + " seat(s) available.");
        }
        int previous = customerBookedSeats(train.getId(), date, customerId, excludedId);
        if (previous + requestedSeats > 4) {
            throw new IllegalArgumentException("This customer can hold at most 4 seats "
                    + "on the same train and date (already has " + previous + ").");
        }
    }
    private String nextBookingId() {
        int max = 0;
        for (Booking b : bookings) {
            String id = b.getBookingId();
            if (id.matches("B[0-9]+")) {
                try { max = Math.max(max, Integer.parseInt(id.substring(1))); }
                catch (NumberFormatException ignored) { /* invalid older ID: skip */ }
            }
        }
        return String.format("B%04d", max + 1);
    }
    public Booking addBooking(Train train, LocalDate date, String customerId,
                              String customerName, String contact, int seats) throws IOException {
        validateJourney(train, date);
        validateSeats(train, date, customerId, seats, "");
        Booking booking = new Booking(nextBookingId(), train.getId(), date,customerId, customerName, contact, seats, "ACTIVE");
        bookings.add(booking);
        try { files.saveBookings(bookings); }
        catch (IOException ex) { bookings.remove(booking); throw ex; }
        return booking;
    }
    public Booking updateBooking(Booking old, Train newTrain, LocalDate newDate,
                                 String newCustomerId, String newName, String newContact,
                                 int newSeats) throws IOException {
        if (old == null || !old.isActive()) {
            throw new IllegalArgumentException("Only active bookings can be modified.");
        }
        validateJourney(newTrain, newDate);
        validateSeats(newTrain, newDate, newCustomerId, newSeats, old.getBookingId());
        Booking updated = old.withChanges(newTrain.getId(), newDate, newCustomerId,newName, newContact, newSeats);
        replaceBooking(old, updated);
        return updated;
    }
    public Booking cancelBooking(Booking old) throws IOException {
        if (old == null || !old.isActive()) {
            throw new IllegalArgumentException("Booking does not exist or is already cancelled.");
        }
        Booking cancelled = old.cancelled();
        replaceBooking(old, cancelled);
        return cancelled;
    }
    private void replaceBooking(Booking old, Booking updated) throws IOException {
        int index = bookings.indexOf(old);
        if (index < 0) throw new IllegalArgumentException("Booking not found.");
        bookings.set(index, updated);
        try { files.saveBookings(bookings); }
        catch (IOException ex) { bookings.set(index, old); throw ex; }
    }
}
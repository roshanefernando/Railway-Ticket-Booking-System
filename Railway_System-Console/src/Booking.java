import java.time.LocalDate;

public class Booking {
    private final String bookingId;
    private final String trainId;
    private final LocalDate travelDate;
    private final String customerId;
    private final String customerName;
    private final String contact;
    private final int seats;
    private final String status;

    public Booking(String bookingId, String trainId, LocalDate travelDate,
                   String customerId, String customerName, String contact,
                   int seats, String status) {
        this.bookingId = bookingId;
        this.trainId = trainId;
        this.travelDate = travelDate;
        this.customerId = customerId;
        this.customerName = customerName;
        this.contact = contact;
        this.seats = seats;
        this.status = status;
    }
    public String getBookingId() { return bookingId; }
    public String getTrainId() { return trainId; }
    public LocalDate getTravelDate() { return travelDate; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getContact() { return contact; }
    public int getSeats() { return seats; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
    public Booking withChanges(String newTrainId, LocalDate newDate, String newCustomerId,
                               String newName, String newContact, int newSeats) {
        return new Booking(bookingId, newTrainId, newDate, newCustomerId,
                           newName, newContact, newSeats, status);
    }
    public Booking cancelled() {
        return new Booking(bookingId, trainId, travelDate, customerId,customerName, contact, seats, "CANCELLED");
    }
}

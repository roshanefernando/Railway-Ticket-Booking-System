import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Booking {
    private final String bookingId, trainId, customerId, customerName, contact, status;
    private final LocalDate travelDate;
    private final List<String> seatIds;

    public Booking(String bookingId, String trainId, LocalDate travelDate,
                   String customerId, String customerName, String contact,
                   List<String> seatIds, String status) {
        this.bookingId = bookingId;
        this.trainId = trainId;
        this.travelDate = travelDate;
        this.customerId = customerId;
        this.customerName = customerName;
        this.contact = contact;
        this.seatIds = new ArrayList<>(seatIds);
        this.status = status;
    }

    public String getBookingId() { return bookingId; }
    public String getTrainId() { return trainId; }
    public LocalDate getTravelDate() { return travelDate; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getContact() { return contact; }
    public String getStatus() { return status; }
    public List<String> getSeatIds() { return Collections.unmodifiableList(seatIds); }
    public int getSeatCount() { return seatIds.size(); }
    public String getSeatDisplay() { return String.join(", ", seatIds); }
    public boolean isActive() { return "ACTIVE".equals(status); }

    public Booking changed(String newTrainId, LocalDate newDate, String newCustomerId,
                           String newName, String newContact, List<String> newSeatIds) {
        return new Booking(bookingId, newTrainId, newDate, newCustomerId,
                newName, newContact, newSeatIds, status);
    }

    public Booking cancelled() {
        return new Booking(bookingId, trainId, travelDate, customerId, customerName,
                contact, seatIds, "CANCELLED");
    }
}

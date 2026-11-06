import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner INPUT = new Scanner(System.in);
    private static final String[] ROUTES = {
        "Colombo - Kankesanthurai", "Colombo - Galle", "Colombo - Kandy"
    };
    private static BookingManager manager;
    public static void main(String[] args) {
        try {
            manager = new BookingManager(new FileManager());
        } catch (IOException ex) {
            System.out.println("Cannot start application: " + ex.getMessage());
            return;
        }
        while (true) {
            System.out.println("\n========================================");
            System.out.println("  SRI LANKA RAILWAY BOOKING SYSTEM");
            System.out.println("========================================");
            System.out.println("1. View train timetable");
            System.out.println("2. Check available seats");
            System.out.println("3. Make a new booking");
            System.out.println("4. Find a booking");
            System.out.println("5. Modify a booking");
            System.out.println("6. Cancel a booking");
            System.out.println("7. View bookings by route and train");
            System.out.println("0. Exit");
            int choice = readNumber("Choose an option: ", 0, 7);
            try {
                switch (choice) {
                    case 1: showTimetable(); break;
                    case 2: checkAvailability(); break;
                    case 3: makeBooking(); break;
                    case 4: findBooking(); break;
                    case 5: modifyBooking(); break;
                    case 6: cancelBooking(); break;
                    case 7: viewTrainBookings(); break;
                    case 0:
                        System.out.println("Goodbye!");
                        return;
                    default: break;
                }
            } catch (IllegalArgumentException | IOException ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
        }
    }
    private static String readText(String prompt) {
        System.out.print(prompt);
        return INPUT.nextLine().trim();
    }
    private static int readNumber(String prompt, int min, int max) {
        while (true) {
            String input = readText(prompt);
            try {
                int number = Integer.parseInt(input);
                if (number >= min && number <= max) return number;
            } catch (NumberFormatException ignored) { }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }
    private static LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readText(prompt + " (YYYY-MM-DD): "));
            } catch (DateTimeParseException ex) {
                System.out.println("Invalid date. Example: 2026-11-05");
            }
        }
    }
    private static String readRequired(String prompt) {
        while (true) {
            String value = readText(prompt);
            if (!value.isEmpty() && !value.contains(",") && !value.contains("\"")) {
                return value;
            }
            System.out.println("Enter a value without commas or double quotes.");
        }
    }
    private static String readContact(String prompt) {
        while (true) {
            String contact = readRequired(prompt);
            if (contact.matches("\\+?[0-9]{9,15}")) return contact;
            System.out.println("Use 9-15 digits, optionally starting with + (e.g. 0771234567).");
        }
    }
    private static String optionalText(String prompt, String oldValue) {
        while (true) {
            String value = readText(prompt + " [" + oldValue + "] (Enter to keep): ");
            if (value.isEmpty()) return oldValue;
            if (!value.contains(",") && !value.contains("\"")) return value;
            System.out.println("Please avoid commas and double quotes.");
        }
    }
    private static String optionalContact(String old) {
        while (true) {
            String value = optionalText("Contact", old);
            if (value.matches("\\+?[0-9]{9,15}")) return value;
            System.out.println("Contact must contain 9-15 digits (optional leading +).");
        }
    }
    private static Train selectTrain() {
        showTimetable();
        while (true) {
            String trainId = readText("Enter train ID (or 0 to return): ");
            if (trainId.equals("0")) return null;
            Train train = manager.findTrain(trainId);
            if (train != null) return train;
            System.out.println("Train ID not found. Try again.");
        }
    }
    private static void showTimetable() {
        System.out.println("\n----------- EXPRESS TRAIN TIMETABLE -----------");
        System.out.println("Sample schedules for this assignment; not live railway data.");
        List<Train> trains = manager.getTrains();
        for (String route : ROUTES) {
            System.out.println("\n" + route);
            System.out.printf("%-7s %-20s %-17s %-17s %-6s %-6s %5s%n", "ID", "TRAIN", "FROM", "TO", "DEP", "ARR", "SEATS");
            for (Train train : trains) {
                if (train.getRoute().equals(route)) {
                    System.out.printf("%-7s %-20s %-17s %-17s %-6s %-6s %5d%n",train.getId(), train.getName(), train.getFrom(), train.getTo(),
                            train.getDeparture(), train.getArrival(), train.getCapacity());
                }
            }
        }
        System.out.println("All sample services: Daily | Times: Sri Lanka local time");
    }
    private static void checkAvailability() {
        Train train = selectTrain();
        if (train == null) return;
        LocalDate date = readDate("Travel date");
        manager.validateJourney(train, date);
        int available = manager.availableSeats(train, date, "");
        System.out.println("\nTrain: " + train.getName() + " (" + train.getId() + ")");
        System.out.println("Date: " + date);
        System.out.println("Total seats: " + train.getCapacity());
        System.out.println("Available seats: " + available);
        if (available == 0) System.out.println("FULLY BOOKED - no more bookings allowed.");
    }
    private static void makeBooking() throws IOException {
        Train train = selectTrain();
        if (train == null) return;
        LocalDate date = readDate("Travel date");
        manager.validateJourney(train, date);
        int available = manager.availableSeats(train, date, "");
        System.out.println("Available: " + available + " seat(s)");
        if (available == 0) {
            System.out.println("This train is FULLY BOOKED. Please select another train/date.");
            return;
        }
        int seats = readNumber("Number of seats (1-4): ", 1, 4);
        String id = readRequired("Customer NIC / Passport: ");
        String name = readRequired("Customer name: ");
        String contact = readContact("Contact number: ");
        Booking booking = manager.addBooking(train, date, id, name, contact, seats);
        System.out.println("\nBOOKING SAVED SUCCESSFULLY!");
        showBookingDetails(booking);
    }
    private static Booking askForBooking() {
        String id = readText("Enter booking ID: ");
        Booking booking = manager.findBooking(id);
        if (booking == null) System.out.println("Booking not found.");
        return booking;
    }
    private static void findBooking() {
        Booking booking = askForBooking();
        if (booking != null) showBookingDetails(booking);
    }
    private static void showBookingDetails(Booking booking) {
        Train train = manager.findTrain(booking.getTrainId());
        System.out.println("\n------------- BOOKING DETAILS -------------");
        System.out.println("Booking ID  : " + booking.getBookingId());
        System.out.println("Status      : " + booking.getStatus());
        System.out.println("Train       : " + booking.getTrainId()
                + (train == null ? "" : " - " + train.getName()));
        if (train != null) System.out.println("Route       : " + train.getFrom() + " to " + train.getTo());
        System.out.println("Travel date : " + booking.getTravelDate());
        System.out.println("Customer ID : " + booking.getCustomerId());
        System.out.println("Name        : " + booking.getCustomerName());
        System.out.println("Contact     : " + booking.getContact());
        System.out.println("Seats       : " + booking.getSeats());
        System.out.println("-------------------------------------------");
    }
    private static void modifyBooking() throws IOException {
        Booking old = askForBooking();
        if (old == null) return;
        if (!old.isActive()) {
            System.out.println("Cancelled bookings cannot be modified.");
            return;
        }
        showBookingDetails(old);
        System.out.println("Enter new details (press Enter to keep existing values).");
        Train train = manager.findTrain(old.getTrainId());
        while (true) {
            String newId = readText("Train ID [" + train.getId() + "]: ");
            if (newId.isEmpty()) break;
            Train selected = manager.findTrain(newId);
            if (selected != null) { train = selected; break; }
            System.out.println("Unknown train ID. See menu option 1 for train IDs.");
        }
        LocalDate date = old.getTravelDate();
        while (true) {
            String newDate = readText("Travel date [" + date + "] (YYYY-MM-DD): ");
            if (newDate.isEmpty()) break;
            try { date = LocalDate.parse(newDate); break; }
            catch (DateTimeParseException ex) { System.out.println("Invalid date format."); }
        }
        int seats = old.getSeats();
        while (true) {
            String input = readText("Seats [" + seats + "] (1-4): ");
            if (input.isEmpty()) break;
            try {
                int value = Integer.parseInt(input);
                if (value >= 1 && value <= 4) { seats = value; break; }
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a number from 1 to 4.");
        }
        String customerId = optionalText("Customer NIC / Passport", old.getCustomerId());
        String name = optionalText("Customer name", old.getCustomerName());
        String contact = optionalContact(old.getContact());
        Booking updated = manager.updateBooking(old, train, date, customerId, name, contact, seats);
        System.out.println("BOOKING UPDATED SUCCESSFULLY!");
        showBookingDetails(updated);
    }
    private static void cancelBooking() throws IOException {
        Booking booking = askForBooking();
        if (booking == null) return;
        showBookingDetails(booking);
        if (!booking.isActive()) {
            System.out.println("This booking is already cancelled.");
            return;
        }
        String answer = readText("Confirm cancellation (Y/N): ");
        if (answer.equalsIgnoreCase("Y")) {
            manager.cancelBooking(booking);
            System.out.println("Booking cancelled. Reserved seats have been released.");
        } else {
            System.out.println("Cancellation cancelled.");
        }
    }
    private static void viewTrainBookings() {
        System.out.println("\nSelect a route:");
        for (int i = 0; i < ROUTES.length; i++) {
            System.out.println((i + 1) + ". " + ROUTES[i]);
        }
        int routeIndex = readNumber("Route (1-3): ", 1, 3) - 1;
        List<Train> candidates = new ArrayList<>();
        System.out.println("\nTrains for " + ROUTES[routeIndex] + ":");
        for (Train t : manager.getTrains()) {
            if (t.getRoute().equals(ROUTES[routeIndex])) {
                candidates.add(t);
                System.out.println(t.getId() + " | " + t.getName() + " | "+ t.getFrom() + " -> " + t.getTo());
            }
        }
        Train selected = null;
        while (selected == null) {
            String id = readText("Enter train ID (or 0 to return): ");
            if (id.equals("0")) return;
            for (Train t : candidates) if (t.getId().equalsIgnoreCase(id)) selected = t;
            if (selected == null) System.out.println("Please select a train shown above.");
        }
        LocalDate date = null;
        while (true) {
            String dateText = readText("Travel date (YYYY-MM-DD, Enter for all dates): ");
            if (dateText.isEmpty()) break;
            try { date = LocalDate.parse(dateText); break; }
            catch (DateTimeParseException ex) { System.out.println("Invalid date."); }
        }
        List<Booking> results = manager.getBookingsForTrain(selected.getId(), date);
        System.out.println("\nBookings: " + selected.getId() + " - " + selected.getName()
                + (date == null ? " (all dates)" : " on " + date));
        if (results.isEmpty()) {
            System.out.println("No booking records found.");
            return;
        }
        System.out.printf("%-7s %-10s %-9s %-16s %-20s %-14s %-5s %-9s%n",
                "BOOK ID", "DATE", "NIC/PASS", "CUSTOMER", "CONTACT", "TRAIN", "SEATS", "STATUS");
        int activeBookings = 0;
        for (Booking b : results) {
            System.out.printf("%-7s %-10s %-9s %-16s %-20s %-14s %-5d %-9s%n", b.getBookingId(), b.getTravelDate(), b.getCustomerId(),
                    b.getCustomerName(), b.getContact(), b.getTrainId(), b.getSeats(), b.getStatus());
            if (b.isActive()) activeBookings++;
        }
        System.out.println("Records: " + results.size() + " | Active bookings: " + activeBookings);
        if (date != null) {
            System.out.println("Available seats on " + date + ": "+ manager.availableSeats(selected, date, ""));
        }
    }
}
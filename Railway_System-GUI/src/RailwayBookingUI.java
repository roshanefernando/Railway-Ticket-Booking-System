import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.EmptyBorder;

public class RailwayBookingUI extends JPanel {
    private static final Color NIGHT = DeskStyle.NIGHT;
    private static final Color INK = new Color(0x192941);
    private static final Color MUTED = new Color(0x526078);
    private static final Color GOLD = DeskStyle.AMBER;
    private static final Color LAVENDER = DeskStyle.LAVENDER;
    private static final Color CORAL = new Color(0xFFD6CB);
    private static final Color BG = DeskStyle.PAPER;
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = DeskStyle.EDGE;
    private static final Color FREE = new Color(0xD5EBE4);
    private static final Color TAKEN = new Color(0xFFD6CB);
    private static final Color SELECTED = GOLD;
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    private final BookingManager manager;
    private final CardLayout pages = new CardLayout();
    private final DeskStyle.Pages body = new DeskStyle.Pages(pages);
    private int visiblePage = -1;
    private final String[] sections = {"Timetable", "Seat booking", "Find booking", "Records"};
    private final JButton[] nav = new JButton[sections.length];
    private final LinkedHashSet<String> selectedSeats = new LinkedHashSet<>();
    private JComboBox<Train> trainList;
    private JTextField travelDate, customerId, customerName, customerContact;
    private JPanel seatArea;
    private JLabel seatStatus, formStatus;
    private JButton saveButton;
    private String editingId, mapTrainId;
    private LocalDate mapDate;
    private boolean updatingForm;
    private JTextField searchId;
    private JPanel bookingDetails;
    private JButton editButton, cancelButton;
    private Booking searchedBooking;
    private JComboBox<String> routeList;
    private JComboBox<Train> recordTrainList;
    private JTextField recordDate;
    private DefaultTableModel recordModel;
    private JLabel recordSummary;

    public RailwayBookingUI(BookingManager manager) {
        super(new BorderLayout());
        this.manager = manager;
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(10, 24, 10, 24)));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(DeskStyle.Symbol.BRAND));
        brand.add(label("Railway Desk", 22, Font.BOLD, NIGHT));
        header.add(brand, BorderLayout.WEST);
        JPanel links = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        links.setOpaque(false);
        for (int i = 0; i < sections.length; i++) {
            final int index = i;
            nav[i] = button(sections[i], LAVENDER);
            nav[i].setIcon(DeskStyle.Symbol.values()[i + 1]);
            nav[i].setIconTextGap(7);
            nav[i].addActionListener(e -> openPage(index));
            links.add(nav[i]);
        }
        header.add(links, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        body.setBorder(BorderFactory.createEmptyBorder(16, 24, 18, 24));
        body.setBackground(BG);
        body.add(timetableTab(), sections[0]);
        body.add(bookingTab(), sections[1]);
        body.add(searchTab(), sections[2]);
        body.add(recordsTab(), sections[3]);
        add(body, BorderLayout.CENTER);
        try { loadSeatMap(false); }
        catch (IllegalArgumentException ex) { clearSeatMap(ex.getMessage()); }
        openPage(0);
    }

    private void openPage(int index) {
        pages.show(body, sections[index]);
        if (index != visiblePage) body.reveal();
        visiblePage = index;
        for (int i = 0; i < nav.length; i++) {
            nav[i].setBackground(i == index ? LAVENDER : WHITE);
        }
        if (index == 3) showTrainRecords();
        if (index == 1 && mapTrainId != null) {
            try { loadSeatMap(true); }
            catch (IllegalArgumentException ex) { clearSeatMap(ex.getMessage()); }
        }
    }

    private JPanel timetableTab() {
        JPanel page = page();
        List<Train> trains = manager.getTrains();
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(heading("Train timetable", "Select a service to see its journey and book seats."), BorderLayout.NORTH);

        JPanel ticket = new JourneyBanner(new BorderLayout(0, 10));
        ticket.setBorder(new EmptyBorder(16, 23, 16, 23));
        JLabel service = label("", 12, Font.BOLD, GOLD);
        JLabel journey = label("", 26, Font.BOLD, WHITE);
        JLabel times = label("", 13, Font.PLAIN, new Color(0xE7EAF3));
        JPanel trip = new JPanel();
        trip.setLayout(new BoxLayout(trip, BoxLayout.Y_AXIS));
        trip.setOpaque(false);
        trip.add(service);
        trip.add(Box.createVerticalStrut(4));
        trip.add(journey);
        trip.add(Box.createVerticalStrut(3));
        trip.add(times);
        ticket.add(trip, BorderLayout.NORTH);
        JPanel ticketActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ticketActions.setOpaque(false);
        JButton book = button("Choose seats  →", GOLD);
        JLabel capacity = label("", 12, Font.PLAIN, WHITE);
        capacity.setBorder(new EmptyBorder(0, 14, 0, 0));
        ticketActions.add(book);
        ticketActions.add(capacity);
        ticket.add(ticketActions, BorderLayout.SOUTH);
        top.add(ticket, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        DefaultTableModel model = model(new String[]{"Service", "Train name", "From", "To", "Departs", "Arrives", "Seats"});
        for (Train t : trains) model.addRow(new Object[]{t.getId(), t.getName(), t.getFrom(), t.getTo(),
                t.getDeparture(), t.getArrival(), t.getCapacity()});
        JTable table = table(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        widths(table, 90, 175, 165, 165, 85, 85, 65);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Train t = trains.get(row);
            service.setText(t.getId() + "    ·    " + t.getName() + "    ·    " + t.getServiceDays());
            journey.setText(t.getFrom() + "  →  " + t.getTo());
            times.setText("Departs  " + t.getDeparture() + "      •      Arrives  " + t.getArrival());
            capacity.setText(t.getCarriages() + " carriages   ·   " + t.getSeatsPerCarriage() + " seats per carriage");
        });
        book.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Train next = trains.get(row);
            if (editingId != null) resetForm();
            trainList.setSelectedItem(next);
            openPage(1);
            try { loadSeatMap(false); }
            catch (IllegalArgumentException ex) { clearSeatMap(ex.getMessage()); }
        });
        table.setRowSelectionInterval(0, 0);
        JPanel board = card("Scheduled services", trains.size() + " services in the sample timetable");
        board.add(scroller(table), BorderLayout.CENTER);
        page.add(board, BorderLayout.CENTER);
        return page;
    }

    private JPanel bookingTab() {
        JPanel page = page();
        JPanel top = card("Seat booking", "Travel date format: YYYY-MM-DD");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        bar.setOpaque(false);
        trainList = new JComboBox<>(manager.getTrains().toArray(new Train[0]));
        trainList.setPreferredSize(new Dimension(365, 35));
        trainList.setFont(FONT);
        trainList.setForeground(Color.BLACK);
        trainList.setBackground(WHITE);
        trainList.setRenderer(trainRenderer());
        DeskStyle.combo(trainList);
        trainList.addActionListener(e -> {
            if (!updatingForm) clearSeatMap("Train changed. Click Load Seat Map.");
        });
        travelDate = field(LocalDate.now().plusDays(1).toString(), 10);
        travelDate.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                if (!updatingForm && mapTrainId != null) clearSeatMap("Date changed. Click Load Seat Map.");
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
        });
        JButton load = button("Load Seat Map", GOLD);
        load.addActionListener(e -> {
            try {
                Train t = (Train) trainList.getSelectedItem();
                LocalDate d = date(travelDate.getText());
                loadSeatMap(t != null && t.getId().equals(mapTrainId) && d.equals(mapDate));
            } catch (IllegalArgumentException ex) { error(ex.getMessage()); }
        });
        bar.add(label("Train", 12, Font.BOLD, INK));
        bar.add(trainList);
        bar.add(label("Date", 12, Font.BOLD, INK));
        bar.add(travelDate);
        bar.add(load);
        top.add(bar, BorderLayout.SOUTH);
        page.add(top, BorderLayout.NORTH);

        JPanel middle = card("Carriage seating", "Choose up to four seats across the carriages.");
        seatArea = new JPanel(new GridLayout(0, 2, 12, 12));
        seatArea.setBackground(BG);
        seatArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(seatArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        middle.add(scroll, BorderLayout.CENTER);
        JPanel legend = new JPanel(new GridLayout(2, 1, 0, 2));
        legend.setOpaque(false);
        JPanel keys = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        keys.setOpaque(false);
        keys.add(key("Available", FREE));
        keys.add(key("×  Booked", TAKEN));
        keys.add(key("✓  Selected", SELECTED));
        legend.add(keys);
        seatStatus = label("", 12, Font.BOLD, INK);
        legend.add(seatStatus);
        middle.add(legend, BorderLayout.SOUTH);

        JPanel bottom = DeskStyle.card(new BorderLayout(0, 12), 13);
        JLabel customerHeading = label("Passenger details", 18, Font.BOLD, NIGHT);
        customerHeading.setBorder(new EmptyBorder(2, 0, 6, 0));
        bottom.add(customerHeading, BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridLayout(3, 1, 0, 5));
        fields.setOpaque(false);
        customerId = field("", 16);
        customerName = field("", 19);
        customerContact = field("", 16);
        for (JTextField input : new JTextField[]{customerId, customerName, customerContact}) {
            input.setColumns(0);
            input.setPreferredSize(new Dimension(140, 29));
        }
        fields.add(fieldGroup("NIC / Passport", customerId));
        fields.add(fieldGroup("Customer name", customerName));
        fields.add(fieldGroup("Contact number", customerContact));
        JPanel fieldHolder = new JPanel(new BorderLayout());
        fieldHolder.setOpaque(false);
        fieldHolder.add(fields, BorderLayout.NORTH);
        JScrollPane formScroll = new JScrollPane(fieldHolder, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(WHITE);
        formScroll.getVerticalScrollBar().setUnitIncrement(15);
        bottom.add(formScroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(0, 4));
        actions.setOpaque(false);
        formStatus = label("New booking", 12, Font.BOLD, MUTED);
        JButton reset = button("New booking", WHITE);
        reset.addActionListener(e -> resetForm());
        saveButton = button("Confirm booking", GOLD);
        saveButton.addActionListener(e -> saveBooking());
        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 6));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(reset);
        actions.add(formStatus, BorderLayout.NORTH);
        actions.add(buttons, BorderLayout.SOUTH);
        bottom.add(actions, BorderLayout.SOUTH);
        bottom.setPreferredSize(new Dimension(242, 0));
        JPanel workspace = new JPanel(new BorderLayout(12, 0));
        workspace.setOpaque(false);
        workspace.add(middle, BorderLayout.CENTER);
        workspace.add(bottom, BorderLayout.EAST);
        page.add(workspace, BorderLayout.CENTER);
        return page;
    }

    private JPanel searchTab() {
        JPanel page = page();
        JPanel top = card("Find a booking", "Enter a booking reference to view, edit or cancel a reservation.");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        bar.setOpaque(false);
        bar.add(label("Booking ID", 12, Font.BOLD, INK));
        searchId = field("", 16);
        bar.add(searchId);
        JButton find = button("Find Booking", GOLD);
        find.addActionListener(e -> findBooking());
        searchId.addActionListener(e -> findBooking());
        bar.add(find);
        top.add(bar, BorderLayout.SOUTH);
        page.add(top, BorderLayout.NORTH);
        JPanel info = card("Reservation details", "Passenger, journey and reserved seats");
        bookingDetails = new JPanel(new BorderLayout());
        bookingDetails.setBackground(WHITE);
        info.add(scroller(bookingDetails), BorderLayout.CENTER);
        page.add(info, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 5));
        actions.setOpaque(false);
        editButton = button("Edit booking", GOLD);
        cancelButton = button("Cancel booking", CORAL);
        editButton.addActionListener(e -> editFoundBooking());
        cancelButton.addActionListener(e -> cancelFoundBooking());
        actions.add(editButton);
        actions.add(cancelButton);
        displayBooking(null, "Find a booking using its reference, for example B0001.");
        page.add(actions, BorderLayout.SOUTH);
        return page;
    }

    private JPanel recordsTab() {
        JPanel page = page();
        JPanel top = card("Booking records", "Choose a route and service. Leave the date blank to see all journeys.");
        JPanel filters = new JPanel(new GridLayout(2, 1, 0, 6));
        filters.setOpaque(false);
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 1));
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 1));
        firstRow.setOpaque(false);
        secondRow.setOpaque(false);
        routeList = new JComboBox<>(manager.getTrains().stream().map(Train::getRoute).distinct().toArray(String[]::new));
        routeList.setPreferredSize(new Dimension(200, 35));
        recordTrainList = new JComboBox<>();
        recordTrainList.setPreferredSize(new Dimension(300, 35));
        routeList.setFont(FONT);
        routeList.setForeground(Color.BLACK);
        routeList.setBackground(WHITE);
        recordTrainList.setFont(FONT);
        recordTrainList.setForeground(Color.BLACK);
        recordTrainList.setBackground(WHITE);
        recordTrainList.setRenderer(trainRenderer());
        DeskStyle.combo(routeList);
        DeskStyle.combo(recordTrainList);
        recordDate = field("", 10);
        routeList.addActionListener(e -> updateRecordTrains());
        firstRow.add(label("Route", 12, Font.BOLD, INK));
        firstRow.add(routeList);
        firstRow.add(label("Train", 12, Font.BOLD, INK));
        firstRow.add(recordTrainList);
        secondRow.add(label("Date (optional)", 12, Font.BOLD, INK));
        secondRow.add(recordDate);
        JButton show = button("Show Records", LAVENDER);
        show.addActionListener(e -> showTrainRecords());
        secondRow.add(show);
        filters.add(firstRow);
        filters.add(secondRow);
        top.add(filters, BorderLayout.SOUTH);
        page.add(top, BorderLayout.NORTH);
        String[] cols = {"Booking ID", "Date", "Customer ID", "Customer Name", "Contact",
                "Train ID", "Seats", "Seat Numbers", "Status"};
        recordModel = model(cols);
        JTable grid = table(recordModel);
        widths(grid, 98, 105, 130, 170, 125, 85, 65, 230, 95);
        page.add(scroller(grid), BorderLayout.CENTER);
        recordSummary = label("", 13, Font.BOLD, INK);
        recordSummary.setBorder(BorderFactory.createEmptyBorder(7, 9, 9, 9));
        page.add(recordSummary, BorderLayout.SOUTH);
        updateRecordTrains();
        return page;
    }

    private void updateRecordTrains() {
        if (recordTrainList == null) return;
        recordTrainList.removeAllItems();
        for (Train t : manager.getTrains())
            if (t.getRoute().equals(routeList.getSelectedItem())) recordTrainList.addItem(t);
    }

    private void showTrainRecords() {
        if (recordModel == null) return;
        Train train = (Train) recordTrainList.getSelectedItem();
        if (train == null) return;
        try {
            LocalDate d = recordDate.getText().trim().isEmpty() ? null : date(recordDate.getText());
            List<Booking> records = manager.getBookingsForTrain(train.getId(), d);
            recordModel.setRowCount(0);
            int active = 0;
            for (Booking b : records) {
                if (b.isActive()) active++;
                recordModel.addRow(new Object[]{b.getBookingId(), b.getTravelDate(), b.getCustomerId(),
                        b.getCustomerName(), b.getContact(), b.getTrainId(), b.getSeatCount(),
                        b.getSeatDisplay(), b.getStatus()});
            }
            String summary = "Records: " + records.size() + "  |  Active: " + active;
            if (d != null) summary += "  |  Available seats: " + manager.getAvailableCount(train, d)
                    + "/" + train.getCapacity();
            recordSummary.setText(records.isEmpty() ? "No reservations for this selection.    " + summary : summary);
        } catch (IllegalArgumentException ex) { error(ex.getMessage()); }
    }

    private void loadSeatMap(boolean keep) {
        Train train = (Train) trainList.getSelectedItem();
        if (train == null) throw new IllegalArgumentException("Select a train first.");
        LocalDate date = date(travelDate.getText());
        manager.validateJourney(train, date);
        if (!keep) selectedSeats.clear();
        String exclude = "";
        if (editingId != null) {
            Booking old = manager.findBooking(editingId);
            if (old != null && old.getTrainId().equalsIgnoreCase(train.getId())
                    && old.getTravelDate().equals(date)) exclude = editingId;
        }
        Set<String> booked = manager.getBookedSeats(train, date, exclude);
        selectedSeats.removeIf(s -> !train.hasSeat(s) || booked.contains(s));
        mapTrainId = train.getId();
        mapDate = date;
        seatArea.removeAll();
        for (int carriage = 1; carriage <= train.getCarriages(); carriage++) {
            JPanel card = DeskStyle.card(new BorderLayout(0, 7), 11);
            JPanel caption = new JPanel(new BorderLayout());
            caption.setOpaque(false);
            JLabel coach = label(String.format("Carriage %02d", carriage), 14, Font.BOLD, NIGHT);
            coach.setIcon(DeskStyle.Symbol.SEAT);
            coach.setIconTextGap(7);
            caption.add(coach, BorderLayout.WEST);
            caption.add(label(train.getSeatsPerCarriage() + " seats", 11, Font.PLAIN, MUTED), BorderLayout.EAST);
            card.add(caption, BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridBagLayout());
            grid.setOpaque(false);
            for (int seat = 1; seat <= train.getSeatsPerCarriage(); seat++) {
                String id = train.seatId(carriage, seat);
                String number = String.format("%02d", seat);
                JButton b = new DeskStyle.Action(number, selectedSeats.contains(id) ? SELECTED : FREE, true);
                b.setToolTipText(id + " - Available");
                b.setPreferredSize(new Dimension(47, 34));
                if (selectedSeats.contains(id)) {
                    b.setText("✓" + number);
                    b.setToolTipText(id + " - Selected");
                }
                if (booked.contains(id)) {
                    b.setText("×" + number);
                    b.setBackground(TAKEN);
                    b.setToolTipText(id + " - Booked");
                    b.setForeground(Color.BLACK);
                    b.addActionListener(e -> error("Seat " + id + " is already booked."));
                } else {
                    b.addActionListener(e -> {
                        if (selectedSeats.remove(id)) b.setBackground(FREE);
                        else if (selectedSeats.size() >= 4) error("Maximum four seats per booking.");
                        else { selectedSeats.add(id); b.setBackground(SELECTED); }
                        boolean chosen = selectedSeats.contains(id);
                        b.setText((chosen ? "✓" : "") + number);
                        b.setToolTipText(id + (chosen ? " - Selected" : " - Available"));
                        updateSeatStatus(train, date, booked.size());
                    });
                }
                GridBagConstraints cell = new GridBagConstraints();
                cell.gridx = (seat - 1) % 4 + ((seat - 1) % 4 >= 2 ? 1 : 0);
                cell.gridy = (seat - 1) / 4;
                cell.insets = new Insets(3, 2, 3, 2);
                grid.add(b, cell);
            }
            GridBagConstraints aisle = new GridBagConstraints();
            aisle.gridx = 2;
            aisle.gridy = 0;
            aisle.gridheight = (train.getSeatsPerCarriage() + 3) / 4;
            grid.add(Box.createHorizontalStrut(10), aisle);
            JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            holder.setOpaque(false);
            holder.add(grid);
            card.add(holder, BorderLayout.CENTER);
            seatArea.add(card);
        }
        seatArea.revalidate();
        seatArea.repaint();
        updateSeatStatus(train, date, booked.size());
    }

    private void updateSeatStatus(Train t, LocalDate date, int occupied) {
        seatStatus.setText("   " + t.getId() + "  |  " + date + "  |  Free: "
                + (t.getCapacity() - occupied) + "/" + t.getCapacity()
                + "  |  Chosen: " + selectedSeats.size());
    }

    private void clearSeatMap(String message) {
        mapTrainId = null;
        mapDate = null;
        selectedSeats.clear();
        if (seatArea != null) {
            seatArea.removeAll();
            seatArea.add(label(message, 13, Font.PLAIN, MUTED));
            seatArea.revalidate();
            seatArea.repaint();
        }
        if (seatStatus != null) seatStatus.setText("");
    }

    private void saveBooking() {
        try {
            Train train = (Train) trainList.getSelectedItem();
            LocalDate d = date(travelDate.getText());
            if (train == null || !train.getId().equals(mapTrainId) || !d.equals(mapDate))
                throw new IllegalArgumentException("Load the seat map for the selected train and date first.");
            List<String> seats = new ArrayList<>(selectedSeats);
            Collections.sort(seats);
            Booking result = editingId == null
                    ? manager.addBooking(train, d, customerId.getText(), customerName.getText(),
                            customerContact.getText(), seats)
                    : manager.updateBooking(manager.findBooking(editingId), train, d,
                            customerId.getText(), customerName.getText(), customerContact.getText(), seats);
            JOptionPane.showMessageDialog(this, "Booking saved!\nReference: " + result.getBookingId()
                    + "\nSeats: " + result.getSeatDisplay(), "Booking saved", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
            searchId.setText(result.getBookingId());
            findBooking();
            openPage(2);
        } catch (IOException | IllegalArgumentException ex) { error(ex.getMessage()); }
    }

    private void resetForm() {
        editingId = null;
        updatingForm = true;
        travelDate.setText(LocalDate.now().plusDays(1).toString());
        updatingForm = false;
        customerId.setText("");
        customerName.setText("");
        customerContact.setText("");
        formStatus.setText("New booking");
        saveButton.setText("Confirm booking");
        clearSeatMap("Select a train and click Load Seat Map.");
        try { loadSeatMap(false); } catch (IllegalArgumentException ignored) { }
    }

    private void findBooking() {
        searchedBooking = manager.findBooking(searchId.getText().trim());
        displayBooking(searchedBooking, "No booking matches this reference. Check the ID and try again.");
    }

    private void displayBooking(Booking booking, String emptyMessage) {
        bookingDetails.removeAll();
        boolean active = booking != null && booking.isActive();
        editButton.setEnabled(active);
        cancelButton.setEnabled(active);
        if (booking == null) {
            JLabel message = label(emptyMessage, 14, Font.PLAIN, MUTED);
            message.setIcon(DeskStyle.Symbol.SEARCH);
            message.setIconTextGap(12);
            message.setHorizontalTextPosition(SwingConstants.CENTER);
            message.setVerticalTextPosition(SwingConstants.BOTTOM);
            message.setHorizontalAlignment(SwingConstants.CENTER);
            bookingDetails.add(message, BorderLayout.CENTER);
        } else {
            Train train = manager.findTrain(booking.getTrainId());
            JPanel receipt = new JPanel(new GridLayout(0, 2, 18, 12));
            receipt.setBackground(WHITE);
            receipt.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));
            String[][] details = {
                {"Booking reference", booking.getBookingId()}, {"Status", booking.getStatus()},
                {"Service", train.getId() + "  /  " + train.getName()}, {"Travel date", booking.getTravelDate().toString()},
                {"Journey", train.getFrom() + " → " + train.getTo()}, {"Seats (" + booking.getSeatCount() + ")", booking.getSeatDisplay()},
                {"Passenger", booking.getCustomerName()}, {"NIC / Passport", booking.getCustomerId()},
                {"Contact number", booking.getContact()}
            };
            for (String[] pair : details) {
                JPanel cell = new JPanel(new BorderLayout(0, 2));
                cell.setOpaque(false);
                cell.add(label(pair[0], 12, Font.PLAIN, MUTED), BorderLayout.NORTH);
                JLabel value = label(pair[1], 15, Font.BOLD, INK);
                value.setToolTipText(pair[1]);
                if (pair[0].equals("Status")) value.setForeground(booking.isActive() ? new Color(0x126351) : new Color(0x913B42));
                cell.add(value, BorderLayout.CENTER);
                receipt.add(cell);
            }
            bookingDetails.add(receipt, BorderLayout.NORTH);
        }
        bookingDetails.revalidate();
        bookingDetails.repaint();
    }

    private void editFoundBooking() {
        if (searchedBooking == null || !searchedBooking.isActive()) {
            error("Find an active booking first.");
            return;
        }
        Booking b = manager.findBooking(searchedBooking.getBookingId());
        Train train = b == null ? null : manager.findTrain(b.getTrainId());
        if (b == null || !b.isActive() || train == null) {
            error("Booking is no longer available to edit.");
            return;
        }
        updatingForm = true;
        trainList.setSelectedItem(train);
        travelDate.setText(b.getTravelDate().toString());
        updatingForm = false;
        editingId = b.getBookingId();
        customerId.setText(b.getCustomerId());
        customerName.setText(b.getCustomerName());
        customerContact.setText(b.getContact());
        clearSeatMap("Loading seats...");
        selectedSeats.addAll(b.getSeatIds());
        try {
            loadSeatMap(true);
            formStatus.setText("Editing " + editingId);
            saveButton.setText("Save Changes");
            openPage(1);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
            resetForm();
        }
    }

    private void cancelFoundBooking() {
        if (searchedBooking == null || !searchedBooking.isActive()) {
            error("Find an active booking first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Cancel booking " + searchedBooking.getBookingId()
                + "?\nThe reserved seats will become available.", "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            String id = searchedBooking.getBookingId();
            manager.cancelBooking(searchedBooking);
            findBooking();
            if (id.equals(editingId)) resetForm();
            JOptionPane.showMessageDialog(this, "Booking cancelled. Seats are now available.");
        } catch (IOException | IllegalArgumentException ex) { error(ex.getMessage()); }
    }

    private ListCellRenderer<? super Train> trainRenderer() {
        return new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object item,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, item, index, selected, focus);
                setForeground(Color.BLACK);
                setBackground(selected ? LAVENDER : Color.WHITE);
                if (item instanceof Train) {
                    Train train = (Train) item;
                    setText(train.getId() + " - " + train.getName() + " | " + train.getDeparture());
                }
                return this;
            }
        };
    }

    private JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 11));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(11, 2, 2, 2));
        return p;
    }

    private JPanel card(String title, String description) {
        JPanel p = DeskStyle.card(new BorderLayout(0, 10), 15);
        p.add(heading(title, description), BorderLayout.NORTH);
        return p;
    }

    private JPanel heading(String title, String description) {
        JPanel h = new JPanel(new BorderLayout(0, 3));
        h.setOpaque(false);
        h.add(label(title, 23, Font.BOLD, NIGHT), BorderLayout.NORTH);
        h.add(label(description, 13, Font.PLAIN, MUTED), BorderLayout.SOUTH);
        return h;
    }

    private JPanel fieldGroup(String caption, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel captionLabel = label(caption, 12, Font.BOLD, INK);
        captionLabel.setLabelFor(field);
        p.add(captionLabel, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel key(String title, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        JLabel chip = new JLabel("    ");
        chip.setOpaque(true);
        chip.setBackground(color);
        chip.setBorder(BorderFactory.createLineBorder(BORDER));
        p.add(chip);
        p.add(label(title, 12, Font.PLAIN, INK));
        return p;
    }

    private JLabel label(String text, int size, int weight, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", weight, size));
        l.setForeground(color);
        return l;
    }

    private JTextField field(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setFont(FONT);
        f.setForeground(Color.BLACK);
        f.setBackground(Color.WHITE);
        f.setCaretColor(NIGHT);
        f.setSelectionColor(LAVENDER);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 35));
        f.setBorder(DeskStyle.inputBorder());
        return f;
    }

    private JButton button(String text, Color color) {
        return new DeskStyle.Action(text, color, false);
    }

    private DefaultTableModel model(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(FONT);
        t.setForeground(Color.BLACK);
        t.setBackground(Color.WHITE);
        t.setRowHeight(37);
        t.setGridColor(BORDER);
        t.setShowVerticalLines(false);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setSelectionBackground(LAVENDER);
        t.setSelectionForeground(Color.BLACK);
        JTableHeader h = t.getTableHeader();
        h.setFont(new Font("SansSerif", Font.BOLD, 12));
        h.setBackground(new Color(0xEDF0F7));
        h.setForeground(Color.BLACK);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setFont(new Font("SansSerif", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.LEFT);
                setOpaque(true);
                setForeground(Color.BLACK);
                setBackground(new Color(0xEDF0F7));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 5));
                return this;
            }
        });
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 36));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setFont(column == 0 ? new Font("Monospaced", Font.BOLD, 13) : FONT);
                if (!selected) setBackground(row % 2 == 0 ? WHITE : new Color(0xFAFBFE));
                setForeground(Color.BLACK);
                Color strip = selected && column == 0 ? new Color(0x626BA4) : getBackground();
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, strip), new EmptyBorder(0, 5, 0, 5)));
                return this;
            }
        });
        return t;
    }

    private void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private JScrollPane scroller(Component component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(WHITE);
        if (component instanceof JTable) pane.setColumnHeaderView(((JTable) component).getTableHeader());
        return pane;
    }

    private LocalDate date(String input) {
        try { return LocalDate.parse(input.trim()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Use date YYYY-MM-DD, e.g. 2026-11-15."); }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Check input", JOptionPane.WARNING_MESSAGE);
    }
}

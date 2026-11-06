import java.time.LocalTime;

public class Train {
    private final String id, name, route, from, to, serviceDays;
    private final LocalTime departure, arrival;
    private final int carriages, seatsPerCarriage;

    public Train(String id, String name, String route, String from, String to,
                 LocalTime departure, LocalTime arrival, int carriages,
                 int seatsPerCarriage, String serviceDays) {
        if (carriages < 1 || seatsPerCarriage < 1) {
            throw new IllegalArgumentException("Carriage and seat counts must be positive.");
        }
        this.id = id;
        this.name = name;
        this.route = route;
        this.from = from;
        this.to = to;
        this.departure = departure;
        this.arrival = arrival;
        this.carriages = carriages;
        this.seatsPerCarriage = seatsPerCarriage;
        this.serviceDays = serviceDays;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoute() { return route; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public LocalTime getDeparture() { return departure; }
    public LocalTime getArrival() { return arrival; }
    public int getCarriages() { return carriages; }
    public int getSeatsPerCarriage() { return seatsPerCarriage; }
    public String getServiceDays() { return serviceDays; }
    public int getCapacity() { return carriages * seatsPerCarriage; }

    public String seatId(int carriage, int seat) {
        return String.format("C%d-S%02d", carriage, seat);
    }

    public boolean hasSeat(String seatId) {
        for (int c = 1; c <= carriages; c++) {
            for (int s = 1; s <= seatsPerCarriage; s++) {
                if (seatId(c, s).equals(seatId)) return true;
            }
        }
        return false;
    }

    @Override public String toString() {
        return id + " - " + name + " (" + from + " to " + to + ")";
    }
}

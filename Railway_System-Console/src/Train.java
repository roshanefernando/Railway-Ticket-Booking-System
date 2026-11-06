import java.time.LocalTime;

public class Train {
    private final String id;
    private final String name;
    private final String route;
    private final String from;
    private final String to;
    private final LocalTime departure;
    private final LocalTime arrival;
    private final int capacity;
    private final String serviceDays;
    public Train(String id, String name, String route, String from, String to,
                 LocalTime departure, LocalTime arrival, int capacity, String serviceDays) {
        this.id = id;
        this.name = name;
        this.route = route;
        this.from = from;
        this.to = to;
        this.departure = departure;
        this.arrival = arrival;
        this.capacity = capacity;
        this.serviceDays = serviceDays;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoute() { return route; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public LocalTime getDeparture() { return departure; }
    public LocalTime getArrival() { return arrival; }
    public int getCapacity() { return capacity; }
    public String getServiceDays() { return serviceDays; }
}

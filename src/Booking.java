import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {
  int id;
  int busId;
  int routeId;
  int fare;
  String userPhone;
  String pickup;
  String drop;
  LocalDate date;
  List<Integer> seats;
  Status status;
  LocalDateTime created;

  Booking(int id, String userPhone, int busId, int routeId, LocalDate date, List<Integer> seats,
          String pickup, String drop, int fare, Status status, LocalDateTime created) {
    this.id = id;
    this.userPhone = userPhone;
    this.busId = busId;
    this.routeId = routeId;
    this.date = date;
    this.seats = new ArrayList<>(seats);
    this.pickup = pickup;
    this.drop = drop;
    this.fare = fare;
    this.status = status;
    this.created = created;
  }
}

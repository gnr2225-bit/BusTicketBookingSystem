package bts.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {
  public int id;
  public int busId;
  public int routeId;
  public int fare;
  public String userPhone;
  public String pickup;
  public String drop;
  public LocalDate date;
  public List<Integer> seats;
  public Status status;
  public LocalDateTime created;

  public Booking(int id, String userPhone, int busId, int routeId, LocalDate date, List<Integer> seats,
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

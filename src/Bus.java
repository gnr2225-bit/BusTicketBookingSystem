import java.time.LocalTime;

public class Bus {
  int id;
  int routeId;
  int seats;
  String opId;
  String name;
  LocalTime dep;

  Bus(int id, String opId, int routeId, String name, int seats, LocalTime dep) {
    this.id = id;
    this.opId = opId;
    this.routeId = routeId;
    this.name = name;
    this.seats = seats;
    this.dep = dep;
  }
}

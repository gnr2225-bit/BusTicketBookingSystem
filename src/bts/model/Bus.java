package bts.model;

import java.time.LocalTime;

public class Bus {
  public int id;
  public int routeId;
  public int seats;
  public String opId;
  public String name;
  public LocalTime dep;

  public Bus(int id, String opId, int routeId, String name, int seats, LocalTime dep) {
    this.id = id;
    this.opId = opId;
    this.routeId = routeId;
    this.name = name;
    this.seats = seats;
    this.dep = dep;
  }
}

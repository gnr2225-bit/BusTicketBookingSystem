package bts.admin;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import bts.app.AppState;
import bts.model.Bus;
import bts.model.Route;
import bts.model.Status;
import bts.util.Input;
import bts.util.Output;

public class BusAdmin {
  private final AppState state;
  private final Input input;

  public BusAdmin(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void showBuses() {
    Output.head("Buses");
    List<List<String>> rows = new ArrayList<>();
    for (Bus b : state.buses.values()) {
      Route r = state.routes.get(b.routeId);
      String rt = r == null ? "N/A" : r.src + " -> " + r.dst;
      rows.add(List.of("" + b.id, b.opId, b.name, rt, b.dep.format(AppState.TF), "" + b.seats,
        "" + state.available(b.id, LocalDate.now())));
    }
    Output.table(List.of("Bus", "Operator", "Name", "Route", "Dep", "Seats", "Avail(Today)"), rows);
  }

  public void addBus() {
    Output.head("Add Bus");
    if (state.ops.isEmpty() || state.routes.isEmpty()) {
      Output.print("Add operators and routes first.");
      return;
    }
    showOperators();
    String opId = input.read("Operator ID").toUpperCase(Locale.ROOT);
    if (!state.ops.containsKey(opId)) {
      Output.print("Invalid operator.");
      return;
    }
    showRoutes();
    int routeId = input.readInt("Route ID", 1, Integer.MAX_VALUE);
    if (!state.routes.containsKey(routeId)) {
      Output.print("Invalid route.");
      return;
    }
    LocalTime dep = state.parseTime(input.read("Departure (HH:mm)"));
    if (dep == null) {
      Output.print("Invalid time.");
      return;
    }
    int seats = input.readInt("Total seats", 10, 80);
    Bus b = new Bus(state.nextBusId++, opId, routeId, input.read("Bus Name"), seats, dep);
    state.buses.put(b.id, b);
    if (state.dbReady) {
      try {
        state.store.insertBus(b);
      } catch (RuntimeException ex) {
        state.buses.remove(b.id);
        state.nextBusId--;
        Output.print("Failed to save bus to database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Added bus ID: " + b.id);
  }

  public void updateBus() {
    showBuses();
    int id = input.readInt("Bus ID", 1, Integer.MAX_VALUE);
    Bus b = state.buses.get(id);
    if (b == null) {
      Output.print("Bus not found.");
      return;
    }
    String oldName = b.name;
    String oldOpId = b.opId;
    int oldRoute = b.routeId;
    int oldSeats = b.seats;
    LocalTime oldDep = b.dep;

    String nm = input.readOpt("Name [" + b.name + "]");
    if (!nm.isBlank()) b.name = nm;
    String t = input.readOpt("Departure [" + b.dep.format(AppState.TF) + "]");
    if (!t.isBlank()) {
      LocalTime pt = state.parseTime(t);
      if (pt != null) b.dep = pt;
    }
    String s = input.readOpt("Seats [" + b.seats + "]");
    if (!s.isBlank()) {
      try {
        int x = Integer.parseInt(s);
        if (x >= 10 && x <= 80) b.seats = x;
      } catch (NumberFormatException ignored) {}
    }
    String r = input.readOpt("Route ID [" + b.routeId + "]");
    if (!r.isBlank()) {
      try {
        int x = Integer.parseInt(r);
        if (state.routes.containsKey(x)) b.routeId = x;
      } catch (NumberFormatException ignored) {}
    }
    if (state.dbReady) {
      try {
        state.store.updateBus(b);
      } catch (RuntimeException ex) {
        b.name = oldName;
        b.opId = oldOpId;
        b.routeId = oldRoute;
        b.seats = oldSeats;
        b.dep = oldDep;
        Output.print("Failed to update bus in database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Bus updated.");
  }

  public void deleteBus() {
    showBuses();
    int id = input.readInt("Bus ID", 1, Integer.MAX_VALUE);
    if (!state.buses.containsKey(id)) {
      Output.print("Bus not found.");
      return;
    }
    boolean active = state.bookings.values().stream().anyMatch(b -> b.busId == id && b.status == Status.BOOKED);
    if (active) {
      Output.print("Active bookings exist. Cannot delete.");
      return;
    }
    Bus removed = state.buses.remove(id);
    if (state.dbReady) {
      try {
        state.store.deleteBus(id);
      } catch (RuntimeException ex) {
        state.buses.put(id, removed);
        Output.print("Failed to delete bus from database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Bus deleted.");
  }

  private void showOperators() {
    new OperatorAdmin(state, input).showOperators();
  }

  private void showRoutes() {
    new RouteAdmin(state, input).showRoutes();
  }
}

package bts.admin;

import java.util.ArrayList;
import java.util.List;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Route;
import bts.model.User;
import bts.util.Output;

public class ReportAdmin {
  private final AppState state;

  public ReportAdmin(AppState state) {
    this.state = state;
  }

  public void report() {
    if (state.dbReady && state.bookings.isEmpty()) {
      try {
        state.store.loadAll(state.users, state.ops, state.routes, state.buses, state.bookings);
        state.syncNextIds();
      } catch (RuntimeException ex) {
        Output.print("Failed to reload bookings from database: " + ex.getMessage());
      }
    }

    Output.head("Booking Report");
    List<List<String>> rows = new ArrayList<>();
    for (Booking b : state.bookings.values()) {
      Route r = state.routes.get(b.routeId);
      User u = state.users.get(b.userPhone);
      rows.add(List.of("" + b.id, b.status.name(), u == null ? b.userPhone : u.name, b.userPhone,
        "" + b.busId, r == null ? "N/A" : r.src + "->" + r.dst, b.date.format(AppState.DF), b.seats.toString(),
        b.pickup, b.drop, "" + b.fare));
    }
    if (rows.isEmpty()) Output.print("No bookings yet.");
    else Output.table(List.of("Booking", "Status", "User", "Phone", "Bus", "Route", "Date", "Seats", "Pickup", "Drop", "Fare"), rows);
  }
}

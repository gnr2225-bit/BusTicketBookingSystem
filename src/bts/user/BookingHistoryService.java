package bts.user;

import java.util.ArrayList;
import java.util.List;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Bus;
import bts.model.Route;
import bts.model.User;
import bts.util.Output;

public class BookingHistoryService {
  private final AppState state;

  public BookingHistoryService(AppState state) {
    this.state = state;
  }

  public void show(User u) {
    Output.head("My Bookings");
    List<List<String>> rows = new ArrayList<>();
    for (Booking b : state.bookings.values()) {
      if (!b.userPhone.equals(u.phone)) continue;
      Route r = state.routes.get(b.routeId);
      Bus bus = state.buses.get(b.busId);
      rows.add(List.of("" + b.id, b.status.name(), r == null ? "N/A" : r.src + " -> " + r.dst,
        bus == null ? "N/A" : bus.name, b.date.format(AppState.DF), b.seats.toString(),
        b.pickup, b.drop, "" + b.fare));
    }
    if (rows.isEmpty()) Output.print("No bookings found.");
    else Output.table(List.of("Booking", "Status", "Route", "Bus", "Date", "Seats", "Pickup", "Drop", "Fare"), rows);
  }
}

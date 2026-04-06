package bts.admin;

import java.util.List;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Bus;
import bts.model.Route;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class BookingLookupAdmin {
  private final AppState state;
  private final Input input;

  public BookingLookupAdmin(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void show() {
    Output.head("Booking Details");
    int id = input.readInt("Booking ID", 1, Integer.MAX_VALUE);
    Booking b = state.bookings.get(id);
    if (b == null && state.dbReady) {
      try {
        state.store.loadAll(state.users, state.ops, state.routes, state.buses, state.bookings);
        state.syncNextIds();
        b = state.bookings.get(id);
      } catch (RuntimeException ex) {
        Output.print("Failed to reload bookings from database: " + ex.getMessage());
      }
    }
    if (b == null) {
      Output.print("Booking not found.");
      return;
    }

    Route r = state.routes.get(b.routeId);
    Bus bus = state.buses.get(b.busId);
    User u = state.users.get(b.userPhone);

    String route = r == null ? "N/A" : r.src + " -> " + r.dst;
    String busName = bus == null ? "N/A" : bus.name + " (" + bus.id + ")";
    String created = b.created == null ? "N/A" : b.created.format(AppState.DTF);
    String passenger = u == null ? "N/A" : u.name;

    Output.table(List.of("Field", "Value"), List.of(
      List.of("Booking ID", "" + b.id),
      List.of("Passenger", passenger),
      List.of("Phone", b.userPhone),
      List.of("Status", b.status.name()),
      List.of("Bus", busName),
      List.of("Route", route),
      List.of("Journey Date", b.date.format(AppState.DF)),
      List.of("Seats", b.seats.toString()),
      List.of("Pickup", b.pickup),
      List.of("Drop", b.drop),
      List.of("Total Fare", "INR " + b.fare),
      List.of("Booked At", created)
    ));
  }
}

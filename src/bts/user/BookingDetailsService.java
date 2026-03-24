package bts.user;

import java.util.List;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Bus;
import bts.model.Route;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class BookingDetailsService {
  private final AppState state;
  private final Input input;

  public BookingDetailsService(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void show(User u) {
    Output.head("Booking Details");
    int id = input.readInt("Booking ID", 1, Integer.MAX_VALUE);
    Booking b = state.bookings.get(id);
    if (b == null || !b.userPhone.equals(u.phone)) {
      Output.print("Booking not found.");
      return;
    }

    Route r = state.routes.get(b.routeId);
    Bus bus = state.buses.get(b.busId);

    String route = r == null ? "N/A" : r.src + " -> " + r.dst;
    String busName = bus == null ? "N/A" : bus.name + " (" + bus.id + ")";
    String created = b.created == null ? "N/A" : b.created.format(AppState.DTF);

    Output.table(List.of("Field", "Value"), List.of(
      List.of("Booking ID", "" + b.id),
      List.of("Passenger", u.name),
      List.of("Phone", u.phone),
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

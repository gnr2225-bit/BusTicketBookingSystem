package bts.user;

import java.util.ArrayList;
import java.util.List;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Route;
import bts.model.Status;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class CancellationService {
  private final AppState state;
  private final Input input;

  public CancellationService(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void cancel(User u) {
    List<Booking> active = new ArrayList<>();
    for (Booking b : state.bookings.values()) {
      if (b.userPhone.equals(u.phone) && b.status == Status.BOOKED) active.add(b);
    }
    if (active.isEmpty()) {
      Output.print("No active bookings.");
      return;
    }
    List<List<String>> rows = new ArrayList<>();
    for (Booking b : active) {
      Route r = state.routes.get(b.routeId);
      rows.add(List.of("" + b.id, r == null ? "N/A" : r.src + " -> " + r.dst,
        b.date.format(AppState.DF), b.seats.toString(), "" + b.fare));
    }
    Output.head("Cancel Booking");
    Output.table(List.of("Booking", "Route", "Date", "Seats", "Fare"), rows);
    int id = input.readInt("Booking ID", 1, Integer.MAX_VALUE);
    Booking b = state.bookings.get(id);
    if (b == null || !b.userPhone.equals(u.phone) || b.status != Status.BOOKED) {
      Output.print("Invalid booking ID.");
      return;
    }
    b.status = Status.CANCELLED;
    if (state.dbReady) {
      try {
        state.store.updateBookingStatus(b.id, Status.CANCELLED);
      } catch (RuntimeException ex) {
        b.status = Status.BOOKED;
        Output.print("Failed to cancel booking in database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Booking cancelled.");
  }
}

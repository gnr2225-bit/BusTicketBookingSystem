package bts.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Bus;
import bts.model.Route;
import bts.model.Status;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class SearchService {
  private final AppState state;
  private final Input input;

  public SearchService(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void searchAndBook(User u) {
    Output.head("Search Routes");
    List<String> options = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (Route r : state.routes.values()) {
      String k = r.src + " -> " + r.dst;
      if (seen.add(k)) options.add(k);
    }
    if (options.isEmpty()) {
      Output.print("No routes.");
      return;
    }
    for (int i = 0; i < options.size(); i++) Output.print((i + 1) + ". " + options.get(i));
    int pick = input.readInt("Select route", 1, options.size()) - 1;
    String[] pair = options.get(pick).split(" -> ");
    LocalDate date = parseDate(input.read("Journey date (dd-MM-yyyy)"));
    if (date == null || date.isBefore(LocalDate.now())) {
      Output.print("Invalid date.");
      return;
    }

    List<Bus> match = new ArrayList<>();
    for (Bus b : state.buses.values()) {
      Route r = state.routes.get(b.routeId);
      if (r != null && r.src.equalsIgnoreCase(pair[0]) && r.dst.equalsIgnoreCase(pair[1])) match.add(b);
    }
    if (match.isEmpty()) {
      Output.print("No buses for route.");
      return;
    }

    match.sort(Comparator.comparing(x -> x.dep));
    List<List<String>> rows = new ArrayList<>();
    for (Bus b : match) {
      rows.add(List.of("" + b.id, b.name, b.opId, b.dep.format(AppState.TF), "" + b.seats,
        "" + state.available(b.id, date), "" + state.fare(b)));
    }
    Output.table(List.of("Bus", "Name", "Operator", "Departure", "Seats", "Available", "Fare/Seat"), rows);

    int busId = input.readInt("Bus ID to book", 1, Integer.MAX_VALUE);
    Bus b = state.buses.get(busId);
    if (b == null || !match.contains(b)) {
      Output.print("Invalid bus.");
      return;
    }
    Route r = state.routes.get(b.routeId);
    int avail = state.available(b.id, date);
    if (avail == 0) {
      Output.print("No seats available.");
      return;
    }
    int seats = input.readInt("How many seats", 1, Math.min(6, avail));

    String pickup = choosePoint("Select Pickup", r.pickups);
    String drop = choosePoint("Select Drop", r.drops);

    Set<Integer> booked = new HashSet<>(state.bookedSeats(b.id, date));
    showSeatMap(b.seats, booked);
    List<Integer> selected = readSeatSelection(b.seats, booked, seats);
    if (selected == null) return;

    int total = state.fare(b) * seats;
    if (!processPayment(total)) return;

    Booking bk = new Booking(state.nextBookingId++, u.phone, b.id, b.routeId, date, selected, pickup, drop,
      total, Status.BOOKED, LocalDateTime.now());
    state.bookings.put(bk.id, bk);
    if (state.dbReady) {
      try {
        state.store.insertBooking(bk);
      } catch (RuntimeException ex) {
        state.bookings.remove(bk.id);
        state.nextBookingId--;
        Output.print("Failed to save booking to database: " + ex.getMessage());
        return;
      }
    }

    Output.head("Booking Confirmed");
    Output.table(List.of("Field", "Value"), List.of(
      List.of("Booking ID", "" + bk.id),
      List.of("Passenger", u.name),
      List.of("Bus", b.name + " (" + b.id + ")"),
      List.of("Route", r.src + " -> " + r.dst),
      List.of("Date", date.format(AppState.DF)),
      List.of("Seats", bk.seats.toString()),
      List.of("Pickup", pickup),
      List.of("Drop", drop),
      List.of("Total Fare", "INR " + total)
    ));
  }

  private String choosePoint(String title, List<String> points) {
    Output.head(title);
    for (int i = 0; i < points.size(); i++) Output.print((i + 1) + ". " + points.get(i));
    return points.get(input.readInt("Choose", 1, points.size()) - 1);
  }

  private void showSeatMap(int totalSeats, Set<Integer> booked) {
    Output.head("Seat Map (XX = booked)");
    int cols = 4;
    StringBuilder line = new StringBuilder();
    for (int i = 1; i <= totalSeats; i++) {
      String cell = booked.contains(i) ? "XX" : String.format("%02d", i);
      line.append(cell);
      if (i % cols == 0 || i == totalSeats) {
        Output.print(line.toString());
        line.setLength(0);
      } else {
        line.append("  ");
      }
    }
  }

  private List<Integer> readSeatSelection(int totalSeats, Set<Integer> booked, int required) {
    while (true) {
      String raw = input.read("Enter seat numbers (comma, " + required + " seats)");
      String[] parts = raw.split(",");
      List<Integer> seats = new ArrayList<>();
      Set<Integer> seen = new HashSet<>();
      boolean ok = true;
      for (String p : parts) {
        String t = p.trim();
        if (t.isEmpty()) continue;
        int v;
        try {
          v = Integer.parseInt(t);
        } catch (NumberFormatException ex) {
          ok = false;
          break;
        }
        if (v < 1 || v > totalSeats || booked.contains(v) || !seen.add(v)) {
          ok = false;
          break;
        }
        seats.add(v);
      }
      if (!ok || seats.size() != required) {
        Output.print("Invalid seat selection. Choose " + required + " available seats.");
        continue;
      }
      return seats;
    }
  }

  private boolean processPayment(int total) {
    Output.head("Payment");
    Output.print("Total Amount: INR " + total);
    Output.print("1. UPI");
    Output.print("2. Card");
    Output.print("3. Wallet");
    Output.print("4. Cancel");
    int c = input.readInt("Choose", 1, 4);
    if (c == 4) {
      Output.print("Payment cancelled.");
      return false;
    }
    input.read("Enter reference/last 4 digits");
    Output.print("Payment successful (dummy).");
    return true;
  }

  private LocalDate parseDate(String s) {
    try {
      return LocalDate.parse(s, AppState.DF);
    } catch (Exception ex) {
      return null;
    }
  }
}

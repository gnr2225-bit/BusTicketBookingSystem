package bts.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bts.app.AppState;
import bts.model.Booking;
import bts.model.Bus;
import bts.model.Route;
import bts.model.Status;
import bts.model.User;

public class BookingSeedService {
  private final AppState state;
  private final Random rnd = new Random();

  public BookingSeedService(AppState state) {
    this.state = state;
  }

  public int seedRandomBookings(int count) {
    ensureUsers(5);
    List<User> users = new ArrayList<>(state.users.values());
    List<Bus> buses = new ArrayList<>(state.buses.values());
    if (users.isEmpty() || buses.isEmpty()) return 0;

    int created = 0;
    int attempts = 0;
    int maxAttempts = count * 30;
    while (created < count && attempts++ < maxAttempts) {
      User u = users.get(rnd.nextInt(users.size()));
      Bus bus = buses.get(rnd.nextInt(buses.size()));
      Route r = state.routes.get(bus.routeId);
      if (r == null) continue;

      LocalDate date = LocalDate.now().plusDays(1 + rnd.nextInt(20));
      int avail = state.available(bus.id, date);
      if (avail <= 0) continue;

      int seats = 1 + rnd.nextInt(Math.min(3, avail));
      List<Integer> selected = state.allocate(bus.seats, state.bookedSeats(bus.id, date), seats);
      if (selected.size() != seats) continue;

      String pickup = pickPoint(r.pickups, "Pickup");
      String drop = pickPoint(r.drops, "Drop");
      if (pickup.equals(drop) && r.drops.size() > 1) {
        drop = r.drops.get((r.drops.indexOf(drop) + 1) % r.drops.size());
      }

      int total = state.fare(bus) * seats;
      Booking bk = new Booking(
        state.nextBookingId++,
        u.phone,
        bus.id,
        bus.routeId,
        date,
        selected,
        pickup,
        drop,
        total,
        Status.BOOKED,
        LocalDateTime.now().minusMinutes(rnd.nextInt(720))
      );

      state.bookings.put(bk.id, bk);
      if (state.dbReady) {
        try {
          state.store.insertBooking(bk);
        } catch (RuntimeException ex) {
          state.bookings.remove(bk.id);
          state.nextBookingId--;
          continue;
        }
      }
      created++;
    }
    return created;
  }

  private void ensureUsers(int count) {
    if (!state.users.isEmpty()) return;
    int created = 0;
    int attempts = 0;
    while (created < count && attempts++ < count * 20) {
      String phone = "9" + String.format("%09d", 100000000 + rnd.nextInt(900000000));
      if (state.users.containsKey(phone)) continue;
      String email = "seed" + (created + 1) + "@example.com";
      User u = new User(phone, "Pass1234", "Seed User " + (created + 1), "Seed Address", email);
      state.users.put(phone, u);
      if (state.dbReady) {
        try {
          state.store.insertUser(u);
        } catch (RuntimeException ex) {
          state.users.remove(phone);
          continue;
        }
      }
      created++;
    }
  }

  private String pickPoint(List<String> points, String label) {
    if (points == null || points.isEmpty()) return label + " Point";
    return points.get(rnd.nextInt(points.size()));
  }
}

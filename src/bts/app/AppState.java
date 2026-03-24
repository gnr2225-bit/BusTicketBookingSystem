package bts.app;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bts.model.*;
import bts.store.Store;

public class AppState {
  public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  public static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

  public final Map<String, User> users = new LinkedHashMap<>();
  public final Map<String, Operator> ops = new LinkedHashMap<>();
  public final Map<Integer, Route> routes = new LinkedHashMap<>();
  public final Map<Integer, Bus> buses = new LinkedHashMap<>();
  public final Map<Integer, Booking> bookings = new LinkedHashMap<>();
  public final Store store;

  public boolean dbReady = false;
  public int nextRouteId = 1;
  public int nextBusId = 1001;
  public int nextBookingId = 50001;

  public AppState(Store store) {
    this.store = store;
  }

  public void syncNextIds() {
    if (dbReady) {
      nextRouteId = store.maxRouteId() + 1;
      nextBusId = store.maxBusId() + 1;
      nextBookingId = store.maxBookingId() + 1;
      return;
    }
    nextRouteId = routes.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    nextBusId = buses.keySet().stream().max(Integer::compareTo).orElse(1000) + 1;
    nextBookingId = bookings.keySet().stream().max(Integer::compareTo).orElse(50000) + 1;
  }

  public int available(int busId, LocalDate date) {
    Bus b = buses.get(busId);
    if (b == null) return 0;
    return b.seats - bookedCount(busId, date);
  }

  public int bookedCount(int busId, LocalDate date) {
    int c = 0;
    for (Booking b : bookings.values()) {
      if (b.busId == busId && b.date.equals(date) && b.status == Status.BOOKED) c += b.seats.size();
    }
    return c;
  }

  public List<Integer> bookedSeats(int busId, LocalDate date) {
    Set<Integer> s = new HashSet<>();
    for (Booking b : bookings.values()) {
      if (b.busId == busId && b.date.equals(date) && b.status == Status.BOOKED) s.addAll(b.seats);
    }
    List<Integer> out = new ArrayList<>(s);
    Collections.sort(out);
    return out;
  }

  public List<Integer> allocate(int totalSeats, List<Integer> booked, int need) {
    Set<Integer> b = new HashSet<>(booked);
    List<Integer> a = new ArrayList<>();
    for (int i = 1; i <= totalSeats; i++) {
      if (!b.contains(i)) {
        a.add(i);
        if (a.size() == need) break;
      }
    }
    return a;
  }

  public int fare(Bus b) {
    Route r = routes.get(b.routeId);
    if (r == null) return 500;
    int base = Math.abs(r.src.length() - r.dst.length()) * 20 + 300;
    return base + (b.seats > 40 ? 30 : 60);
  }

  public LocalTime parseTime(String s) {
    try {
      return LocalTime.parse(s, TF);
    } catch (Exception ex) {
      return null;
    }
  }
}

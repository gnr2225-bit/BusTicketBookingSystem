package bts.store;

import java.util.Map;
import bts.model.*;

public interface Store {
  boolean isAvailable();
  void loadAll(Map<String, User> users, Map<String, Operator> ops, Map<Integer, Route> routes,
               Map<Integer, Bus> buses, Map<Integer, Booking> bookings);

  int maxRouteId();
  int maxBusId();
  int maxBookingId();

  void insertUser(User u);
  void updateUser(User u);

  void upsertOperator(Operator o);
  void insertRoute(Route r);
  void updateRoute(Route r);
  void deleteRoute(int routeId);

  void insertBus(Bus b);
  void updateBus(Bus b);
  void deleteBus(int busId);

  void insertBooking(Booking b);
  void updateBookingStatus(int bookingId, Status status);
}

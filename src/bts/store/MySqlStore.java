package bts.store;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bts.model.*;

public class MySqlStore implements Store {
  private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/bus_ticket_system?serverTimezone=Asia/Kolkata";
  private static final String DEFAULT_USER = "admin";
  private static final String DEFAULT_PASS = "password123";

  private final String url;
  private final String user;
  private final String pass;

  public MySqlStore() {
    this.url = envOrDefault("BTS_DB_URL", DEFAULT_URL);
    this.user = envOrDefault("BTS_DB_USER", DEFAULT_USER);
    this.pass = envOrDefault("BTS_DB_PASS", DEFAULT_PASS);
  }

  private String envOrDefault(String key, String fallback) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? fallback : v;
  }

  private Connection conn() throws SQLException {
    return DriverManager.getConnection(url, user, pass);
  }

  @Override
  public boolean isAvailable() {
    try (Connection c = conn()) {
      return c.isValid(3);
    } catch (SQLException ex) {
      return false;
    }
  }

  @Override
  public void loadAll(Map<String, User> users, Map<String, Operator> ops, Map<Integer, Route> routes,
                      Map<Integer, Bus> buses, Map<Integer, Booking> bookings) {
    users.clear();
    ops.clear();
    routes.clear();
    buses.clear();
    bookings.clear();

    try (Connection c = conn()) {
      loadUsers(c, users);
      loadOperators(c, ops);
      loadRoutes(c, routes);
      loadBuses(c, buses);
      loadBookings(c, bookings);
    } catch (SQLException ex) {
      throw new RuntimeException("Failed to load MySQL data: " + ex.getMessage(), ex);
    }
  }

  private void loadUsers(Connection c, Map<String, User> users) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(
      "SELECT phone, pass_hash, name, addr, email FROM users ORDER BY phone")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        User u = new User(rs.getString("phone"), rs.getString("pass_hash"), rs.getString("name"),
          rs.getString("addr"), rs.getString("email"));
        users.put(u.phone, u);
      }
    }
  }

  private void loadOperators(Connection c, Map<String, Operator> ops) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(
      "SELECT op_id, name, contact FROM operators ORDER BY op_id")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Operator o = new Operator(rs.getString("op_id"), rs.getString("name"), rs.getString("contact"));
        ops.put(o.id, o);
      }
    }
  }

  private void loadRoutes(Connection c, Map<Integer, Route> routes) throws SQLException {
    Map<Integer, List<String>> pickups = new LinkedHashMap<>();
    Map<Integer, List<String>> drops = new LinkedHashMap<>();

    try (PreparedStatement ps = c.prepareStatement(
      "SELECT route_id, point_type, point_name FROM route_points ORDER BY route_id, point_type, sequence_no")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        int rid = rs.getInt("route_id");
        String kind = rs.getString("point_type");
        String point = rs.getString("point_name");
        if ("PICKUP".equalsIgnoreCase(kind)) {
          pickups.computeIfAbsent(rid, x -> new ArrayList<>()).add(point);
        } else {
          drops.computeIfAbsent(rid, x -> new ArrayList<>()).add(point);
        }
      }
    }

    try (PreparedStatement ps = c.prepareStatement(
      "SELECT route_id, src, dst FROM routes ORDER BY route_id")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("route_id");
        List<String> pu = pickups.getOrDefault(id, List.of());
        List<String> dr = drops.getOrDefault(id, List.of());
        Route r = new Route(id, rs.getString("src"), rs.getString("dst"), pu, dr);
        routes.put(id, r);
      }
    }
  }

  private void loadBuses(Connection c, Map<Integer, Bus> buses) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(
      "SELECT bus_id, op_id, route_id, bus_name, total_seats, dep_time FROM buses ORDER BY bus_id")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Bus b = new Bus(
          rs.getInt("bus_id"),
          rs.getString("op_id"),
          rs.getInt("route_id"),
          rs.getString("bus_name"),
          rs.getInt("total_seats"),
          rs.getTime("dep_time").toLocalTime()
        );
        buses.put(b.id, b);
      }
    }
  }

  private void loadBookings(Connection c, Map<Integer, Booking> bookings) throws SQLException {
    Map<Integer, List<Integer>> seatMap = new LinkedHashMap<>();
    try (PreparedStatement ps = c.prepareStatement(
      "SELECT booking_id, seat_no FROM booking_seats ORDER BY booking_id, seat_no")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        seatMap.computeIfAbsent(rs.getInt("booking_id"), x -> new ArrayList<>()).add(rs.getInt("seat_no"));
      }
    }

    try (PreparedStatement ps = c.prepareStatement(
      "SELECT booking_id, user_phone, bus_id, route_id, journey_date, pickup_point, drop_point, total_fare, status, created_at FROM bookings ORDER BY booking_id")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("booking_id");
        Booking b = new Booking(
          id,
          rs.getString("user_phone"),
          rs.getInt("bus_id"),
          rs.getInt("route_id"),
          rs.getDate("journey_date").toLocalDate(),
          seatMap.getOrDefault(id, List.of()),
          rs.getString("pickup_point"),
          rs.getString("drop_point"),
          rs.getInt("total_fare"),
          Status.valueOf(rs.getString("status")),
          rs.getTimestamp("created_at").toLocalDateTime()
        );
        bookings.put(id, b);
      }
    }
  }

  @Override
  public int maxRouteId() {
    return maxId("SELECT COALESCE(MAX(route_id), 0) FROM routes");
  }

  @Override
  public int maxBusId() {
    return maxId("SELECT COALESCE(MAX(bus_id), 1000) FROM buses");
  }

  @Override
  public int maxBookingId() {
    return maxId("SELECT COALESCE(MAX(booking_id), 50000) FROM bookings");
  }

  private int maxId(String sql) {
    try (Connection c = conn(); Statement st = c.createStatement()) {
      ResultSet rs = st.executeQuery(sql);
      rs.next();
      return rs.getInt(1);
    } catch (SQLException ex) {
      throw new RuntimeException("Failed to fetch max id: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void insertUser(User u) {
    executeUpdate("INSERT INTO users(phone, pass_hash, name, addr, email) VALUES(?,?,?,?,?)",
      u.phone, u.pass, u.name, u.addr, u.email);
  }

  @Override
  public void updateUser(User u) {
    executeUpdate("UPDATE users SET pass_hash=?, name=?, addr=?, email=? WHERE phone=?",
      u.pass, u.name, u.addr, u.email, u.phone);
  }

  @Override
  public void upsertOperator(Operator o) {
    executeUpdate("INSERT INTO operators(op_id, name, contact) VALUES(?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name), contact=VALUES(contact)",
      o.id, o.name, o.contact);
  }

  @Override
  public void insertRoute(Route r) {
    try (Connection c = conn()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO routes(route_id, src, dst) VALUES(?,?,?)")) {
          ps.setInt(1, r.id);
          ps.setString(2, r.src);
          ps.setString(3, r.dst);
          ps.executeUpdate();
        }
        insertRoutePoints(c, r);
        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    } catch (SQLException ex) {
      throw new RuntimeException("Failed to insert route: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void updateRoute(Route r) {
    try (Connection c = conn()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement("UPDATE routes SET src=?, dst=? WHERE route_id=?")) {
          ps.setString(1, r.src);
          ps.setString(2, r.dst);
          ps.setInt(3, r.id);
          ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM route_points WHERE route_id=?")) {
          ps.setInt(1, r.id);
          ps.executeUpdate();
        }
        insertRoutePoints(c, r);
        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    } catch (SQLException ex) {
      throw new RuntimeException("Failed to update route: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void deleteRoute(int routeId) {
    executeUpdate("DELETE FROM routes WHERE route_id=?", routeId);
  }

  private void insertRoutePoints(Connection c, Route r) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(
      "INSERT INTO route_points(route_id, point_type, point_name, sequence_no) VALUES(?,?,?,?)")) {
      int seq = 1;
      for (String p : r.pickups) {
        ps.setInt(1, r.id);
        ps.setString(2, "PICKUP");
        ps.setString(3, p);
        ps.setInt(4, seq++);
        ps.addBatch();
      }
      seq = 1;
      for (String p : r.drops) {
        ps.setInt(1, r.id);
        ps.setString(2, "DROP");
        ps.setString(3, p);
        ps.setInt(4, seq++);
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  @Override
  public void insertBus(Bus b) {
    executeUpdate("INSERT INTO buses(bus_id, op_id, route_id, bus_name, total_seats, dep_time) VALUES(?,?,?,?,?,?)",
      b.id, b.opId, b.routeId, b.name, b.seats, Time.valueOf(b.dep));
  }

  @Override
  public void updateBus(Bus b) {
    executeUpdate("UPDATE buses SET op_id=?, route_id=?, bus_name=?, total_seats=?, dep_time=? WHERE bus_id=?",
      b.opId, b.routeId, b.name, b.seats, Time.valueOf(b.dep), b.id);
  }

  @Override
  public void deleteBus(int busId) {
    executeUpdate("DELETE FROM buses WHERE bus_id=?", busId);
  }

  @Override
  public void insertBooking(Booking b) {
    try (Connection c = conn()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO bookings(booking_id, user_phone, bus_id, route_id, journey_date, pickup_point, drop_point, total_fare, status, created_at) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
          ps.setInt(1, b.id);
          ps.setString(2, b.userPhone);
          ps.setInt(3, b.busId);
          ps.setInt(4, b.routeId);
          ps.setDate(5, Date.valueOf(b.date));
          ps.setString(6, b.pickup);
          ps.setString(7, b.drop);
          ps.setInt(8, b.fare);
          ps.setString(9, b.status.name());
          ps.setTimestamp(10, Timestamp.valueOf(b.created));
          ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO booking_seats(booking_id, seat_no) VALUES(?,?)")) {
          for (Integer seat : b.seats) {
            ps.setInt(1, b.id);
            ps.setInt(2, seat);
            ps.addBatch();
          }
          ps.executeBatch();
        }
        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    } catch (SQLException ex) {
      throw new RuntimeException("Failed to insert booking: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void updateBookingStatus(int bookingId, Status status) {
    executeUpdate("UPDATE bookings SET status=? WHERE booking_id=?", status.name(), bookingId);
  }

  private void executeUpdate(String sql, Object... params) {
    try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        ps.setObject(i + 1, params[i]);
      }
      ps.executeUpdate();
    } catch (SQLException ex) {
      throw new RuntimeException("SQL update failed: " + ex.getMessage(), ex);
    }
  }
}

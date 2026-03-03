import java.time.*;
import java.time.format.*;
import java.util.*;

public class BusTicketSystem {
  static final String ADMIN_ID = "admin", ADMIN_PASS = "password";
  static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");

  final Scanner sc = new Scanner(System.in);
  final Map<String, User> users = new LinkedHashMap<>();
  final Map<String, Operator> ops = new LinkedHashMap<>();
  final Map<Integer, Route> routes = new LinkedHashMap<>();
  final Map<Integer, Bus> buses = new LinkedHashMap<>();
  final Map<Integer, Booking> bookings = new LinkedHashMap<>();
  final MySqlStore store = new MySqlStore();
  boolean dbReady = false;

  int nextRouteId = 1, nextBusId = 1001, nextBookingId = 50001;

  public static void main(String[] args) {
    BusTicketSystem app = new BusTicketSystem();
    app.initData();
    app.mainMenu();
  }

  void initData() {
    if (!store.isAvailable()) {
      seed();
      p("MySQL not reachable, running in-memory mode.");
      return;
    }

    dbReady = true;
    try {
      store.loadAll(users, ops, routes, buses, bookings);
      syncNextIds();
      if (ops.isEmpty()) {
        seedOps();
        persistOperators();
      }
      if (routes.isEmpty()) {
        seedRoutes();
        persistRoutes();
      }
      if (buses.isEmpty()) {
        seedBuses();
        persistBuses();
      }
      if (ops.isEmpty() && routes.isEmpty() && buses.isEmpty()) {
        seed();
        persistSeeds();
        syncNextIds();
      }
      syncNextIds();
      p("MySQL connected. Data loaded from database.");
    } catch (RuntimeException ex) {
      dbReady = false;
      users.clear();
      ops.clear();
      routes.clear();
      buses.clear();
      bookings.clear();
      seed();
      p("MySQL load failed, running in-memory mode: " + ex.getMessage());
    }
  }

  void syncNextIds() {
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

  void persistSeeds() {
    persistOperators();
    persistRoutes();
    persistBuses();
  }

  void persistOperators() {
    for (Operator o : ops.values()) store.upsertOperator(o);
  }

  void persistRoutes() {
    for (Route r : routes.values()) store.insertRoute(r);
  }

  void persistBuses() {
    for (Bus b : buses.values()) store.insertBus(b);
  }

  void mainMenu() {
    while (true) {
      head("South India Bus Ticket System");
      p("1. Admin Mode");
      p("2. User Mode");
      p("3. Exit");
      int c = readInt("Choose", 1, 3);
      if (c == 1) adminLogin();
      else if (c == 2) userMode();
      else { p("\nThank you."); return; }
    }
  }

  void adminLogin() {
    head("Admin Login");
    String id = read("Admin ID"), pass = read("Password");
    if (ADMIN_ID.equals(id) && ADMIN_PASS.equals(pass)) adminMenu();
    else p("Invalid admin credentials.");
  }

  void adminMenu() {
    while (true) {
      head("Admin Panel");
      p("1. Show Routes");
      p("2. Add Route");
      p("3. Update Route");
      p("4. Delete Route");
      p("5. Show Operators");
      p("6. Add Operator");
      p("7. Show Buses");
      p("8. Add Bus");
      p("9. Update Bus");
      p("10. Delete Bus");
      p("11. Booking Reports");
      p("12. Back");
      int c = readInt("Choose", 1, 12);
      switch (c) {
        case 1 -> showRoutes();
        case 2 -> addRoute();
        case 3 -> updRoute();
        case 4 -> delRoute();
        case 5 -> showOps();
        case 6 -> addOp();
        case 7 -> showBuses();
        case 8 -> addBus();
        case 9 -> updBus();
        case 10 -> delBus();
        case 11 -> report();
        case 12 -> { return; }
      }
      pause();
    }
  }

  void userMode() {
    while (true) {
      head("User Mode");
      p("1. Register");
      p("2. Login");
      p("3. Back");
      int c = readInt("Choose", 1, 3);
      if (c == 1) register();
      else if (c == 2) login();
      else return;
      pause();
    }
  }

  void register() {
    head("Register");
    String phone;
    while (true) {
      phone = read("Phone (10 digits)");
      if (!phone.matches("\\d{10}")) { p("Phone must be 10 digits."); continue; }
      if (users.containsKey(phone)) { p("Phone already registered."); continue; }
      break;
    }
    String pass = readPassword();
    String name = read("Name");
    String addr = read("Address");
    String email = readEmail();
    User u = new User(phone, pass, name, addr, email);
    users.put(phone, u);
    if (dbReady) {
      try {
        store.insertUser(u);
      } catch (RuntimeException ex) {
        users.remove(phone);
        p("Failed to save user to database: " + ex.getMessage());
        return;
      }
    }
    p("Registration successful.");
  }

  void login() {
    head("User Login");
    String phone = read("Phone"), pass = read("Password");
    User u = users.get(phone);
    if (u == null || !u.pass.equals(pass)) { p("Invalid credentials."); return; }
    userDash(u);
  }

  void userDash(User u) {
    while (true) {
      head("Welcome, " + u.name);
      p("1. View/Edit Profile");
      p("2. Search & Book");
      p("3. My Bookings");
      p("4. Cancel Booking");
      p("5. Logout");
      int c = readInt("Choose", 1, 5);
      switch (c) {
        case 1 -> editProfile(u);
        case 2 -> searchBook(u);
        case 3 -> myBookings(u);
        case 4 -> cancel(u);
        case 5 -> { return; }
      }
      pause();
    }
  }

  void editProfile(User u) {
    head("Profile");
    table(List.of("Field", "Value"), List.of(
      List.of("Phone", u.phone), List.of("Name", u.name), List.of("Address", u.addr), List.of("Email", u.email)
    ));
    p("1. Password  2. Name  3. Address  4. Email  5. Back");
    int c = readInt("Choose", 1, 5);
    String oldPass = u.pass, oldName = u.name, oldAddr = u.addr, oldEmail = u.email;
    if (c == 1) u.pass = readPassword();
    else if (c == 2) u.name = read("New Name");
    else if (c == 3) u.addr = read("New Address");
    else if (c == 4) u.email = readEmail();

    if (dbReady && c != 5) {
      try {
        store.updateUser(u);
      } catch (RuntimeException ex) {
        u.pass = oldPass;
        u.name = oldName;
        u.addr = oldAddr;
        u.email = oldEmail;
        p("Failed to update profile in database: " + ex.getMessage());
      }
    }
  }

  void showRoutes() {
    head("Routes");
    List<List<String>> rows = new ArrayList<>();
    for (Route r : routes.values()) rows.add(List.of(""+r.id, r.src, r.dst, ""+r.pickups.size(), ""+r.drops.size()));
    table(List.of("Route", "Source", "Destination", "PickupPts", "DropPts"), rows);
  }

  void addRoute() {
    head("Add Route");
    String src = cap(read("Source City")), dst = cap(read("Destination City"));
    List<String> pu = readPoints("Pickup points (comma)");
    List<String> dr = readPoints("Drop points (comma)");
    Route r = new Route(nextRouteId++, src, dst, pu, dr);
    routes.put(r.id, r);
    if (dbReady) {
      try {
        store.insertRoute(r);
      } catch (RuntimeException ex) {
        routes.remove(r.id);
        nextRouteId--;
        p("Failed to save route to database: " + ex.getMessage());
        return;
      }
    }
    p("Added route ID: " + r.id);
  }

  void updRoute() {
    showRoutes();
    int id = readInt("Route ID", 1, Integer.MAX_VALUE);
    Route r = routes.get(id);
    if (r == null) { p("Route not found."); return; }
    String oldSrc = r.src, oldDst = r.dst;
    List<String> oldPickups = new ArrayList<>(r.pickups);
    List<String> oldDrops = new ArrayList<>(r.drops);
    String src = readOpt("Source [" + r.src + "]");
    if (!src.isBlank()) r.src = cap(src);
    String dst = readOpt("Destination [" + r.dst + "]");
    if (!dst.isBlank()) r.dst = cap(dst);
    String pu = readOpt("Pickup comma list (blank skip)");
    if (!pu.isBlank()) r.pickups = parsePoints(pu);
    String dr = readOpt("Drop comma list (blank skip)");
    if (!dr.isBlank()) r.drops = parsePoints(dr);
    if (dbReady) {
      try {
        store.updateRoute(r);
      } catch (RuntimeException ex) {
        r.src = oldSrc;
        r.dst = oldDst;
        r.pickups = oldPickups;
        r.drops = oldDrops;
        p("Failed to update route in database: " + ex.getMessage());
        return;
      }
    }
    p("Route updated.");
  }

  void delRoute() {
    showRoutes();
    int id = readInt("Route ID", 1, Integer.MAX_VALUE);
    if (!routes.containsKey(id)) { p("Route not found."); return; }
    boolean linked = buses.values().stream().anyMatch(b -> b.routeId == id);
    if (linked) { p("Delete buses linked to this route first."); return; }
    Route removed = routes.remove(id);
    if (dbReady) {
      try {
        store.deleteRoute(id);
      } catch (RuntimeException ex) {
        routes.put(id, removed);
        p("Failed to delete route from database: " + ex.getMessage());
        return;
      }
    }
    p("Route deleted.");
  }

  void showOps() {
    head("Operators");
    List<List<String>> rows = new ArrayList<>();
    for (Operator o : ops.values()) rows.add(List.of(o.id, o.name, o.contact));
    table(List.of("ID", "Name", "Contact"), rows);
  }
  void addOp() {
    head("Add Operator");
    String id;
    while (true) {
      id = read("Operator ID").toUpperCase(Locale.ROOT);
      if (!ops.containsKey(id)) break;
      p("Operator ID exists.");
    }
    Operator o = new Operator(id, read("Operator Name"), read("Contact"));
    if (dbReady) {
      try {
        store.upsertOperator(o);
      } catch (RuntimeException ex) {
        p("Failed to save operator to database: " + ex.getMessage());
        return;
      }
    }
    ops.put(id, o);
    p("Operator added.");
  }

  void showBuses() {
    head("Buses");
    List<List<String>> rows = new ArrayList<>();
    for (Bus b : buses.values()) {
      Route r = routes.get(b.routeId);
      String rt = r == null ? "N/A" : r.src + " -> " + r.dst;
      rows.add(List.of(""+b.id, b.opId, b.name, rt, b.dep.format(TF), ""+b.seats, ""+available(b.id, LocalDate.now())));
    }
    table(List.of("Bus", "Operator", "Name", "Route", "Dep", "Seats", "Avail(Today)"), rows);
  }

  void addBus() {
    head("Add Bus");
    if (ops.isEmpty() || routes.isEmpty()) { p("Add operators and routes first."); return; }
    showOps();
    String opId = read("Operator ID").toUpperCase(Locale.ROOT);
    if (!ops.containsKey(opId)) { p("Invalid operator."); return; }
    showRoutes();
    int routeId = readInt("Route ID", 1, Integer.MAX_VALUE);
    if (!routes.containsKey(routeId)) { p("Invalid route."); return; }
    LocalTime dep = parseTime(read("Departure (HH:mm)"));
    if (dep == null) { p("Invalid time."); return; }
    int seats = readInt("Total seats", 10, 80);
    Bus b = new Bus(nextBusId++, opId, routeId, read("Bus Name"), seats, dep);
    buses.put(b.id, b);
    if (dbReady) {
      try {
        store.insertBus(b);
      } catch (RuntimeException ex) {
        buses.remove(b.id);
        nextBusId--;
        p("Failed to save bus to database: " + ex.getMessage());
        return;
      }
    }
    p("Added bus ID: " + b.id);
  }

  void updBus() {
    showBuses();
    int id = readInt("Bus ID", 1, Integer.MAX_VALUE);
    Bus b = buses.get(id);
    if (b == null) { p("Bus not found."); return; }
    String oldName = b.name, oldOpId = b.opId;
    int oldRoute = b.routeId, oldSeats = b.seats;
    LocalTime oldDep = b.dep;
    String nm = readOpt("Name [" + b.name + "]");
    if (!nm.isBlank()) b.name = nm;
    String t = readOpt("Departure [" + b.dep.format(TF) + "]");
    if (!t.isBlank()) { LocalTime pt = parseTime(t); if (pt != null) b.dep = pt; }
    String s = readOpt("Seats [" + b.seats + "]");
    if (!s.isBlank()) {
      try { int x = Integer.parseInt(s); if (x >= 10 && x <= 80) b.seats = x; }
      catch (NumberFormatException ignored) {}
    }
    String r = readOpt("Route ID [" + b.routeId + "]");
    if (!r.isBlank()) {
      try { int x = Integer.parseInt(r); if (routes.containsKey(x)) b.routeId = x; }
      catch (NumberFormatException ignored) {}
    }
    if (dbReady) {
      try {
        store.updateBus(b);
      } catch (RuntimeException ex) {
        b.name = oldName;
        b.opId = oldOpId;
        b.routeId = oldRoute;
        b.seats = oldSeats;
        b.dep = oldDep;
        p("Failed to update bus in database: " + ex.getMessage());
        return;
      }
    }
    p("Bus updated.");
  }

  void delBus() {
    showBuses();
    int id = readInt("Bus ID", 1, Integer.MAX_VALUE);
    if (!buses.containsKey(id)) { p("Bus not found."); return; }
    boolean active = bookings.values().stream().anyMatch(b -> b.busId == id && b.status == Status.BOOKED);
    if (active) { p("Active bookings exist. Cannot delete."); return; }
    Bus removed = buses.remove(id);
    if (dbReady) {
      try {
        store.deleteBus(id);
      } catch (RuntimeException ex) {
        buses.put(id, removed);
        p("Failed to delete bus from database: " + ex.getMessage());
        return;
      }
    }
    p("Bus deleted.");
  }

  void report() {
    head("Booking Report");
    List<List<String>> rows = new ArrayList<>();
    for (Booking b : bookings.values()) {
      Route r = routes.get(b.routeId);
      User u = users.get(b.userPhone);
      rows.add(List.of(""+b.id, b.status.name(), u == null ? b.userPhone : u.name, b.userPhone,
        ""+b.busId, r == null ? "N/A" : r.src + "->" + r.dst, b.date.format(DF), b.seats.toString(), b.pickup, b.drop, ""+b.fare));
    }
    if (rows.isEmpty()) p("No bookings yet.");
    else table(List.of("Booking", "Status", "User", "Phone", "Bus", "Route", "Date", "Seats", "Pickup", "Drop", "Fare"), rows);
  }

  void searchBook(User u) {
    head("Search Routes");
    List<String> options = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (Route r : routes.values()) {
      String k = r.src + " -> " + r.dst;
      if (seen.add(k)) options.add(k);
    }
    if (options.isEmpty()) { p("No routes."); return; }
    for (int i = 0; i < options.size(); i++) p((i + 1) + ". " + options.get(i));
    int pick = readInt("Select route", 1, options.size()) - 1;
    String[] pair = options.get(pick).split(" -> ");
    LocalDate date = parseDate(read("Journey date (dd-MM-yyyy)"));
    if (date == null || date.isBefore(LocalDate.now())) { p("Invalid date."); return; }

    List<Bus> match = new ArrayList<>();
    for (Bus b : buses.values()) {
      Route r = routes.get(b.routeId);
      if (r != null && r.src.equalsIgnoreCase(pair[0]) && r.dst.equalsIgnoreCase(pair[1])) match.add(b);
    }
    if (match.isEmpty()) { p("No buses for route."); return; }

    match.sort(Comparator.comparing(x -> x.dep));
    List<List<String>> rows = new ArrayList<>();
    for (Bus b : match) rows.add(List.of(""+b.id, b.name, b.opId, b.dep.format(TF), ""+b.seats, ""+available(b.id, date), ""+fare(b)));
    table(List.of("Bus", "Name", "Operator", "Departure", "Seats", "Available", "Fare/Seat"), rows);

    int busId = readInt("Bus ID to book", 1, Integer.MAX_VALUE);
    Bus b = buses.get(busId);
    if (b == null || !match.contains(b)) { p("Invalid bus."); return; }
    Route r = routes.get(b.routeId);
    int avail = available(b.id, date);
    if (avail == 0) { p("No seats available."); return; }
    int seats = readInt("How many seats", 1, Math.min(6, avail));

    String pickup = choosePoint("Select Pickup", r.pickups);
    String drop = choosePoint("Select Drop", r.drops);

    List<Integer> alloc = allocate(b.seats, bookedSeats(b.id, date), seats);
    if (alloc.size() != seats) { p("Seat allocation failed."); return; }

    int total = fare(b) * seats;
    Booking bk = new Booking(nextBookingId++, u.phone, b.id, b.routeId, date, alloc, pickup, drop, total, Status.BOOKED, LocalDateTime.now());
    bookings.put(bk.id, bk);
    if (dbReady) {
      try {
        store.insertBooking(bk);
      } catch (RuntimeException ex) {
        bookings.remove(bk.id);
        nextBookingId--;
        p("Failed to save booking to database: " + ex.getMessage());
        return;
      }
    }

    head("Booking Confirmed");
    table(List.of("Field", "Value"), List.of(
      List.of("Booking ID", ""+bk.id), List.of("Passenger", u.name), List.of("Bus", b.name + " (" + b.id + ")"),
      List.of("Route", r.src + " -> " + r.dst), List.of("Date", date.format(DF)), List.of("Seats", bk.seats.toString()),
      List.of("Pickup", pickup), List.of("Drop", drop), List.of("Total Fare", "INR " + total)
    ));
  }
  void myBookings(User u) {
    head("My Bookings");
    List<List<String>> rows = new ArrayList<>();
    for (Booking b : bookings.values()) {
      if (!b.userPhone.equals(u.phone)) continue;
      Route r = routes.get(b.routeId);
      Bus bus = buses.get(b.busId);
      rows.add(List.of(""+b.id, b.status.name(), r == null ? "N/A" : r.src + " -> " + r.dst,
        bus == null ? "N/A" : bus.name, b.date.format(DF), b.seats.toString(), b.pickup, b.drop, ""+b.fare));
    }
    if (rows.isEmpty()) p("No bookings found.");
    else table(List.of("Booking", "Status", "Route", "Bus", "Date", "Seats", "Pickup", "Drop", "Fare"), rows);
  }

  void cancel(User u) {
    List<Booking> active = new ArrayList<>();
    for (Booking b : bookings.values()) if (b.userPhone.equals(u.phone) && b.status == Status.BOOKED) active.add(b);
    if (active.isEmpty()) { p("No active bookings."); return; }
    List<List<String>> rows = new ArrayList<>();
    for (Booking b : active) {
      Route r = routes.get(b.routeId);
      rows.add(List.of(""+b.id, r == null ? "N/A" : r.src + " -> " + r.dst, b.date.format(DF), b.seats.toString(), ""+b.fare));
    }
    head("Cancel Booking");
    table(List.of("Booking", "Route", "Date", "Seats", "Fare"), rows);
    int id = readInt("Booking ID", 1, Integer.MAX_VALUE);
    Booking b = bookings.get(id);
    if (b == null || !b.userPhone.equals(u.phone) || b.status != Status.BOOKED) { p("Invalid booking ID."); return; }
    b.status = Status.CANCELLED;
    if (dbReady) {
      try {
        store.updateBookingStatus(b.id, Status.CANCELLED);
      } catch (RuntimeException ex) {
        b.status = Status.BOOKED;
        p("Failed to cancel booking in database: " + ex.getMessage());
        return;
      }
    }
    p("Booking cancelled.");
  }

  int available(int busId, LocalDate date) {
    Bus b = buses.get(busId);
    if (b == null) return 0;
    return b.seats - bookedCount(busId, date);
  }

  int bookedCount(int busId, LocalDate date) {
    int c = 0;
    for (Booking b : bookings.values()) if (b.busId == busId && b.date.equals(date) && b.status == Status.BOOKED) c += b.seats.size();
    return c;
  }

  List<Integer> bookedSeats(int busId, LocalDate date) {
    Set<Integer> s = new HashSet<>();
    for (Booking b : bookings.values()) if (b.busId == busId && b.date.equals(date) && b.status == Status.BOOKED) s.addAll(b.seats);
    List<Integer> out = new ArrayList<>(s);
    Collections.sort(out);
    return out;
  }

  List<Integer> allocate(int totalSeats, List<Integer> booked, int need) {
    Set<Integer> b = new HashSet<>(booked);
    List<Integer> a = new ArrayList<>();
    for (int i = 1; i <= totalSeats; i++) if (!b.contains(i)) { a.add(i); if (a.size() == need) break; }
    return a;
  }

  int fare(Bus b) {
    Route r = routes.get(b.routeId);
    if (r == null) return 500;
    int base = Math.abs(r.src.length() - r.dst.length()) * 20 + 300;
    return base + (b.seats > 40 ? 30 : 60);
  }

  String choosePoint(String title, List<String> points) {
    head(title);
    for (int i = 0; i < points.size(); i++) p((i + 1) + ". " + points.get(i));
    return points.get(readInt("Choose", 1, points.size()) - 1);
  }

  String readPassword() {
    while (true) {
      String s = read("Password (min 8, alphanumeric with letters+digits)");
      if (s.length() >= 8 && s.matches(".*[A-Za-z].*") && s.matches(".*\\d.*") && s.matches("[A-Za-z0-9]+")) return s;
      p("Password policy failed.");
    }
  }

  String readEmail() {
    while (true) {
      String e = read("Email");
      if (e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) return e;
      p("Invalid email.");
    }
  }

  int readInt(String label, int min, int max) {
    while (true) {
      try {
        int v = Integer.parseInt(read(label));
        if (v < min || v > max) { p("Enter " + min + " to " + max + "."); continue; }
        return v;
      } catch (NumberFormatException ex) { p("Enter valid number."); }
    }
  }

  String read(String label) {
    while (true) {
      System.out.print(label + ": ");
      String x = sc.nextLine().trim();
      if (!x.isBlank()) return x;
      p("Mandatory field.");
    }
  }

  String readOpt(String label) {
    System.out.print(label + ": ");
    return sc.nextLine().trim();
  }

  List<String> readPoints(String label) {
    while (true) {
      List<String> pts = parsePoints(read(label));
      if (pts.size() >= 2) return pts;
      p("Need at least 2 points.");
    }
  }

  List<String> parsePoints(String raw) {
    List<String> out = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String x : raw.split(",")) {
      String c = cap(x.trim());
      if (!c.isBlank() && seen.add(c.toLowerCase(Locale.ROOT))) out.add(c);
    }
    return out;
  }

  String cap(String s) {
    StringBuilder sb = new StringBuilder();
    for (String w : s.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
      if (w.isBlank()) continue;
      sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
    }
    return sb.toString().trim();
  }

  LocalDate parseDate(String s) { try { return LocalDate.parse(s, DF); } catch (DateTimeParseException ex) { return null; } }
  LocalTime parseTime(String s) { try { return LocalTime.parse(s, TF); } catch (DateTimeParseException ex) { return null; } }

  void head(String t) {
    int w = Math.max(45, t.length() + 10);
    String line = rep('=', w);
    p("\n" + line); p(center(t, w)); p(line);
  }

  void table(List<String> hdr, List<List<String>> rows) {
    if (rows == null || rows.isEmpty()) { p("(No data)"); return; }
    int n = hdr.size();
    int[] w = new int[n];
    for (int i = 0; i < n; i++) w[i] = hdr.get(i).length();
    for (List<String> r : rows) for (int i = 0; i < n; i++) {
      String c = i < r.size() ? r.get(i) : "";
      w[i] = Math.min(45, Math.max(w[i], c.length()));
    }
    String sep = sep(w); p(sep); p(row(hdr, w)); p(sep);
    for (List<String> r : rows) {
      List<String> c = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        String x = i < r.size() ? r.get(i) : "";
        if (x.length() > 45) x = x.substring(0, 42) + "...";
        c.add(x);
      }
      p(row(c, w));
    }
    p(sep);
  }
  String sep(int[] w) {
    StringBuilder sb = new StringBuilder("+");
    for (int x : w) sb.append(rep('-', x + 2)).append('+');
    return sb.toString();
  }

  String row(List<String> cols, int[] w) {
    StringBuilder sb = new StringBuilder("|");
    for (int i = 0; i < w.length; i++) {
      String v = i < cols.size() ? cols.get(i) : "";
      sb.append(' ').append(pad(v, w[i])).append(' ').append('|');
    }
    return sb.toString();
  }

  String pad(String v, int w) { return v.length() >= w ? v : v + rep(' ', w - v.length()); }
  String center(String t, int w) {
    if (t.length() >= w) return t;
    int l = (w - t.length()) / 2, r = w - t.length() - l;
    return rep(' ', l) + t + rep(' ', r);
  }
  String rep(char c, int n) {
    if (n <= 0) return "";
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) sb.append(c);
    return sb.toString();
  }

  void pause() { System.out.print("\nPress Enter to continue..."); sc.nextLine(); }
  void p(String s) { System.out.println(s); }

  void seed() { seedOps(); seedRoutes(); seedBuses(); }

  void seedOps() {
    List<Operator> list = List.of(
      new Operator("OP001", "TSRTC", "9123456701"),
      new Operator("OP002", "APSRTC", "9123456702"),
      new Operator("OP003", "KSRTC", "9123456703"),
      new Operator("OP004", "TNSTC", "9123456704"),
      new Operator("OP005", "KeralaRoad", "9123456705"),
      new Operator("OP006", "Orange Travels", "9123456706"),
      new Operator("OP007", "VRL Express", "9123456707"),
      new Operator("OP008", "SRS Travels", "9123456708"),
      new Operator("OP009", "Morning Star", "9123456709"),
      new Operator("OP010", "SouthLink", "9123456710")
    );
    for (Operator o : list) ops.put(o.id, o);
  }

  void seedRoutes() {
    addSeedRoute("Hyderabad", "Chennai", List.of("Miyapur", "Ameerpet", "LB Nagar", "Uppal"), List.of("Koyambedu", "Guindy", "Tambaram", "Sholinganallur"));
    addSeedRoute("Chennai", "Bangalore", List.of("Koyambedu", "Tambaram", "Guindy"), List.of("Madiwala", "Silk Board", "Majestic", "Hebbal"));
    addSeedRoute("Bangalore", "Hyderabad", List.of("Electronic City", "Madiwala", "Majestic", "Yelahanka"), List.of("Ameerpet", "JBS", "Gachibowli", "Kukatpally"));
    addSeedRoute("Rajahmundry", "Hyderabad", List.of("Kotipalli", "Diwancheruvu", "Morampudi", "Kambala Cheruvu"), List.of("LB Nagar", "Dilsukhnagar", "Ameerpet", "Miyapur"));
    addSeedRoute("Kakinada", "Hyderabad", List.of("Bhanugudi", "Madhavapatnam", "Sarpavaram", "Jagannaickpur"), List.of("Uppal", "Malkajgiri", "Ameerpet", "KPHB"));
    addSeedRoute("Rajahmundry", "Chennai", List.of("Morampudi", "Kotipalli", "Gokavaram"), List.of("Koyambedu", "Egmore", "Guindy", "Tambaram"));
    addSeedRoute("Kakinada", "Bangalore", List.of("Bhanugudi", "Sarpavaram", "Madhavapatnam"), List.of("Silk Board", "Madiwala", "Majestic"));

    String[][] pairs = {
      {"Vijayawada", "Hyderabad"}, {"Vijayawada", "Bangalore"}, {"Vijayawada", "Chennai"}, {"Guntur", "Hyderabad"}, {"Guntur", "Chennai"},
      {"Visakhapatnam", "Hyderabad"}, {"Visakhapatnam", "Bangalore"}, {"Visakhapatnam", "Chennai"}, {"Warangal", "Hyderabad"}, {"Warangal", "Bangalore"},
      {"Tirupati", "Hyderabad"}, {"Tirupati", "Bangalore"}, {"Tirupati", "Chennai"}, {"Nellore", "Hyderabad"}, {"Nellore", "Bangalore"},
      {"Nellore", "Chennai"}, {"Mysore", "Chennai"}, {"Mysore", "Hyderabad"}, {"Mysore", "Coimbatore"}, {"Coimbatore", "Bangalore"},
      {"Coimbatore", "Chennai"}, {"Coimbatore", "Hyderabad"}, {"Madurai", "Chennai"}, {"Madurai", "Bangalore"}, {"Madurai", "Coimbatore"},
      {"Salem", "Chennai"}, {"Salem", "Bangalore"}, {"Salem", "Hyderabad"}, {"Trichy", "Chennai"}, {"Trichy", "Bangalore"},
      {"Trichy", "Hyderabad"}, {"Pondicherry", "Bangalore"}, {"Pondicherry", "Hyderabad"}, {"Kochi", "Bangalore"}, {"Kochi", "Chennai"},
      {"Kochi", "Hyderabad"}, {"Trivandrum", "Bangalore"}, {"Trivandrum", "Chennai"}, {"Trivandrum", "Hyderabad"}, {"Mangalore", "Bangalore"},
      {"Mangalore", "Hyderabad"}, {"Hubli", "Bangalore"}, {"Hubli", "Hyderabad"}
    };
    for (String[] pr : pairs) {
      if (routes.size() >= 50) break;
      addSeedRoute(pr[0], pr[1], defaultPoints(pr[0], "Pickup"), defaultPoints(pr[1], "Drop"));
    }
    while (routes.size() < 50) {
      String src = "City" + routes.size() + "A", dst = "City" + routes.size() + "B";
      addSeedRoute(src, dst, defaultPoints(src, "Pickup"), defaultPoints(dst, "Drop"));
    }
  }

  void seedBuses() {
    Random rnd = new Random(42);
    List<String> opIds = new ArrayList<>(ops.keySet());
    List<Integer> routeIds = new ArrayList<>(routes.keySet());
    routeIds.sort(Integer::compareTo);
    String[] types = {"Express", "SuperLux", "Sleeper", "Volvo", "SemiSleeper", "UltraDeluxe"};

    for (int i = 0; i < 75; i++) {
      int rid = routeIds.get(i % routeIds.size());
      String oid = opIds.get(rnd.nextInt(opIds.size()));
      int seats = 30 + rnd.nextInt(21);
      LocalTime dep = LocalTime.of(5 + rnd.nextInt(18), rnd.nextBoolean() ? 0 : 30);
      Bus b = new Bus(nextBusId++, oid, rid, "SI-" + (i + 1) + " " + types[i % types.length], seats, dep);
      buses.put(b.id, b);
    }
  }

  void addSeedRoute(String src, String dst, List<String> pu, List<String> dr) {
    Route r = new Route(nextRouteId++, src, dst, pu, dr);
    routes.put(r.id, r);
  }

  List<String> defaultPoints(String city, String type) {
    return List.of(city + " " + type + " Point 1", city + " " + type + " Point 2", city + " " + type + " Point 3");
  }
}


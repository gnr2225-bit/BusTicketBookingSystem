package bts.app;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bts.model.*;

public final class SeedData {
  private SeedData() {}

  public static void seedAll(AppState state) {
    seedOps(state);
    seedRoutes(state);
    seedBuses(state);
  }

  public static void seedOps(AppState state) {
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
    for (Operator o : list) state.ops.put(o.id, o);
  }

  public static void seedRoutes(AppState state) {
    addSeedRoute(state, "Hyderabad", "Chennai", List.of("Miyapur", "Ameerpet", "LB Nagar", "Uppal"), List.of("Koyambedu", "Guindy", "Tambaram", "Sholinganallur"));
    addSeedRoute(state, "Chennai", "Bangalore", List.of("Koyambedu", "Tambaram", "Guindy"), List.of("Madiwala", "Silk Board", "Majestic", "Hebbal"));
    addSeedRoute(state, "Bangalore", "Hyderabad", List.of("Electronic City", "Madiwala", "Majestic", "Yelahanka"), List.of("Ameerpet", "JBS", "Gachibowli", "Kukatpally"));
    addSeedRoute(state, "Rajahmundry", "Hyderabad", List.of("Kotipalli", "Diwancheruvu", "Morampudi", "Kambala Cheruvu"), List.of("LB Nagar", "Dilsukhnagar", "Ameerpet", "Miyapur"));
    addSeedRoute(state, "Kakinada", "Hyderabad", List.of("Bhanugudi", "Madhavapatnam", "Sarpavaram", "Jagannaickpur"), List.of("Uppal", "Malkajgiri", "Ameerpet", "KPHB"));
    addSeedRoute(state, "Rajahmundry", "Chennai", List.of("Morampudi", "Kotipalli", "Gokavaram"), List.of("Koyambedu", "Egmore", "Guindy", "Tambaram"));
    addSeedRoute(state, "Kakinada", "Bangalore", List.of("Bhanugudi", "Sarpavaram", "Madhavapatnam"), List.of("Silk Board", "Madiwala", "Majestic"));

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
      if (state.routes.size() >= 50) break;
      addSeedRoute(state, pr[0], pr[1], defaultPoints(pr[0], "Pickup"), defaultPoints(pr[1], "Drop"));
    }
    while (state.routes.size() < 50) {
      String src = "City" + state.routes.size() + "A";
      String dst = "City" + state.routes.size() + "B";
      addSeedRoute(state, src, dst, defaultPoints(src, "Pickup"), defaultPoints(dst, "Drop"));
    }
  }

  public static void seedBuses(AppState state) {
    Random rnd = new Random(42);
    List<String> opIds = new ArrayList<>(state.ops.keySet());
    List<Integer> routeIds = new ArrayList<>(state.routes.keySet());
    routeIds.sort(Integer::compareTo);
    String[] types = {"Express", "SuperLux", "Sleeper", "Volvo", "SemiSleeper", "UltraDeluxe"};

    for (int i = 0; i < 75; i++) {
      int rid = routeIds.get(i % routeIds.size());
      String oid = opIds.get(rnd.nextInt(opIds.size()));
      int seats = 30 + rnd.nextInt(21);
      LocalTime dep = LocalTime.of(5 + rnd.nextInt(18), rnd.nextBoolean() ? 0 : 30);
      Bus b = new Bus(state.nextBusId++, oid, rid, "SI-" + (i + 1) + " " + types[i % types.length], seats, dep);
      state.buses.put(b.id, b);
    }
  }

  private static void addSeedRoute(AppState state, String src, String dst, List<String> pu, List<String> dr) {
    Route r = new Route(state.nextRouteId++, src, dst, pu, dr);
    state.routes.put(r.id, r);
  }

  private static List<String> defaultPoints(String city, String type) {
    return List.of(city + " " + type + " Point 1", city + " " + type + " Point 2", city + " " + type + " Point 3");
  }
}

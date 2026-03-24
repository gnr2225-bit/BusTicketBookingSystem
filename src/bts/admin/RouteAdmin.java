package bts.admin;

import java.util.ArrayList;
import java.util.List;

import bts.app.AppState;
import bts.model.Route;
import bts.util.Input;
import bts.util.Output;
import bts.util.TextUtil;

public class RouteAdmin {
  private final AppState state;
  private final Input input;

  public RouteAdmin(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void showRoutes() {
    Output.head("Routes");
    List<List<String>> rows = new ArrayList<>();
    for (Route r : state.routes.values()) {
      rows.add(List.of("" + r.id, r.src, r.dst, "" + r.pickups.size(), "" + r.drops.size()));
    }
    Output.table(List.of("Route", "Source", "Destination", "PickupPts", "DropPts"), rows);
  }

  public void addRoute() {
    Output.head("Add Route");
    String src = TextUtil.cap(input.read("Source City"));
    String dst = TextUtil.cap(input.read("Destination City"));
    List<String> pu = readPoints("Pickup points (comma)");
    List<String> dr = readPoints("Drop points (comma)");
    Route r = new Route(state.nextRouteId++, src, dst, pu, dr);
    state.routes.put(r.id, r);
    if (state.dbReady) {
      try {
        state.store.insertRoute(r);
      } catch (RuntimeException ex) {
        state.routes.remove(r.id);
        state.nextRouteId--;
        Output.print("Failed to save route to database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Added route ID: " + r.id);
  }

  public void updateRoute() {
    showRoutes();
    int id = input.readInt("Route ID", 1, Integer.MAX_VALUE);
    Route r = state.routes.get(id);
    if (r == null) {
      Output.print("Route not found.");
      return;
    }
    String oldSrc = r.src;
    String oldDst = r.dst;
    List<String> oldPickups = new ArrayList<>(r.pickups);
    List<String> oldDrops = new ArrayList<>(r.drops);

    String src = input.readOpt("Source [" + r.src + "]");
    if (!src.isBlank()) r.src = TextUtil.cap(src);
    String dst = input.readOpt("Destination [" + r.dst + "]");
    if (!dst.isBlank()) r.dst = TextUtil.cap(dst);
    String pu = input.readOpt("Pickup comma list (blank skip)");
    if (!pu.isBlank()) r.pickups = TextUtil.parsePoints(pu);
    String dr = input.readOpt("Drop comma list (blank skip)");
    if (!dr.isBlank()) r.drops = TextUtil.parsePoints(dr);

    if (state.dbReady) {
      try {
        state.store.updateRoute(r);
      } catch (RuntimeException ex) {
        r.src = oldSrc;
        r.dst = oldDst;
        r.pickups = oldPickups;
        r.drops = oldDrops;
        Output.print("Failed to update route in database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Route updated.");
  }

  public void deleteRoute() {
    showRoutes();
    int id = input.readInt("Route ID", 1, Integer.MAX_VALUE);
    if (!state.routes.containsKey(id)) {
      Output.print("Route not found.");
      return;
    }
    boolean linked = state.buses.values().stream().anyMatch(b -> b.routeId == id);
    if (linked) {
      Output.print("Delete buses linked to this route first.");
      return;
    }
    Route removed = state.routes.remove(id);
    if (state.dbReady) {
      try {
        state.store.deleteRoute(id);
      } catch (RuntimeException ex) {
        state.routes.put(id, removed);
        Output.print("Failed to delete route from database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Route deleted.");
  }

  private List<String> readPoints(String label) {
    while (true) {
      List<String> pts = TextUtil.parsePoints(input.read(label));
      if (pts.size() >= 2) return pts;
      Output.print("Need at least 2 points.");
    }
  }
}

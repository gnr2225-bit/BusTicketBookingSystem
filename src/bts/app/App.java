package bts.app;

import bts.admin.AdminMenu;
import bts.store.MySqlStore;
import bts.user.UserMenu;
import bts.util.Input;
import bts.util.Output;

public class App {
  private final AppState state = new AppState(new MySqlStore());
  private final Input input = new Input();
  private final AdminMenu adminMenu = new AdminMenu(state, input);
  private final UserMenu userMenu = new UserMenu(state, input);

  public static void main(String[] args) {
    new App().run();
  }

  private void run() {
    initData();
    mainMenu();
  }

  private void initData() {
    if (!state.store.isAvailable()) {
      SeedData.seedAll(state);
      Output.print("MySQL not reachable, running in-memory mode.");
      return;
    }

    state.dbReady = true;
    try {
      state.store.loadAll(state.users, state.ops, state.routes, state.buses, state.bookings);
      state.syncNextIds();
      if (state.ops.isEmpty()) {
        SeedData.seedOps(state);
        persistOperators();
      }
      if (state.routes.isEmpty()) {
        SeedData.seedRoutes(state);
        persistRoutes();
      }
      if (state.buses.isEmpty()) {
        SeedData.seedBuses(state);
        persistBuses();
      }
      if (state.ops.isEmpty() && state.routes.isEmpty() && state.buses.isEmpty()) {
        SeedData.seedAll(state);
        persistSeeds();
        state.syncNextIds();
      }
      state.syncNextIds();
      Output.print("MySQL connected. Data loaded from database.");
    } catch (RuntimeException ex) {
      state.dbReady = false;
      state.users.clear();
      state.ops.clear();
      state.routes.clear();
      state.buses.clear();
      state.bookings.clear();
      SeedData.seedAll(state);
      Output.print("MySQL load failed, running in-memory mode: " + ex.getMessage());
    }
  }

  private void persistSeeds() {
    persistOperators();
    persistRoutes();
    persistBuses();
  }

  private void persistOperators() {
    for (var o : state.ops.values()) state.store.upsertOperator(o);
  }

  private void persistRoutes() {
    for (var r : state.routes.values()) state.store.insertRoute(r);
  }

  private void persistBuses() {
    for (var b : state.buses.values()) state.store.insertBus(b);
  }

  private void mainMenu() {
    while (true) {
      Output.head("South India Bus Ticket System");
      Output.print("1. Admin Mode");
      Output.print("2. User Mode");
      Output.print("3. Exit");
      int c = input.readInt("Choose", 1, 3);
      if (c == 1) adminMenu.loginAndRun();
      else if (c == 2) userMenu.run();
      else {
        Output.print("\nThank you.");
        return;
      }
    }
  }
}

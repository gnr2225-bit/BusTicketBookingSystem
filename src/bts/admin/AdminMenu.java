package bts.admin;

import bts.app.AppState;
import bts.util.Input;
import bts.util.Output;

public class AdminMenu {
  private static final String ADMIN_ID = "admin";
  private static final String ADMIN_PASS = "password";

  private final Input input;
  private final RouteAdmin routeAdmin;
  private final OperatorAdmin operatorAdmin;
  private final BusAdmin busAdmin;
  private final ReportAdmin reportAdmin;
  private final BookingSeedService bookingSeedService;

  public AdminMenu(AppState state, Input input) {
    this.input = input;
    this.routeAdmin = new RouteAdmin(state, input);
    this.operatorAdmin = new OperatorAdmin(state, input);
    this.busAdmin = new BusAdmin(state, input);
    this.reportAdmin = new ReportAdmin(state);
    this.bookingSeedService = new BookingSeedService(state);
  }

  public void loginAndRun() {
    Output.head("Admin Login");
    String id = input.read("Admin ID");
    String pass = input.readPassword("Password");
    if (ADMIN_ID.equals(id) && ADMIN_PASS.equals(pass)) adminMenu();
    else Output.print("Invalid admin credentials.");
  }

  private void adminMenu() {
    while (true) {
      Output.head("Admin Panel");
      Output.print("1. Show Routes");
      Output.print("2. Add Route");
      Output.print("3. Update Route");
      Output.print("4. Delete Route");
      Output.print("5. Show Operators");
      Output.print("6. Add Operator");
      Output.print("7. Show Buses");
      Output.print("8. Add Bus");
      Output.print("9. Update Bus");
      Output.print("10. Delete Bus");
      Output.print("11. Booking Reports");
      Output.print("12. Seed 5 Random Bookings");
      Output.print("13. Back");
      int c = input.readInt("Choose", 1, 13);
      switch (c) {
        case 1 -> routeAdmin.showRoutes();
        case 2 -> routeAdmin.addRoute();
        case 3 -> routeAdmin.updateRoute();
        case 4 -> routeAdmin.deleteRoute();
        case 5 -> operatorAdmin.showOperators();
        case 6 -> operatorAdmin.addOperator();
        case 7 -> busAdmin.showBuses();
        case 8 -> busAdmin.addBus();
        case 9 -> busAdmin.updateBus();
        case 10 -> busAdmin.deleteBus();
        case 11 -> reportAdmin.report();
        case 12 -> {
          int created = bookingSeedService.seedRandomBookings(5);
          Output.print("Random bookings created: " + created);
          reportAdmin.report();
        }
        case 13 -> { return; }
      }
      input.pause();
    }
  }
}

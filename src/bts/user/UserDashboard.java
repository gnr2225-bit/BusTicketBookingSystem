package bts.user;

import bts.app.AppState;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class UserDashboard {
  private final Input input;
  private final ProfileService profileService;
  private final SearchService searchService;
  private final BookingHistoryService bookingHistoryService;
  private final BookingDetailsService bookingDetailsService;
  private final CancellationService cancellationService;

  public UserDashboard(AppState state, Input input) {
    this.input = input;
    this.profileService = new ProfileService(state, input);
    this.searchService = new SearchService(state, input);
    this.bookingHistoryService = new BookingHistoryService(state);
    this.bookingDetailsService = new BookingDetailsService(state, input);
    this.cancellationService = new CancellationService(state, input);
  }

  public void run(User u) {
    while (true) {
      Output.head("Welcome, " + u.name);
      Output.print("1. View/Edit Profile");
      Output.print("2. Search & Book");
      Output.print("3. My Bookings");
      Output.print("4. Booking Details by ID");
      Output.print("5. Cancel Booking");
      Output.print("6. Logout");
      int c = input.readInt("Choose", 1, 6);
      switch (c) {
        case 1 -> profileService.editProfile(u);
        case 2 -> searchService.searchAndBook(u);
        case 3 -> bookingHistoryService.show(u);
        case 4 -> bookingDetailsService.show(u);
        case 5 -> cancellationService.cancel(u);
        case 6 -> { return; }
      }
      input.pause();
    }
  }
}

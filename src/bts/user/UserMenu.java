package bts.user;

import bts.app.AppState;
import bts.util.Input;
import bts.util.Output;

public class UserMenu {
  private final Input input;
  private final AuthService auth;
  private final UserDashboard dashboard;

  public UserMenu(AppState state, Input input) {
    this.input = input;
    this.auth = new AuthService(state, input);
    this.dashboard = new UserDashboard(state, input);
  }

  public void run() {
    while (true) {
      Output.head("User Mode");
      Output.print("1. Register");
      Output.print("2. Login");
      Output.print("3. Back");
      int c = input.readInt("Choose", 1, 3);
      if (c == 1) auth.register();
      else if (c == 2) {
        var u = auth.login();
        if (u != null) dashboard.run(u);
      } else {
        return;
      }
      input.pause();
    }
  }
}

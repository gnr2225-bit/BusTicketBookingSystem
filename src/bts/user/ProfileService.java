package bts.user;

import java.util.List;

import bts.app.AppState;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class ProfileService {
  private final AppState state;
  private final Input input;

  public ProfileService(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void editProfile(User u) {
    Output.head("Profile");
    Output.table(List.of("Field", "Value"), List.of(
      List.of("Phone", u.phone), List.of("Name", u.name), List.of("Address", u.addr), List.of("Email", u.email)
    ));
    Output.print("1. Password  2. Name  3. Address  4. Email  5. Back");
    int c = input.readInt("Choose", 1, 5);
    String oldPass = u.pass;
    String oldName = u.name;
    String oldAddr = u.addr;
    String oldEmail = u.email;
    if (c == 1) u.pass = input.readPassword("Password (min 8, alphanumeric with letters+digits)", true);
    else if (c == 2) u.name = input.read("New Name");
    else if (c == 3) u.addr = input.read("New Address");
    else if (c == 4) u.email = input.readEmail();

    if (state.dbReady && c != 5) {
      try {
        state.store.updateUser(u);
      } catch (RuntimeException ex) {
        u.pass = oldPass;
        u.name = oldName;
        u.addr = oldAddr;
        u.email = oldEmail;
        Output.print("Failed to update profile in database: " + ex.getMessage());
      }
    }
  }
}

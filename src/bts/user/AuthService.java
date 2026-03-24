package bts.user;

import bts.app.AppState;
import bts.model.User;
import bts.util.Input;
import bts.util.Output;

public class AuthService {
  private final AppState state;
  private final Input input;

  public AuthService(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void register() {
    Output.head("Register");
    String phone;
    while (true) {
      phone = input.read("Phone (10 digits)");
      if (!phone.matches("\\d{10}")) {
        Output.print("Phone must be 10 digits.");
        continue;
      }
      if (state.users.containsKey(phone)) {
        Output.print("Phone already registered.");
        continue;
      }
      break;
    }
    String pass = input.readPassword("Password (min 8, alphanumeric with letters+digits)", true);
    String name = input.read("Name");
    String addr = input.read("Address");
    String email = input.readEmail();
    User u = new User(phone, pass, name, addr, email);
    state.users.put(phone, u);
    if (state.dbReady) {
      try {
        state.store.insertUser(u);
      } catch (RuntimeException ex) {
        state.users.remove(phone);
        Output.print("Failed to save user to database: " + ex.getMessage());
        return;
      }
    }
    Output.print("Registration successful.");
  }

  public User login() {
    Output.head("User Login");
    String phone = input.read("Phone");
    String pass = input.readPassword("Password");
    User u = state.users.get(phone);
    if (u == null || !u.pass.equals(pass)) {
      Output.print("Invalid credentials.");
      return null;
    }
    return u;
  }
}

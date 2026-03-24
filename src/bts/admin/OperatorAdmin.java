package bts.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import bts.app.AppState;
import bts.model.Operator;
import bts.util.Input;
import bts.util.Output;

public class OperatorAdmin {
  private final AppState state;
  private final Input input;

  public OperatorAdmin(AppState state, Input input) {
    this.state = state;
    this.input = input;
  }

  public void showOperators() {
    Output.head("Operators");
    List<List<String>> rows = new ArrayList<>();
    for (Operator o : state.ops.values()) rows.add(List.of(o.id, o.name, o.contact));
    Output.table(List.of("ID", "Name", "Contact"), rows);
  }

  public void addOperator() {
    Output.head("Add Operator");
    String id;
    while (true) {
      id = input.read("Operator ID").toUpperCase(Locale.ROOT);
      if (!state.ops.containsKey(id)) break;
      Output.print("Operator ID exists.");
    }
    Operator o = new Operator(id, input.read("Operator Name"), input.read("Contact"));
    if (state.dbReady) {
      try {
        state.store.upsertOperator(o);
      } catch (RuntimeException ex) {
        Output.print("Failed to save operator to database: " + ex.getMessage());
        return;
      }
    }
    state.ops.put(id, o);
    Output.print("Operator added.");
  }
}

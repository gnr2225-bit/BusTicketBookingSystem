package bts.util;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Input {
  private final Scanner sc = new Scanner(System.in);

  public String read(String label) {
    return read(label, false);
  }

  public String read(String label, boolean allowBlank) {
    while (true) {
      System.out.print(label + ": ");
      String x = sc.nextLine().trim();
      if (allowBlank || !x.isBlank()) return x;
      Output.print("Mandatory field.");
    }
  }

  public String readOpt(String label) {
    return read(label, true);
  }

  public int readInt(String label, int min, int max) {
    while (true) {
      try {
        int v = Integer.parseInt(read(label));
        if (v < min || v > max) {
          Output.print("Enter %d to %d.", min, max);
          continue;
        }
        return v;
      } catch (NumberFormatException ex) {
        Output.print("Enter valid number.");
      }
    }
  }

  public String readEmail() {
    while (true) {
      String e = read("Email");
      if (e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) return e;
      Output.print("Invalid email.");
    }
  }

  public String readPassword(String label) {
    return readPassword(label, false);
  }

  public String readPassword(String label, boolean enforcePolicy) {
    while (true) {
      String s = readHidden(label);
      if (!enforcePolicy) return s;
      if (s.length() >= 8 && s.matches(".*[A-Za-z].*") && s.matches(".*\\d.*") && s.matches("[A-Za-z0-9]+")) {
        return s;
      }
      Output.print("Password policy failed.");
    }
  }

  public void pause() {
    System.out.print("\nPress Enter to continue...");
    sc.nextLine();
  }

  private String readHidden(String label) {
    Console console = System.console();
    if (console != null) {
      char[] pw = console.readPassword(label + ": ");
      return pw == null ? "" : new String(pw).trim();
    }
    return readHiddenFallback(label);
  }

  private String readHiddenFallback(String label) {
    System.out.print(label + ": ");
    StringBuilder sb = new StringBuilder();
    InputStream in = System.in;
    try {
      while (true) {
        int ch = in.read();
        if (ch == -1 || ch == '\n' || ch == '\r') {
          if (ch == '\r') {
            if (in.available() > 0) in.read();
          }
          break;
        }
        if (ch == 8 || ch == 127) {
          if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
            System.out.print("\b \b");
          }
          continue;
        }
        sb.append((char) ch);
        System.out.print("*");
      }
    } catch (IOException ex) {
      return "";
    }
    System.out.println();
    return sb.toString().trim();
  }
}

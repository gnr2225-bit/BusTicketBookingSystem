package bts.util;

import java.util.ArrayList;
import java.util.List;

public final class Output {
  private Output() {}

  public static void print(String s) {
    System.out.println(s);
  }

  public static void print(String fmt, Object... args) {
    System.out.println(String.format(fmt, args));
  }

  public static void pause() {
    System.out.print("\nPress Enter to continue...");
    try {
      System.in.read();
      while (System.in.available() > 0) System.in.read();
    } catch (Exception ignored) {}
  }

  public static void head(String t) {
    int w = Math.max(45, t.length() + 10);
    String line = rep('=', w);
    print("\n" + line);
    print(center(t, w));
    print(line);
  }

  public static void table(List<String> hdr, List<List<String>> rows) {
    if (rows == null || rows.isEmpty()) {
      print("(No data)");
      return;
    }
    int n = hdr.size();
    int[] w = new int[n];
    for (int i = 0; i < n; i++) w[i] = hdr.get(i).length();
    for (List<String> r : rows) {
      for (int i = 0; i < n; i++) {
        String c = i < r.size() ? r.get(i) : "";
        w[i] = Math.min(45, Math.max(w[i], c.length()));
      }
    }
    String sep = sep(w);
    print(sep);
    print(row(hdr, w));
    print(sep);
    for (List<String> r : rows) {
      List<String> c = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        String x = i < r.size() ? r.get(i) : "";
        if (x.length() > 45) x = x.substring(0, 42) + "...";
        c.add(x);
      }
      print(row(c, w));
    }
    print(sep);
  }

  private static String sep(int[] w) {
    StringBuilder sb = new StringBuilder("+");
    for (int x : w) sb.append(rep('-', x + 2)).append('+');
    return sb.toString();
  }

  private static String row(List<String> cols, int[] w) {
    StringBuilder sb = new StringBuilder("|");
    for (int i = 0; i < w.length; i++) {
      String v = i < cols.size() ? cols.get(i) : "";
      sb.append(' ').append(pad(v, w[i])).append(' ').append('|');
    }
    return sb.toString();
  }

  private static String pad(String v, int w) {
    return v.length() >= w ? v : v + rep(' ', w - v.length());
  }

  private static String center(String t, int w) {
    if (t.length() >= w) return t;
    int l = (w - t.length()) / 2;
    int r = w - t.length() - l;
    return rep(' ', l) + t + rep(' ', r);
  }

  private static String rep(char c, int n) {
    if (n <= 0) return "";
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) sb.append(c);
    return sb.toString();
  }
}

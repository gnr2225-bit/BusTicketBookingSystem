package bts.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextUtil {
  private TextUtil() {}

  public static String cap(String s) {
    StringBuilder sb = new StringBuilder();
    for (String w : s.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
      if (w.isBlank()) continue;
      sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
    }
    return sb.toString().trim();
  }

  public static List<String> parsePoints(String raw) {
    List<String> out = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String x : raw.split(",")) {
      String c = cap(x.trim());
      if (!c.isBlank() && seen.add(c.toLowerCase(Locale.ROOT))) out.add(c);
    }
    return out;
  }
}

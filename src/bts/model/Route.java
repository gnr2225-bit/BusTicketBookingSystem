package bts.model;

import java.util.ArrayList;
import java.util.List;

public class Route {
  public int id;
  public String src;
  public String dst;
  public List<String> pickups;
  public List<String> drops;

  public Route(int id, String src, String dst, List<String> pickups, List<String> drops) {
    this.id = id;
    this.src = src;
    this.dst = dst;
    this.pickups = new ArrayList<>(pickups);
    this.drops = new ArrayList<>(drops);
  }
}

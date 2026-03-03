import java.util.ArrayList;
import java.util.List;

public class Route {
  int id;
  String src;
  String dst;
  List<String> pickups;
  List<String> drops;

  Route(int id, String src, String dst, List<String> pickups, List<String> drops) {
    this.id = id;
    this.src = src;
    this.dst = dst;
    this.pickups = new ArrayList<>(pickups);
    this.drops = new ArrayList<>(drops);
  }
}

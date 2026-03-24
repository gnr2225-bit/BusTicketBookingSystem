package bts.model;

public class Operator extends Person {
  public String id;

  public Operator(String id, String name, String contact) {
    super(name, contact);
    this.id = id;
  }
}

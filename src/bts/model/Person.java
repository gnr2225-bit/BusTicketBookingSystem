package bts.model;

public abstract class Person {
  public String name;
  public String contact;

  protected Person(String name, String contact) {
    this.name = name;
    this.contact = contact;
  }
}

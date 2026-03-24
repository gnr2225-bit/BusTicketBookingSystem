package bts.model;

public class User extends Person {
  public String phone;
  public String pass;
  public String addr;
  public String email;

  public User(String phone, String pass, String name, String addr, String email) {
    super(name, phone);
    this.phone = phone;
    this.pass = pass;
    this.addr = addr;
    this.email = email;
  }
}

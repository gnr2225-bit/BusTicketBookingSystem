CREATE DATABASE IF NOT EXISTS bus_ticket_system;
USE bus_ticket_system;

CREATE TABLE IF NOT EXISTS users (
  phone VARCHAR(10) PRIMARY KEY,
  pass_hash VARCHAR(255) NOT NULL,
  name VARCHAR(120) NOT NULL,
  addr VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operators (
  op_id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  contact VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS routes (
  route_id INT PRIMARY KEY AUTO_INCREMENT,
  src VARCHAR(120) NOT NULL,
  dst VARCHAR(120) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_route_src_dst (src, dst)
);

CREATE TABLE IF NOT EXISTS route_points (
  point_id INT PRIMARY KEY AUTO_INCREMENT,
  route_id INT NOT NULL,
  point_type ENUM('PICKUP', 'DROP') NOT NULL,
  point_name VARCHAR(120) NOT NULL,
  sequence_no INT NOT NULL,
  FOREIGN KEY (route_id) REFERENCES routes(route_id) ON DELETE CASCADE,
  UNIQUE KEY uq_route_point_order (route_id, point_type, sequence_no)
);

CREATE TABLE IF NOT EXISTS buses (
  bus_id INT PRIMARY KEY AUTO_INCREMENT,
  op_id VARCHAR(10) NOT NULL,
  route_id INT NOT NULL,
  bus_name VARCHAR(120) NOT NULL,
  total_seats INT NOT NULL,
  dep_time TIME NOT NULL,
  FOREIGN KEY (op_id) REFERENCES operators(op_id),
  FOREIGN KEY (route_id) REFERENCES routes(route_id)
);

CREATE TABLE IF NOT EXISTS bookings (
  booking_id INT PRIMARY KEY AUTO_INCREMENT,
  user_phone VARCHAR(10) NOT NULL,
  bus_id INT NOT NULL,
  route_id INT NOT NULL,
  journey_date DATE NOT NULL,
  pickup_point VARCHAR(120) NOT NULL,
  drop_point VARCHAR(120) NOT NULL,
  total_fare INT NOT NULL,
  status ENUM('BOOKED', 'CANCELLED') NOT NULL DEFAULT 'BOOKED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_phone) REFERENCES users(phone),
  FOREIGN KEY (bus_id) REFERENCES buses(bus_id),
  FOREIGN KEY (route_id) REFERENCES routes(route_id)
);

CREATE TABLE IF NOT EXISTS booking_seats (
  booking_id INT NOT NULL,
  seat_no INT NOT NULL,
  PRIMARY KEY (booking_id, seat_no),
  FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

ALTER TABLE routes AUTO_INCREMENT = 1;
ALTER TABLE buses AUTO_INCREMENT = 1001;
ALTER TABLE bookings AUTO_INCREMENT = 50001;

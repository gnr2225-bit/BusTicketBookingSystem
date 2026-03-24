# Bus Ticket System (Java CLI)

Terminal-based Bus Ticket System for Windows.

## Requirements
- Java 17+ (tested with Java 25)
- MySQL Server 8.x (for schema scripts in `db/`)

## Compile
```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName })
```

## Run
```powershell
java -cp "out;lib\mysql-connector-j-8.4.0.jar" BusTicketSystem
```

## MySQL Setup
1. Install MySQL Server and ensure `mysql` is available in PATH.
2. Run schema setup:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1 -RootPassword <your-root-password>
```
3. The script creates:
- DB: `bus_ticket_system`
- DB user: `admin`
- Password: `password123`
- Tables + keys + auto-increment IDs

For this project, local helper scripts are included:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-mysql-local.ps1
powershell -ExecutionPolicy Bypass -File scripts\stop-mysql-local.ps1
```

## JDBC Configuration
- Default URL: `jdbc:mysql://localhost:3306/bus_ticket_system?serverTimezone=Asia/Kolkata`
- Default DB user/password: `admin` / `password123`
- Optional env overrides:
  - `BTS_DB_URL`
  - `BTS_DB_USER`
  - `BTS_DB_PASS`

## Login Modes (App)
- Admin mode credentials:
  - ID: `admin`
  - Password: `password`
- User mode:
  - Register with phone (unique), password (min 8 alphanumeric with letters+digits), name, address, email

## Included Data
- 50 seeded South India routes
- 75 seeded buses mapped across these routes
- Multi pickup/drop points on key routes including Hyderabad, Chennai, Bangalore, Kakinada, Rajahmundry

## Core Features
- Admin:
  - Show/Add/Update/Delete routes
  - Show/Add operators
  - Show/Add/Update/Delete buses
  - Booking reports
- User:
  - Register/Login
  - Edit profile
  - Search route by source-destination pair
  - Select bus, date, pickup/drop points, seats
  - Book and cancel tickets
  - View booking history

## Notes
- App runtime data is currently in-memory (resets when app restarts)
- SQL scripts are provided for relational schema creation and DB provisioning

## dir /s /b src\*.java > sources.txt & javac -d out @sources.txt & del sources.txt & java -cp "out;lib\mysql-connector-j-8.4.0.jar" BusTicketSystem


# Hospital / Patient Management System

A terminal-based hospital management application built with core Java. It manages
patient records, doctors, and appointment scheduling, backed by a persistent
SQLite database accessed via JDBC.

## Features Implemented

- **Patient management**: add, view, update, delete, and search patients by name.
- **Doctor management**: add, view, update, and delete doctors with specializations.
- **Patient-doctor assignment**: each patient can be assigned a primary doctor (stored as a foreign key), and the assignment can be changed or removed at any time.
- **Appointment scheduling**: schedule, reschedule, cancel, and complete appointments.
- **Double-booking prevention**: a doctor cannot be booked twice for overlapping time slots — enforced with a custom `DoubleBookingException`.
- **Reports**:
  - A doctor's daily schedule for a given date.
  - A patient's full visit history.
  - All patients assigned to a doctor.
- **Linked output**: appointment listings show the patient, the appointment's doctor, and the patient's assigned doctor by name; patient listings show the assigned doctor by name.
- **Patient profile**: one menu option shows a patient's details together with all of their appointments.
- **Data persistence**: all data is stored in a SQLite database file (`hospital.db`) and survives across program runs.
- **Input validation**: rejects non-numeric input where a number is expected, rejects empty required fields, and re-prompts instead of crashing.
- **Graceful error handling**: `SQLException`, `RecordNotFoundException`, and `DoubleBookingException` are all caught and reported to the user without crashing the app.

## Technologies / Libraries Used

- **Java**: JDK 17+ (core Java only — no frameworks)
- **Build tool**: Maven
- **Database**: SQLite
- **JDBC driver**: `org.xerial:sqlite-jdbc` (version 3.46.1.3)

## Project Structure

```
hospital-management-system/
├── pom.xml
├── schema.sql
├── src/main/java/com/hospital/
│   ├── model/        # Person (abstract), Patient, Doctor, Appointment
│   ├── dao/           # DatabaseConnection, PatientDAO, DoctorDAO, AppointmentDAO
│   ├── service/       # HospitalService (business logic + in-memory caches)
│   ├── exception/     # RecordNotFoundException, DoubleBookingException
│   ├── util/          # InputValidator
│   └── main/          # Main (menu-driven console entry point)
└── src/main/resources/
    ├── config.properties.example
    └── schema.sql
```

### OOP design notes
- `Person` is an **abstract class** extended by `Patient` and `Doctor`.
- `describe()` is overridden differently by each subclass — **polymorphism**.
- All model fields are **private** with public getters/setters — **encapsulation**.
- `Appointment implements Comparable<Appointment>` for natural time-based ordering.

### Collections Framework usage
- `ArrayList` — ordered lists returned from DAO/service methods (e.g. `listPatients()`).
- `HashMap<Integer, Patient>` / `HashMap<Integer, Doctor>` — ID-keyed in-memory caches in `HospitalService` for fast lookups.
- `Collections.sort()` / `Comparable` — appointments sorted by date/time for reports.

## Database Setup

The app uses SQLite, so there is no separate database server to install — the
database is a single file created automatically on first run.

Schema (`schema.sql`, also bundled under `src/main/resources/`):

```sql
CREATE TABLE IF NOT EXISTS doctors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    specialization TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS patients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    date_of_birth TEXT NOT NULL,
    address TEXT,
    diagnosis TEXT,
    primary_doctor_id INTEGER,
    FOREIGN KEY (primary_doctor_id) REFERENCES doctors(id)
);

CREATE TABLE IF NOT EXISTS appointments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL,
    doctor_id INTEGER NOT NULL,
    appointment_time TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    notes TEXT,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);
```

The app runs this script automatically at startup (`CREATE TABLE IF NOT EXISTS`,
so it's safe to run every time). If you have a `hospital.db` from an earlier version, the app
adds the missing `patients.primary_doctor_id` column automatically.

### Relationships

- `patients.primary_doctor_id` -> `doctors.id`: a patient has at most one assigned doctor; a doctor can have many assigned patients (one-to-many).
- `appointments.patient_id` -> `patients.id` and `appointments.doctor_id` -> `doctors.id`: the appointments table links a specific patient and doctor at a specific time. Because a patient can see several doctors and a doctor several patients, appointments resolve that many-to-many relationship.
- Deleting a doctor clears the assignment of every patient assigned to them.

### Configuration (no hard-coded credentials)

The database file path is read from `src/main/resources/config.properties`,
which is **excluded from git** via `.gitignore`. A template is committed as
`config.properties.example`:

```properties
db.file=hospital.db
```

To set up locally:
```bash
cp src/main/resources/config.properties.example src/main/resources/config.properties
```
(SQLite has no username/password, but this pattern is in place so the same
approach — an ignored config file — extends cleanly to MySQL/PostgreSQL if
you swap the driver later.)

## Setup and Run Instructions

**Prerequisites**: JDK 17+, Maven 3.6+

1. Clone the repository and enter the folder:
   ```bash
   git clone <your-repo-url>
   cd hospital-management-system
   ```
2. Copy the config template:
   ```bash
   cp src/main/resources/config.properties.example src/main/resources/config.properties
   ```
3. Build the project (downloads the SQLite JDBC driver and packages a runnable jar):
   ```bash
   mvn clean package
   ```
4. Run the application:
   ```bash
   java -jar target/hospital-management-system-jar-with-dependencies.jar
   ```

   Alternatively, during development, run directly with Maven:
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.hospital.main.Main"
   ```

On first run, `hospital.db` is created automatically in the project root and
the schema is applied.

## Screenshots

_Add 2–3 screenshots of the running terminal application here before submitting,
e.g.:_

```
![Main menu](screenshots/main-menu.png)
![Adding a patient](screenshots/add-patient.png)
![Doctor daily schedule report](screenshots/doctor-report.png)
```

## Known Limitations

- Appointment overlap checking treats every appointment as a fixed 30-minute
  slot rather than supporting variable-length appointments.
- No authentication / login mode (single-user terminal app, per assignment scope).
- Search is limited to patients by name; doctors are not currently searchable
  by specialization (only listable).
- Deleting a patient or doctor does not remove their existing appointments.

## Stretch Goals Not Implemented

- Billing module.
- Prescription/notes log beyond the free-text `notes` field on an appointment.
- Priority queue for walk-in/emergency patients.

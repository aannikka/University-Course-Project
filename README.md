# Tennis Tournament Management System 

A desktop application designed to automate the planning of tennis tournaments, schedule matches, and manage participants (players and referees). 

The project is built with a strong focus on clean architecture, data reliability, and comprehensive unit test coverage.

## Tech Stack & Architecture
* **Language:** Java
* **UI:** Swing 
* **Architecture:** MVC (Model-View-Controller) + GRASP Patterns
* **Data Access:** DAO (Data Access Object) Pattern + Singleton
* **Database:** PostgreSQL
* **Database Optimization:** HikariCP connection pool
* **Testing:** JUnit 5, Mockito, Reflection API

## Engineering Decisions
1. **Logic Isolation:** Strict separation of UI, business logic, and SQL queries following GRASP principles (Low Coupling, Information Expert, Creator, Controller).
2. **Database Security:** Designed in 3rd Normal Form (3NF). Implements transactions, foreign keys, and cascading deletes. Protected against SQL injections.
3. **UI/UX:** "Foolproof" design and strict field validation to prevent invalid data entry.
4. **Professional Testing:** Unit tests for controllers are implemented using white-box (Basis Path Testing) and gray-box (State Machine Testing) methodologies, with complete isolation (mocking) of the database layer.

## 👥 Role-Based Access
The system supports three distinct user roles:
* **Administrator:** Manages locations, courts, and system users.
* **Planner:** Creates tournaments, schedules matches, and manages the timetable.
* **Registrar:** Registers new players and referees for ongoing tournaments.


## How to Run (Getting Started)

### Prerequisites
Before you begin, ensure you have the following installed on your machine:
* **Java Development Kit (JDK)** 17 or higher
* **PostgreSQL** (version 12 or higher)
* **Apache Maven**

### Installation & Setup

**1. Clone the repository:**
`git clone https://github.com/aannikka/University-Course-Project.git`
`cd University-Course-Project`

**2. Database Setup:**
* Create a new PostgreSQL database (e.g., tennis_db).
* Execute the SQL script (if provided in the repository) to initialize the tables and relations.

**3. Configure Database Connection:**
* Navigate to the src/main/resources/ directory.
* Find the file named db.properties.example and rename it (or copy it) to db.properties.
* Open db.properties and update it with your actual PostgreSQL credentials:

`db.url=jdbc:postgresql://localhost:5432/tennis_db`
`db.user=your_postgres_username`
`db.password=your_postgres_password`

**4. Build and Run:**
* You can open the project in your favorite IDE (like IntelliJ IDEA or Eclipse) and run the main class (App.java).
* Alternatively, you can compile and run the application directly from the terminal using Maven:
`mvn clean compile exec:java -Dexec.mainClass="App"`

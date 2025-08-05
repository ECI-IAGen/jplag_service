# Taller3_AREP: MicroSpring Framework

This project is a micro-framework in Java inspired by Spring Boot that enables you to build a minimal web server. The server:

- **Automatically scans** the classpath for classes annotated with `@RestController`.
- **Maps endpoints** using the `@GetMapping` annotation and resolves URL parameters with `@RequestParam`.
- **Serves static resources** (such as HTML and PNG files) from either the file system or the classpath (from `resources/static`).
- Includes a front-end example that consumes the `/greeting` endpoint.
- Implements caching in the `GreetingController` to store and reuse greetings.

---

## Getting Started

### Prerequisites

Before you begin, ensure that you have the following installed:

- **Java JDK 17** (or higher)  
  Download it from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or use OpenJDK.
- **Apache Maven**  
  Download it from [Maven Official](https://maven.apache.org/download.cgi) and follow the installation instructions.
- **Git** (optional, for cloning the repository)

### Installation

1. **Clone the repository:**

  ```bash
  https://github.com/murcia0421/ArepLab3.git
  ```

2. **Navigate to the project directory:**
   
  ```bash
  cd Taller3_AREP
  ```

3. Compile and package the project with Maven:
   
  ```bash
  mvn clean package
  ```
4. Run the server:

  ```bash
  java -jar target/AREP3-1.0-SNAPSHOT.jar
  ```

5. Check the console for messages such as:

  ```bash
  Registered: /greeting -> greeting
  Server running on port 8080...
   ```
## Project Architecture

The project follows the standard Maven structure. Below is an example of the directory structure using the tree command:

![image](https://github.com/user-attachments/assets/076d5b4a-5520-4433-9334-c9780990a596)

## Screenshots

### Endpoint Functionality (Point 2)

![image](https://github.com/user-attachments/assets/e2d82e5d-991c-48bc-ba6c-67818deeb20a)
![image](https://github.com/user-attachments/assets/6731f1fd-83dd-4940-ba8e-2c5b11159c6e)



### Automatic Component Scanning (Point 3)

![image](https://github.com/user-attachments/assets/8976abf0-2e77-4ca6-9203-40bfbbce2106)


### Parameter Mapping with @RequestParam (Point 4)

![image](https://github.com/user-attachments/assets/cb6d93f1-4662-4e59-914d-44dfb5a68ca9)




## Running Tests

The project includes unit tests written with JUnit 5. To run the tests, use the following command:

```bash
  mvn test
  ```
![image](https://github.com/user-attachments/assets/fa049c4b-1b37-4dab-b2a0-98fe7867ef7e)
![image](https://github.com/user-attachments/assets/6157f48b-b022-4a98-958f-ea9296671839)

## Technologies Used

- Java 17
- Apache Maven
- JUnit 5 (for unit testing)
- Reflections (for automatic classpath scanning)

## Author

- Juan Daniel Murcia
- GitHub: murcia0421





   

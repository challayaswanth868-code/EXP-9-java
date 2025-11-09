// File: App.java
// Demonstrates Spring Dependency Injection, Hibernate CRUD, and Transaction Management
// NOTE: This is a combined conceptual demonstration. Actual execution requires
// proper Spring + Hibernate dependencies and MySQL configuration.

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import java.util.*;

// =============================
// PART A: SPRING DEPENDENCY INJECTION
// =============================

// 1. Dependent class
class Course {
    private String name;

    public Course(String name) {
        this.name = name;
    }

    public void displayCourse() {
        System.out.println("Enrolled in course: " + name);
    }
}

// 2. Dependent class that will be injected
class Student {
    private Course course;
    private String studentName;

    public Student(Course course) {
        this.course = course;
        this.studentName = "John Doe";
    }

    public void showInfo() {
        System.out.println("Student Name: " + studentName);
        course.displayCourse();
    }
}

// 3. Spring Configuration (Java-based)
@Configuration
class SpringConfig {
    @Bean
    public Course course() {
        return new Course("Spring & Hibernate Integration");
    }

    @Bean
    public Student student() {
        return new Student(course());
    }
}

// =============================
// PART B: HIBERNATE CRUD OPERATIONS
// =============================

// 1. Hibernate Entity
@Entity
@Table(name = "students")
class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "course")
    private String course;

    public StudentEntity() {}
    public StudentEntity(String name, String course) {
        this.name = name;
        this.course = course;
    }

    // Getters/Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getCourse() { return course; }
    public void setName(String name) { this.name = name; }
    public void setCourse(String course) { this.course = course; }

    @Override
    public String toString() {
        return "StudentEntity [ID=" + id + ", Name=" + name + ", Course=" + course + "]";
    }
}

// 2. Hibernate Utility for SessionFactory
class HibernateUtil {
    private static final SessionFactory sessionFactory;
    static {
        try {
            sessionFactory = new Configuration()
                    .configure("hibernate.cfg.xml") // requires proper XML in classpath
                    .addAnnotatedClass(StudentEntity.class)
                    .buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}

// 3. DAO for CRUD
class StudentDAO {
    public void createStudent(StudentEntity student) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        session.save(student);
        session.getTransaction().commit();
        session.close();
        System.out.println("Student created: " + student);
    }

    public StudentEntity readStudent(int id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        StudentEntity student = session.get(StudentEntity.class, id);
        session.close();
        return student;
    }

    public void updateStudent(int id, String newCourse) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        StudentEntity student = session.get(StudentEntity.class, id);
        if (student != null) {
            student.setCourse(newCourse);
            session.update(student);
            session.getTransaction().commit();
            System.out.println("Updated student: " + student);
        } else {
            System.out.println("Student not found!");
        }
        session.close();
    }

    public void deleteStudent(int id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        StudentEntity student = session.get(StudentEntity.class, id);
        if (student != null) {
            session.delete(student);
            session.getTransaction().commit();
            System.out.println("Deleted student ID: " + id);
        } else {
            System.out.println("Student not found!");
        }
        session.close();
    }
}

// =============================
// PART C: SPRING + HIBERNATE TRANSACTION MANAGEMENT
// =============================

// 1. Entities
@Entity
@Table(name = "accounts")
class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private double balance;

    public Account() {}
    public Account(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    // Getters/Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return "Account [ID=" + id + ", Name=" + name + ", Balance=" + balance + "]";
    }
}

// 2. DAO for Account
@Repository
class AccountDAO {
    @PersistenceContext
    private EntityManager entityManager;

    public Account findById(int id) {
        return entityManager.find(Account.class, id);
    }

    public void update(Account account) {
        entityManager.merge(account);
    }
}

// 3. Service Layer with Transaction
@Service
@Transactional
class BankingService {
    @Autowired
    private AccountDAO accountDAO;

    public void transferMoney(int fromId, int toId, double amount) {
        Account from = accountDAO.findById(fromId);
        Account to = accountDAO.findById(toId);

        if (from.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance!");
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        accountDAO.update(from);
        accountDAO.update(to);

        System.out.println("Transfer successful: " + amount + " from " + from.getName() + " to " + to.getName());
    }
}

// 4. App Config for Hibernate + Transaction
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "com.example")
class AppConfig {
    // DataSource, SessionFactory, TransactionManager beans normally go here.
}

// =============================
// MAIN EXECUTION
// =============================
public class App {
    public static void main(String[] args) {
        System.out.println("=== Part A: Spring Dependency Injection Demo ===");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfig.class);
        Student s = ctx.getBean(Student.class);
        s.showInfo();
        ctx.close();

        System.out.println("\n=== Part B: Hibernate CRUD Demo ===");
        StudentDAO dao = new StudentDAO();
        StudentEntity st = new StudentEntity("Alice", "Java");
        dao.createStudent(st);

        StudentEntity read = dao.readStudent(1);
        System.out.println("Fetched: " + read);

        dao.updateStudent(1, "Spring Boot");
        dao.deleteStudent(1);

        System.out.println("\n=== Part C: Transaction Management (Spring + Hibernate) ===");
        System.out.println("(Conceptual only – requires Spring container + DB setup)");
        System.out.println("Transactions ensure atomic fund transfers between accounts.");
    }
}

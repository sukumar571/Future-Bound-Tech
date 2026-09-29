package com.futureboundtech;

import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.CourseModule;
import com.futureboundtech.entity.Lesson;
import com.futureboundtech.enums.LessonType;
import com.futureboundtech.repository.CourseModuleRepository;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lays down a rich, editable starter syllabus (modules + lessons) for the catalog
 * courses. The pass is ADDITIVE and idempotent: modules are matched by title and
 * lessons by title-within-module, so a restart only fills in what is missing. Admin
 * edits, reordering and deletions of existing items are never overwritten, and any
 * newly added catalog course is covered on the next start.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeders.enabled", havingValue = "true", matchIfMissing = true)
public class SyllabusDataSeeder {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    /** A lesson to ensure exists. */
    private record L(String title, LessonType type, int minutes, String description, String notes) {}

    /** A module to ensure exists, with its lessons. */
    private record M(String title, String description, List<L> lessons) {}

    @Bean
    @Order(2)
    public CommandLineRunner initSyllabus() {
        return args -> {
            apply("java-full-stack-development", javaSyllabus());
            apply("python-full-stack-development", pythonSyllabus());
            apply("web-development", webSyllabus());
            apply("aws-cloud-computing", awsSyllabus());
            apply("artificial-intelligence", aiSyllabus());
        };
    }

    // =====================================================================
    // Idempotent, additive upsert
    // =====================================================================
    private void apply(String slug, List<M> modules) {
        courseRepository.findBySlugAndDeletedFalse(slug).ifPresent(course -> {
            Map<String, CourseModule> byTitle = new LinkedHashMap<>();
            int moduleOrder = 0;
            for (CourseModule m : moduleRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId())) {
                byTitle.put(key(m.getTitle()), m);
                moduleOrder = Math.max(moduleOrder, m.getOrderIndex() == null ? 0 : m.getOrderIndex());
            }
            for (M spec : modules) {
                CourseModule module = byTitle.get(key(spec.title()));
                if (module == null) {
                    module = moduleRepository.save(CourseModule.builder()
                            .title(spec.title())
                            .description(spec.description())
                            .orderIndex(++moduleOrder)
                            .course(course)
                            .build());
                    byTitle.put(key(spec.title()), module);
                }
                addLessons(module, spec.lessons());
            }
        });
    }

    private void addLessons(CourseModule module, List<L> lessons) {
        Set<String> have = new HashSet<>();
        int order = 0;
        for (Lesson l : lessonRepository.findByModule_Id(module.getId())) {
            have.add(key(l.getTitle()));
            order = Math.max(order, l.getOrderIndex() == null ? 0 : l.getOrderIndex());
        }
        for (L spec : lessons) {
            if (have.contains(key(spec.title()))) {
                continue;
            }
            lessonRepository.save(Lesson.builder()
                    .title(spec.title())
                    .lessonType(spec.type())
                    .durationMinutes(spec.minutes())
                    .published(spec.type() != LessonType.VIDEO) // recordings go live once the trainer uploads the URL
                    .description(spec.description())
                    .notes(spec.notes())
                    .module(module)
                    .orderIndex(++order)
                    .build());
            have.add(key(spec.title()));
        }
    }

    private static String key(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static L lesson(String t, LessonType ty, int mins, String d, String n) {
        return new L(t, ty, mins, d, n);
    }

    @SafeVarargs
    private static M module(String t, String d, L... lessons) {
        return new M(t, d, List.of(lessons));
    }

    // =====================================================================
    // Java Full Stack
    // =====================================================================
    private List<M> javaSyllabus() {
        return List.of(
            module("Java Fundamentals", "Syntax, types and the JVM: the building blocks every Java program is made of.",
                lesson("JDK, JVM and JRE", LessonType.READING, 25, "How Java code compiles to bytecode and runs on the JVM, and what the JDK provides.", "Install JDK 17 and set JAVA_HOME.\nCompile and run Hello World from the terminal."),
                lesson("Variables, Data Types and Operators", LessonType.READING, 40, "Primitive vs reference types, literals, type casting and the operator set.", "Write a program that converts units using operators.\nPredict output of casting examples."),
                lesson("Control Flow: if, loops, switch", LessonType.CODING, 60, "Branching and iteration — the backbone of any algorithm.", "Solve: print a multiplication table.\nSolve: find the largest of N numbers."),
                lesson("Arrays and Strings", LessonType.CODING, 55, "Declaring, traversing and manipulating arrays and the String API.", "Reverse an array in place.\nCount vowels in a string."),
                lesson("Java Fundamentals Quiz", LessonType.QUIZ, 15, "Self-check on types, operators and control flow.", null)),
            module("OOP", "Objects, classes and the four pillars that structure large Java programs.",
                lesson("Classes, Objects and Constructors", LessonType.READING, 35, "Modelling real entities with fields, methods and constructors.", "Model a Book class with price and stock.\nAdd overloaded constructors."),
                lesson("Encapsulation and Access Modifiers", LessonType.READING, 30, "Getters/setters, information hiding and the four access levels.", "Refactor the Book class to be encapsulated."),
                lesson("Inheritance and Polymorphism", LessonType.CODING, 60, "extends, method overriding, super and dynamic dispatch.", "Build a Shape hierarchy with an area() method."),
                lesson("Abstraction and Interfaces", LessonType.CODING, 55, "Abstract classes vs interfaces, default methods and multiple inheritance of type.", "Define a Payable interface and implement it for two classes."),
                lesson("OOP Design Quiz", LessonType.QUIZ, 15, "Pillars, coupling and cohesion.", null)),
            module("Advanced Java", "The collections framework, exceptions, generics and modern language features.",
                lesson("Exception Handling", LessonType.READING, 35, "checked vs unchecked, try/catch/finally, custom exceptions and try-with-resources.", "Wrap file reading in robust exception handling."),
                lesson("Collections Framework", LessonType.CODING, 65, "List, Set, Map, Queue — implementations, complexity and when to use each.", "Dedupe a list and count word frequency with a Map."),
                lesson("Generics", LessonType.READING, 35, "Type-safe containers, bounded type parameters and wildcards.", "Write a generic Stack<T>."),
                lesson("Lambdas and the Stream API", LessonType.CODING, 70, "Functional interfaces, streams, filter/map/reduce and collectors.", "Use streams to summarise a list of employees by department."),
                lesson("Date, Time and Optional", LessonType.READING, 30, "java.time, the Optional container and avoiding nulls.", "Model a booking with LocalDate and validate overlaps.")),
            module("SQL and MySQL", "Relational modelling and querying — the persistence layer behind every app.",
                lesson("Relational Modelling and Keys", LessonType.READING, 35, "Tables, primary/foreign keys, normalisation and ER diagrams.", "Model a students–courses–enrollments schema."),
                lesson("CRUD and Filtering", LessonType.CODING, 55, "INSERT, SELECT, UPDATE, DELETE with WHERE, ORDER BY and LIMIT.", "Write queries to list active students alphabetically."),
                lesson("Joins and Aggregation", LessonType.CODING, 60, "INNER/LEFT joins, GROUP BY, HAVING and aggregate functions.", "Report enrolment counts per course."),
                lesson("Indexes, Transactions and JDBC", LessonType.READING, 40, "How indexes speed queries, ACID transactions and connecting from Java via JDBC.", "Add an index and compare EXPLAIN output."),
                lesson("SQL Quiz", LessonType.QUIZ, 15, "Joins, grouping and keys.", null)),
            module("HTML/CSS/Bootstrap/JavaScript", "The frontend fundamentals needed to consume your own backend APIs.",
                lesson("Semantic HTML and Forms", LessonType.READING, 35, "Document structure, accessibility and form inputs/validation.", "Build a registration form with validation."),
                lesson("CSS Box Model and Layout", LessonType.CODING, 50, "Selectors, box model, flex and grid.", "Recreate a card layout using flexbox."),
                lesson("Responsive UI with Bootstrap", LessonType.CODING, 45, "Grid, components and utility classes for mobile-first pages.", "Add a responsive navbar to your form page."),
                lesson("JavaScript and the DOM", LessonType.CODING, 55, "Variables, functions, events and DOM manipulation.", "Toggle a theme with JavaScript."),
                lesson("Fetch API and JSON", LessonType.CODING, 45, "Calling REST endpoints from the browser and rendering results.", "Fetch course data and render it as cards.")),
            module("Spring Boot", "Building production REST APIs and web apps with the Spring ecosystem.",
                lesson("IoC Container and Dependency Injection", LessonType.READING, 40, "Beans, contexts, @Component, constructors and injection.", "Wire a service into a controller via DI."),
                lesson("REST Controllers and Request Mapping", LessonType.CODING, 60, "@RestController, mappings, path/request params and status codes.", "Expose a CRUD REST API for a resource."),
                lesson("Spring Data JPA and Hibernate", LessonType.CODING, 65, "Entities, repositories, relationships and derived queries.", "Map a one-to-many relationship and query it."),
                lesson("Validation, Exceptions and DTOs", LessonType.READING, 40, "Bean validation, @ControllerAdvice and mapping entities to DTOs.", "Add request validation and a global error handler."),
                lesson("Security and Configuration", LessonType.READING, 45, "Filters, authentication, roles, profiles and externalised config.", "Protect an endpoint with role-based access."),
                lesson("Spring Boot Quiz", LessonType.QUIZ, 15, "DI, web layer and JPA.", null)),
            module("Frontend Integration", "Connecting a real UI to your Spring Boot backend end to end.",
                lesson("Consuming Your API from the UI", LessonType.CODING, 50, "Auth headers, pagination and error handling in the client.", "Load a paginated list from your API."),
                lesson("State and Component Patterns", LessonType.READING, 40, "Managing client state and reusable UI components.", "Extract a reusable table component."),
                lesson("Building a Full-Stack Feature", LessonType.PROJECT, 120, "Design the API, build the UI and wire them together for one feature.", "Ship an end-to-end search + detail flow."),
                lesson("Deployment Basics", LessonType.READING, 30, "Packaging a Spring Boot jar and serving the static frontend.", "Run the packaged app locally.")),
            module("Data Structures & Algorithms", "The problem-solving core tested in technical interviews.",
                lesson("Complexity and Arrays/Strings", LessonType.CODING, 55, "Big-O, two pointers, sliding window and string techniques.", "Solve: longest substring without repeats."),
                lesson("Linked Lists, Stacks and Queues", LessonType.CODING, 60, "Implementations and classic manipulation problems.", "Reverse a linked list; implement a queue with stacks."),
                lesson("Hashing, Sorting and Searching", LessonType.CODING, 60, "HashMaps, comparison sorts, binary search and problem patterns.", "Two-sum, merge intervals, kth largest."),
                lesson("Recursion and Trees", LessonType.CODING, 65, "Recursion, backtracking and binary tree traversals.", "Inorder traversal; max depth of a tree.")),
            module("Projects and Placement", "Consolidate the stack in a capstone and prepare to be hired.",
                lesson("Capstone Project Build", LessonType.PROJECT, 180, "Plan, build and review a full-stack capstone with your trainer.", "Scope the project with your trainer.\nCommit in stages."),
                lesson("Code Review and Best Practices", LessonType.ASSIGNMENT, 60, "Clean code, git hygiene and reviewing pull requests.", "Submit a PR and review a peer's."),
                lesson("Resume and GitHub Polish", LessonType.READING, 30, "Turning your projects into a recruiter-ready profile.", "Publish the capstone with a README."),
                lesson("Interview Sprint", LessonType.QUIZ, 45, "Mixed technical + HR questions drawn from the whole program.", null)),
            module("Testing & DevOps Basics", "Ship with confidence: automated tests and a basic delivery pipeline.",
                lesson("Unit Testing with JUnit", LessonType.CODING, 50, "Assertions, test doubles and structuring a test suite.", "Write tests for a service with mocks."),
                lesson("Integration Testing", LessonType.READING, 35, "Testing the web and persistence layers with Spring test support.", "Add a controller test with MockMvc."),
                lesson("Git, CI and Docker Overview", LessonType.READING, 40, "Version control flow, a simple CI pipeline and containerising the app.", "Containerise the capstone with a Dockerfile."))
        );
    }

    // =====================================================================
    // Python Full Stack
    // =====================================================================
    private List<M> pythonSyllabus() {
        return List.of(
            module("Python", "The language fundamentals that everything else builds on.",
                lesson("Syntax, Types and Control Flow", LessonType.READING, 40, "Indentation, dynamic types, loops and conditionals.", "Write a number-guessing loop."),
                lesson("Functions and Modules", LessonType.CODING, 45, "def, args/kwargs, scope, imports and packages.", "Split a script into reusable modules."),
                lesson("Lists, Dicts, Tuples and Sets", LessonType.CODING, 55, "Core containers, comprehensions and when to use each.", "Aggregate word counts with a dict."),
                lesson("Python Quiz", LessonType.QUIZ, 15, "Types and containers.", null)),
            module("OOP", "Structuring Python programs with classes and idioms.",
                lesson("Classes, Objects and dunder Methods", LessonType.READING, 45, "__init__, self, magic methods and operator overloading.", "Model a Vector with __add__."),
                lesson("Inheritance, Encapsulation, Polymorphism", LessonType.CODING, 55, "Super, MRO, properties and abstract base classes.", "Build an Animal hierarchy with an abstract speak()."),
                lesson("Decorators, Generators and Context Managers", LessonType.CODING, 60, "Reusable behaviour, lazy sequences and with-blocks.", "Write a timing decorator and a file context manager.")),
            module("SQL", "Persisting Python applications to a relational database.",
                lesson("Relational Modelling", LessonType.READING, 35, "Tables, keys, normalisation and relationships.", "Design a blog schema."),
                lesson("Querying with SQL and an ORM", LessonType.CODING, 60, "Raw SQL, then SQLAlchemy/ORM session and models.", "Model tables and run joins via the ORM."),
                lesson("Migrations and Sessions", LessonType.READING, 35, "Schema migrations, transactions and session lifecycle.", "Add a column via a migration.")),
            module("HTML/CSS/JavaScript", "Frontend essentials for templated and dynamic pages.",
                lesson("Pages with HTML and CSS", LessonType.CODING, 45, "Structure, styling and responsive layout.", "Style a landing page."),
                lesson("JavaScript for Interactivity", LessonType.CODING, 45, "DOM events and fetch for dynamic behaviour.", "Add client-side validation."),
                lesson("Templating and Static Files", LessonType.READING, 35, "Rendering server templates and serving assets.", "Wire templates into a small app.")),
            module("Django or Flask", "Two paths to a Python web backend — pick one, learn the other.",
                lesson("Routing, Views and Templates", LessonType.CODING, 60, "Request handling, views and rendering.", "Build a multi-page site."),
                lesson("Models, Admin and Forms", LessonType.CODING, 60, "ORM models, admin and validated forms.", "Add a CRUD feature with forms."),
                lesson("REST APIs with Python", LessonType.CODING, 55, "Serialising JSON and versioning endpoints.", "Expose a REST API for your models."),
                lesson("Frameworks Quiz", LessonType.QUIZ, 15, "Views, models and routing.", null)),
            module("REST APIs", "Designing and consuming clean HTTP services.",
                lesson("HTTP, Resources and Status Codes", LessonType.READING, 35, "Verbs, resources, codes and idempotency.", "Map actions to verbs and codes."),
                lesson("Auth, Pagination and Errors", LessonType.CODING, 55, "Token auth, paginated lists and consistent error shapes.", "Add auth + pagination to an endpoint."),
                lesson("Consuming APIs from Python", LessonType.CODING, 40, "requests/httpx, retries and JSON handling.", "Call a public API and parse results.")),
            module("Frontend Integration", "Gluing a JavaScript UI to your Python API.",
                lesson("Fetching Data in the Browser", LessonType.CODING, 45, "fetch, CORS and rendering lists.", "Render data from your API."),
                lesson("Building a Full-Stack Feature", LessonType.PROJECT, 120, "Design the API and UI together and ship one feature.", "Deliver search + detail end to end.")),
            module("Projects", "Apply everything in portfolio-ready Python projects.",
                lesson("Guided Project Build", LessonType.PROJECT, 180, "A supervised end-to-end project.", "Plan, build, review with your trainer."),
                lesson("Testing and Deployment", LessonType.READING, 40, "pytest, WSGI servers and deploying to a host.", "Deploy the project behind gunicorn."),
                lesson("Project Showcase and Review", LessonType.ASSIGNMENT, 60, "Demo and collect structured feedback.", "Submit the final summary."))
        );
    }

    // =====================================================================
    // Web Development
    // =====================================================================
    private List<M> webSyllabus() {
        return List.of(
            module("HTML", "The semantic structure of every web page.",
                lesson("Document Structure and Semantics", LessonType.READING, 35, "Head/body, landmarks and the right element for the job.", "Rebuild a page using semantic tags."),
                lesson("Forms and Validation", LessonType.CODING, 45, "Inputs, attributes and native validation.", "Build an accessible contact form."),
                lesson("Media, Links and SEO Basics", LessonType.READING, 30, "Images, links, meta and headings.", "Add alt text and meta tags.")),
            module("CSS", "Styling, layout and responsive design.",
                lesson("Selectors, Box Model and Units", LessonType.READING, 40, "Specificity, the box model and relative units.", "Centre a box multiple ways."),
                lesson("Flexbox and Grid", LessonType.CODING, 60, "One- and two-dimensional layout systems.", "Build a card grid and a navbar."),
                lesson("Responsive and Theming", LessonType.CODING, 45, "Media queries, custom properties and dark mode.", "Add a theme toggle."),
                lesson("CSS Quiz", LessonType.QUIZ, 15, "Layout and specificity.", null)),
            module("Bootstrap", "Shipping polished UI quickly with a component system.",
                lesson("Grid and Utilities", LessonType.CODING, 40, "Columns, spacing and responsive utilities.", "Lay out a landing page."),
                lesson("Components", LessonType.CODING, 45, "Navbars, cards, modals and forms.", "Assemble a dashboard shell.")),
            module("JavaScript", "Making pages interactive with the language of the browser.",
                lesson("Fundamentals: Types, Functions, Scope", LessonType.READING, 45, "let/const, functions, closures and the event loop.", "Solve array transformation drills."),
                lesson("The DOM and Events", LessonType.CODING, 55, "Selecting, manipulating and reacting to the page.", "Build an interactive to-do list."),
                lesson("Async: Promises and Fetch", LessonType.CODING, 55, "async/await, fetch and handling responses.", "Load data from a public API."),
                lesson("JavaScript Quiz", LessonType.QUIZ, 15, "Scope, DOM and async.", null)),
            module("DOM", "Deep, practical control of the page.",
                lesson("Traversing and Mutating the DOM", LessonType.CODING, 50, "Nodes, templates and efficient updates.", "Render a list from data."),
                lesson("Events and Delegation", LessonType.CODING, 45, "Bubbling, capture and delegation patterns.", "Handle clicks on dynamic rows.")),
            module("Fetch API", "Talking to servers from the browser.",
                lesson("HTTP Basics and fetch", LessonType.CODING, 45, "Requests, responses, headers and JSON.", "Consume a REST endpoint."),
                lesson("Error Handling and Loading States", LessonType.READING, 35, "Robust async UX patterns.", "Add spinners and error toasts.")),
            module("Responsive Design", "Building interfaces that work on every screen.",
                lesson("Mobile-First and Breakpoints", LessonType.READING, 35, "Strategy for responsive layouts.", "Refactor a page to mobile-first."),
                lesson("Accessibility and Performance", LessonType.READING, 40, "ARIA, contrast, and Core Web Vitals.", "Run an a11y audit on your project.")),
            module("Projects", "Portfolio projects that prove the skills.",
                lesson("Guided Project Build", LessonType.PROJECT, 180, "A multi-page responsive site with real data.", "Plan and build with checkpoints."),
                lesson("Project Showcase and Review", LessonType.ASSIGNMENT, 60, "Present and receive feedback.", "Deploy and share the link."))
        );
    }

    // =====================================================================
    // AWS Cloud Computing
    // =====================================================================
    private List<M> awsSyllabus() {
        return List.of(
            module("Cloud Fundamentals", "What the cloud is and how AWS is organised.",
                lesson("Cloud Models and AWS Overview", LessonType.READING, 35, "IaaS/PaaS/SaaS, regions, AZs and the console.", "Explore the console and set up billing alerts."),
                lesson("Accounts, Billing and the Free Tier", LessonType.READING, 30, "Organisations, cost tracking and staying free.", "Read a cost report."),
                lesson("Fundamentals Quiz", LessonType.QUIZ, 15, "Regions, AZs and models.", null)),
            module("IAM", "Secure identity and access management.",
                lesson("Users, Groups, Roles and Policies", LessonType.READING, 45, "Principals, JSON policies and least privilege.", "Create a role with a scoped policy."),
                lesson("MFA, Federation and Best Practices", LessonType.CODING, 40, "Hardening access and temporary credentials.", "Enforce MFA and use a permission boundary.")),
            module("EC2", "Virtual servers in the cloud.",
                lesson("Instances, AMIs and Key Pairs", LessonType.CODING, 50, "Launching, connecting and stopping instances.", "Launch and SSH into a Linux instance."),
                lesson("Security Groups and Load Balancing", LessonType.CODING, 55, "Firewalls, target groups and health checks.", "Put two instances behind a load balancer."),
                lesson("Auto Scaling", LessonType.READING, 35, "Scaling policies and launch templates.", "Configure an autoscaling group."),
                lesson("EC2 Quiz", LessonType.QUIZ, 15, "Instances and security groups.", null)),
            module("S3", "Scalable object storage.",
                lesson("Buckets, Objects and Storage Classes", LessonType.CODING, 45, "Uploading, versioning and lifecycle rules.", "Create a bucket with a lifecycle policy."),
                lesson("Access, Encryption and Static Hosting", LessonType.CODING, 50, "Bucket policies, encryption and hosting a site.", "Host a static site from S3.")),
            module("VPC", "Your private network in the cloud.",
                lesson("Subnets, Route Tables and Gateways", LessonType.READING, 50, "Public vs private subnets and routing.", "Build a VPC with two subnets."),
                lesson("Security and NAT", LessonType.CODING, 45, "NACLs, security groups and NAT gateways.", "Isolate a private tier behind NAT.")),
            module("RDS", "Managed relational databases.",
                lesson("Provisioning and Engines", LessonType.READING, 40, "Instance classes, engines and parameters.", "Launch a MySQL RDS instance."),
                lesson("Backups, Read Replicas and HA", LessonType.READING, 40, "Multi-AZ, failover and replication.", "Enable automated backups and a replica.")),
            module("CloudWatch", "Observability: metrics, logs and alarms.",
                lesson("Metrics, Logs and Alarms", LessonType.CODING, 45, "Dashboards, log groups and alerting.", "Alarm on high CPU."),
                lesson("Dashboards and Automation", LessonType.READING, 30, "Operational visibility and remediation.", "Build a service dashboard.")),
            module("Deployment", "Shipping and operating workloads on AWS.",
                lesson("Serverless with Lambda", LessonType.CODING, 50, "Functions, triggers and API Gateway.", "Deploy a Lambda behind an API."),
                lesson("CI/CD and Elastic Beanstalk", LessonType.READING, 45, "Pipelines and platform deployment.", "Deploy an app with Beanstalk."),
                lesson("Well-Architected Review", LessonType.QUIZ, 20, "The six pillars applied to a design.", null))
        );
    }

    // =====================================================================
    // Artificial Intelligence
    // =====================================================================
    private List<M> aiSyllabus() {
        return List.of(
            module("Python for AI", "The numerical Python toolkit used across AI.",
                lesson("Python Refresher for Data", LessonType.READING, 35, "Functions, comprehensions and the notebook workflow.", "Set up Jupyter and clean a dataset."),
                lesson("NumPy Arrays and Broadcasting", LessonType.CODING, 50, "Vectorised maths on ndarrays.", "Implement dot products with NumPy."),
                lesson("Pandas for Dataframes", LessonType.CODING, 60, "Loading, filtering, grouping and reshaping tabular data.", "Explore and summarise a CSV dataset.")),
            module("Data Handling", "Getting data into a model-ready state.",
                lesson("Loading and Inspecting Data", LessonType.CODING, 45, "Sources, dtypes and quick profiling.", "Profile missing values."),
                lesson("Cleaning and Feature Engineering", LessonType.CODING, 60, "Imputation, encoding, scaling and transforms.", "Build a preprocessing pipeline."),
                lesson("Data Handling Quiz", LessonType.QUIZ, 15, "Cleaning and encoding.", null)),
            module("Machine Learning", "Training models that learn from data.",
                lesson("Supervised vs Unsupervised", LessonType.READING, 35, "Problem types and the ML workflow.", "Classify a set of tasks."),
                lesson("Regression", LessonType.CODING, 55, "Linear/logistic regression and regularisation.", "Fit and evaluate a regression model."),
                lesson("Classification", LessonType.CODING, 60, "Trees, SVMs, k-NN and ensembles.", "Train a spam classifier."),
                lesson("Clustering and Dimensionality Reduction", LessonType.CODING, 55, "k-means, PCA and when to use them.", "Cluster customers and visualise with PCA.")),
            module("Model Evaluation", "Knowing whether your model actually works.",
                lesson("Metrics: Precision, Recall, ROC", LessonType.READING, 45, "Confusion matrix and the right metric per problem.", "Compute metrics from a confusion matrix."),
                lesson("Cross-Validation and Tuning", LessonType.CODING, 50, "Over/underfitting, CV and hyperparameter search.", "Grid-search a classifier."),
                lesson("Evaluation Quiz", LessonType.QUIZ, 15, "Metrics and bias-variance.", null)),
            module("Deep Learning Fundamentals", "Neural networks from the ground up.",
                lesson("Perceptrons and Backpropagation", LessonType.READING, 50, "Neurons, activations, loss and gradient descent.", "Trace a forward/backward pass."),
                lesson("Building Networks", LessonType.CODING, 65, "MLPs, CNNs and training loops in a framework.", "Train an image classifier."),
                lesson("Regularisation and Optimisers", LessonType.READING, 35, "Dropout, batch norm and adaptive learning.", "Improve a network's validation score.")),
            module("NLP and Computer Vision", "Applied deep learning on text and images.",
                lesson("Text Processing and Embeddings", LessonType.CODING, 55, "Tokenisation, embeddings and sentiment.", "Build a text classifier."),
                lesson("Vision Models and Transfer Learning", LessonType.CODING, 55, "CNN architectures and fine-tuning.", "Fine-tune a pretrained image model.")),
            module("AI Applications", "Putting models to work responsibly.",
                lesson("Deploying and Serving Models", LessonType.READING, 40, "Packaging, APIs and monitoring predictions.", "Serve a model behind an API."),
                lesson("Ethics, Bias and Interpretability", LessonType.READING, 35, "Fairness, explainability and safe AI.", "Audit a model for bias."),
                lesson("AI Applications Quiz", LessonType.QUIZ, 15, "Deployment and ethics.", null)),
            module("Projects", "End-to-end AI projects with clear metrics.",
                lesson("Capstone AI Project", LessonType.PROJECT, 180, "Frame, build and evaluate an applied AI project.", "Deliver a model with an evaluation report."),
                lesson("Project Showcase and Review", LessonType.ASSIGNMENT, 60, "Present results and methods.", "Submit the final write-up."))
        );
    }
}

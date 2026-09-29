package com.futureboundtech;

import com.futureboundtech.entity.MockTest;
import com.futureboundtech.entity.PracticeQuestion;
import com.futureboundtech.enums.QuestionCategory;
import com.futureboundtech.enums.QuestionDifficulty;
import com.futureboundtech.enums.QuestionType;
import com.futureboundtech.repository.MockTestRepository;
import com.futureboundtech.repository.PracticeQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds a starter practice bank (Phase 16): MCQs across the categories,
 * study-only coding problems and one timed mock test. The pass is ADDITIVE and
 * idempotent — it matches on the question text, so a restart only fills in what is
 * missing and never duplicates or overwrites existing bank entries.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeders.enabled", havingValue = "true", matchIfMissing = true)
public class PracticeDataSeeder {

    private final PracticeQuestionRepository questionRepository;
    private final MockTestRepository mockTestRepository;

    /** Question texts already in the bank, used to keep seeding idempotent. */
    private final Set<String> seenTexts = new HashSet<>();

    @Bean
    @Order(3)
    public CommandLineRunner initPractice() {
        return args -> {
            seedQuestions();
            if (mockTestRepository.count() == 0) {
                seedMockTest();
            }
        };
    }

    private void seedQuestions() {
        seenTexts.clear();
        questionRepository.findAll().forEach(q -> seenTexts.add(q.getQuestionText()));
        // ----- MCQs: one or two per category -----
        mcq(QuestionCategory.JAVA, "OOP", QuestionDifficulty.EASY,
                "Which keyword is used to inherit a class in Java?",
                "extends", "implements", "inherits", "super", "A",
                "'extends' establishes a class-to-class inheritance relationship; 'implements' is for interfaces.");
        mcq(QuestionCategory.JAVA, "Collections", QuestionDifficulty.MEDIUM,
                "Which Java collection does NOT allow duplicate elements?",
                "ArrayList", "LinkedList", "HashSet", "Vector", "C",
                "HashSet is backed by a hash table and enforces uniqueness of its elements.");

        mcq(QuestionCategory.PYTHON, "Basics", QuestionDifficulty.EASY,
                "What is the output of print(type([])) in Python?",
                "<class 'list'>", "<class 'tuple'>", "<class 'dict'>", "<class 'set'>", "A",
                "Square brackets create a list literal, so type([]) reports the list class.");
        mcq(QuestionCategory.PYTHON, "Data Structures", QuestionDifficulty.MEDIUM,
                "Which Python data structure is immutable?",
                "list", "dict", "tuple", "set", "C",
                "Tuples cannot be modified after creation, unlike lists, dicts and sets.");

        mcq(QuestionCategory.SQL, "Joins", QuestionDifficulty.MEDIUM,
                "Which JOIN returns only rows matching in BOTH tables?",
                "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL OUTER JOIN", "C",
                "INNER JOIN keeps the intersection of the two tables based on the join condition.");
        mcq(QuestionCategory.SQL, "Aggregation", QuestionDifficulty.EASY,
                "Which clause filters groups after aggregation in SQL?",
                "WHERE", "HAVING", "GROUP BY", "ORDER BY", "B",
                "WHERE filters rows before grouping; HAVING filters the resulting groups.");

        mcq(QuestionCategory.HTML, "Semantics", QuestionDifficulty.EASY,
                "Which HTML5 tag defines the main navigation area of a page?",
                "<navigation>", "<nav>", "<menu>", "<links>", "B",
                "The semantic <nav> element marks major navigation blocks.");

        mcq(QuestionCategory.CSS, "Layout", QuestionDifficulty.MEDIUM,
                "Which CSS property creates a flexible box layout?",
                "display: flex", "position: relative", "float: left", "layout: grid", "A",
                "'display: flex' enables the Flexbox one-dimensional layout model.");

        mcq(QuestionCategory.JAVASCRIPT, "Basics", QuestionDifficulty.EASY,
                "Which keyword declares a block-scoped variable in JavaScript?",
                "var", "let", "def", "dim", "B",
                "'let' (and 'const') are block-scoped; 'var' is function-scoped.");
        mcq(QuestionCategory.JAVASCRIPT, "Async", QuestionDifficulty.HARD,
                "What does a JavaScript Promise represent?",
                "A synchronous loop", "An eventual result of an async operation", "A DOM node", "A CSS animation", "B",
                "A Promise is a proxy for a value that will be available when an async operation settles.");

        mcq(QuestionCategory.AWS, "Compute", QuestionDifficulty.EASY,
                "Which AWS service provides virtual servers (instances)?",
                "S3", "EC2", "RDS", "CloudFront", "B",
                "Amazon EC2 (Elastic Compute Cloud) provides resizable virtual servers.");
        mcq(QuestionCategory.AWS, "Storage", QuestionDifficulty.MEDIUM,
                "Which AWS storage service is object storage accessed over HTTP?",
                "EBS", "Instance Store", "S3", "EFS", "C",
                "Amazon S3 is web-scale object storage; EBS is block storage and EFS is file storage.");

        mcq(QuestionCategory.AI, "Machine Learning", QuestionDifficulty.MEDIUM,
                "Supervised learning is trained on data that is…",
                "Unlabelled", "Labelled", "Encrypted", "Streaming only", "B",
                "Supervised learning uses input–output label pairs to learn a mapping.");
        mcq(QuestionCategory.AI, "Fundamentals", QuestionDifficulty.EASY,
                "Which task is a classification problem?",
                "Predicting house price", "Detecting spam email", "Forecasting temperature", "Recommending duration", "B",
                "Spam detection assigns inputs to discrete categories, i.e. classes.");

        mcq(QuestionCategory.APTITUDE, "Percentages", QuestionDifficulty.EASY,
                "What is 20% of 250?",
                "30", "40", "50", "60", "C",
                "20% of 250 = 0.2 × 250 = 50.");
        mcq(QuestionCategory.APTITUDE, "Time & Work", QuestionDifficulty.MEDIUM,
                "A finishes work in 10 days, B in 15 days. Working together they finish in?",
                "5 days", "6 days", "7.5 days", "8 days", "B",
                "Combined rate = 1/10 + 1/15 = 1/6 of the work per day → 6 days.");

        mcq(QuestionCategory.REASONING, "Series", QuestionDifficulty.EASY,
                "Find the next number: 2, 4, 8, 16, …",
                "18", "24", "32", "36", "C",
                "Each term doubles the previous one, so the next term is 32.");
        mcq(QuestionCategory.REASONING, "Coding-Decoding", QuestionDifficulty.MEDIUM,
                "If CAT is coded as DBU, how is DOG coded?",
                "EPH", "EPI", "FQH", "CPH", "A",
                "Each letter shifts one position forward: D→E, O→P, G→H = EPH.");

        mcq(QuestionCategory.TECHNICAL_INTERVIEW, "Design", QuestionDifficulty.HARD,
                "What does horizontal scaling mean?",
                "Adding more power to one machine", "Adding more machines to share load", "Compressing data", "Reducing threads", "B",
                "Horizontal scaling adds nodes; vertical scaling upgrades a single node.");
        mcq(QuestionCategory.TECHNICAL_INTERVIEW, "APIs", QuestionDifficulty.MEDIUM,
                "Which HTTP method is idempotent and used to retrieve data?",
                "POST", "GET", "PATCH", "TRACE", "B",
                "GET requests are safe and idempotent and are used to read resources.");

        mcq(QuestionCategory.HR_INTERVIEW, "Behavioural", QuestionDifficulty.EASY,
                "What does the STAR method structure?",
                "A salary negotiation", "A behavioural interview answer", "A code review", "A project plan", "B",
                "STAR = Situation, Task, Action, Result — a framework for answering behavioural questions.");
        mcq(QuestionCategory.HR_INTERVIEW, "Motivation", QuestionDifficulty.EASY,
                "The best way to answer 'Why should we hire you?' is to…",
                "List personal problems", "Match your skills to the job needs", "Ask for a higher salary", "Say you need training", "B",
                "Employers want evidence that your strengths solve the role's problems.");

        // ----- Coding library (study-only, never executed) -----
        coding(QuestionCategory.JAVA, QuestionDifficulty.EASY,
                "Reverse a string",
                "Write a program that reads a word and prints it in reverse order.",
                "A single line containing a string S (1 ≤ |S| ≤ 1000).",
                "The reversed string on one line.",
                "|S| ≤ 1000\nS contains alphanumeric characters only.",
                "Input:  hello\nOutput: olleh",
                "strings, loops");
        coding(QuestionCategory.PYTHON, QuestionDifficulty.MEDIUM,
                "FizzBuzz",
                "Print numbers 1 to N, replacing multiples of 3 with 'Fizz', of 5 with 'Buzz', and of both with 'FizzBuzz'.",
                "A single integer N (1 ≤ N ≤ 100).",
                "N lines, one value per line as described.",
                "1 ≤ N ≤ 100",
                "Input:  5\nOutput: 1 2 Fizz 4 Buzz (each on its own line)",
                "loops, conditionals");
        coding(QuestionCategory.SQL, QuestionDifficulty.MEDIUM,
                "Second highest salary",
                "Given an 'employees' table (id, name, salary), write a query returning the second highest salary.",
                "Table employees(id INT, name VARCHAR, salary DECIMAL).",
                "A single row with column 'salary' (or NULL when it does not exist).",
                "Handle duplicate salary values.",
                "If salaries are 100, 200, 200, 300 the answer is 200.",
                "sql, aggregation");
        coding(QuestionCategory.JAVASCRIPT, QuestionDifficulty.EASY,
                "Two sum",
                "Given an array of integers and a target, return the indices of the two numbers that add up to the target.",
                "nums: integer array (length ≥ 2); target: integer.",
                "An array with the two indices, e.g. [0, 1].",
                "Exactly one valid answer exists; an element cannot be used twice.",
                "nums = [2, 7, 11, 15], target = 9 → [0, 1]",
                "arrays, hashmaps");

        // ----- Expanded course-based MCQs (added idempotently) -----
        mcq(QuestionCategory.JAVA, "Collections", QuestionDifficulty.EASY,
                "Which interface represents an ordered collection that allows duplicates?",
                "Set", "List", "Map", "Queue", "B",
                "A List is ordered and permits duplicate elements; a Set forbids duplicates.");
        mcq(QuestionCategory.JAVA, "String", QuestionDifficulty.MEDIUM,
                "Why are String objects in Java immutable?",
                "Because they are stored on the heap only",
                "So their value cannot change once created, enabling safe sharing and caching",
                "Because they cannot be null",
                "Because they are primitive types", "B",
                "Immutability lets Strings be safely shared, cached in the pool and used as hash keys.");
        mcq(QuestionCategory.JAVA, "Spring Boot", QuestionDifficulty.MEDIUM,
                "Which annotation marks a class as a REST controller returning JSON?",
                "@Controller", "@RestController", "@Service", "@Component", "B",
                "@RestController combines @Controller with @ResponseBody so handlers return data directly.");
        mcq(QuestionCategory.JAVA, "Spring Boot", QuestionDifficulty.HARD,
                "In Spring, what does constructor injection primarily improve?",
                "Runtime speed", "Immutability and testability of dependencies", "HTTP caching", "SQL performance", "B",
                "Constructor injection allows final fields and easy instantiation in unit tests.");

        mcq(QuestionCategory.PYTHON, "Functions", QuestionDifficulty.EASY,
                "Which keyword defines a function in Python?",
                "func", "def", "function", "lambda only", "B",
                "def introduces a named function; lambda creates an anonymous one.");
        mcq(QuestionCategory.PYTHON, "Data Structures", QuestionDifficulty.MEDIUM,
                "What does the expression [x*2 for x in range(3)] evaluate to?",
                "[0, 1, 2]", "[0, 2, 4]", "[2, 4, 6]", "range(3)", "B",
                "The list comprehension doubles each of 0,1,2 giving [0, 2, 4].");
        mcq(QuestionCategory.PYTHON, "Django", QuestionDifficulty.MEDIUM,
                "In Django, which component defines the database schema as a Python class?",
                "View", "Template", "Model", "URLconf", "C",
                "Django models map to database tables and express fields/relationships in Python.");

        mcq(QuestionCategory.SQL, "Aggregate", QuestionDifficulty.EASY,
                "Which SQL function returns the number of rows?",
                "SUM()", "COUNT()", "TOTAL()", "SIZE()", "B",
                "COUNT(*) returns the number of rows in a group.");
        mcq(QuestionCategory.SQL, "Joins", QuestionDifficulty.HARD,
                "Which JOIN keeps all rows from the left table plus matches from the right?",
                "INNER JOIN", "LEFT JOIN", "CROSS JOIN", "NATURAL JOIN", "B",
                "A LEFT JOIN returns every left-table row, with NULLs where there is no right match.");
        mcq(QuestionCategory.SQL, "Keys", QuestionDifficulty.MEDIUM,
                "What does a FOREIGN KEY enforce?",
                "Uniqueness within a table", "Referential integrity between tables", "Encryption", "Index speed", "B",
                "Foreign keys ensure values reference valid rows in the parent table.");

        mcq(QuestionCategory.HTML, "Forms", QuestionDifficulty.MEDIUM,
                "Which input type shows a date picker?",
                "type=\"text\"", "type=\"date\"", "type=\"calendar\"", "type=\"datetime\"", "B",
                "The native date input type is 'date'.");
        mcq(QuestionCategory.CSS, "Selectors", QuestionDifficulty.EASY,
                "Which selector targets an element with id=\"main\"?",
                ".main", "#main", "main", "*main", "B",
                "The hash (#) prefix selects by id; the dot (.) selects by class.");
        mcq(QuestionCategory.CSS, "Layout", QuestionDifficulty.HARD,
                "Which CSS layout system is best for two-dimensional grids?",
                "Flexbox", "CSS Grid", "Floats", "Tables", "B",
                "CSS Grid handles rows and columns together; Flexbox is one-dimensional.");
        mcq(QuestionCategory.JAVASCRIPT, "Async", QuestionDifficulty.MEDIUM,
                "What does 'await' do inside an async function?",
                "Blocks the whole thread", "Pauses until the Promise settles", "Throws an error", "Loops forever", "B",
                "await suspends the async function until the Promise resolves, without blocking the main thread.");
        mcq(QuestionCategory.JAVASCRIPT, "Basics", QuestionDifficulty.MEDIUM,
                "What is the result of typeof null in JavaScript?",
                "'null'", "'object'", "'undefined'", "'number'", "B",
                "A long-standing quirk: typeof null returns 'object'.");

        mcq(QuestionCategory.AWS, "Networking", QuestionDifficulty.MEDIUM,
                "Which AWS service provides a managed virtual private network?",
                "S3", "VPC", "IAM", "CloudFront", "B",
                "Amazon VPC lets you launch resources into a logically isolated virtual network.");
        mcq(QuestionCategory.AWS, "Serverless", QuestionDifficulty.MEDIUM,
                "Which service runs code without provisioning servers?",
                "EC2", "Lambda", "RDS", "EBS", "B",
                "AWS Lambda runs functions in response to events with no servers to manage.");
        mcq(QuestionCategory.AWS, "Monitoring", QuestionDifficulty.EASY,
                "Which service collects metrics and sets alarms?",
                "CloudWatch", "CloudTrail", "GuardDuty", "Config", "A",
                "CloudWatch gathers metrics and logs and can trigger alarms.");

        mcq(QuestionCategory.AI, "Machine Learning", QuestionDifficulty.MEDIUM,
                "Overfitting means the model…",
                "Performs equally on train and test",
                "Performs well on training data but poorly on unseen data",
                "Cannot be trained",
                "Has too few parameters", "B",
                "Overfitting captures noise in training data, hurting generalisation.");
        mcq(QuestionCategory.AI, "Deep Learning", QuestionDifficulty.HARD,
                "Which activation is most common in hidden layers of a modern neural network?",
                "Sigmoid", "ReLU", "Linear", "Step", "B",
                "ReLU is widely used for hidden layers because it mitigates vanishing gradients.");
        mcq(QuestionCategory.AI, "NLP", QuestionDifficulty.MEDIUM,
                "Tokenisation in NLP is the process of…",
                "Encrypting text", "Splitting text into words/subwords", "Compressing images", "Sorting rows", "B",
                "Tokenisation breaks raw text into units a model can process.");

        mcq(QuestionCategory.APTITUDE, "Profit & Loss", QuestionDifficulty.MEDIUM,
                "An item bought for ₹400 and sold for ₹500. What is the profit percent?",
                "20%", "25%", "10%", "30%", "B",
                "Profit = 100 on cost 400 → 100/400 = 25%.");
        mcq(QuestionCategory.APTITUDE, "Averages", QuestionDifficulty.EASY,
                "The average of 10, 20 and 30 is?",
                "15", "20", "25", "30", "B",
                "(10+20+30)/3 = 60/3 = 20.");
        mcq(QuestionCategory.REASONING, "Analogy", QuestionDifficulty.MEDIUM,
                "Doctor : Hospital :: Teacher : ?",
                "Student", "School", "Book", "Lesson", "B",
                "A doctor works in a hospital as a teacher works in a school.");
        mcq(QuestionCategory.TECHNICAL_INTERVIEW, "Data Structures", QuestionDifficulty.MEDIUM,
                "Which structure uses FIFO ordering?",
                "Stack", "Queue", "Tree", "Graph", "B",
                "A queue is First-In-First-Out; a stack is Last-In-First-Out.");
        mcq(QuestionCategory.TECHNICAL_INTERVIEW, "Databases", QuestionDifficulty.HARD,
                "CAP theorem states a distributed system can guarantee at most two of…",
                "Consistency, Availability, Partition tolerance",
                "Caching, Accuracy, Performance",
                "Concurrency, Atomicity, Persistence",
                "Cost, Agility, Portability", "A",
                "CAP: Consistency, Availability, Partition tolerance — pick two under a network partition.");
        mcq(QuestionCategory.HR_INTERVIEW, "Teamwork", QuestionDifficulty.EASY,
                "When you disagree with a teammate's technical approach you should…",
                "Ignore it", "Discuss trade-offs with evidence and decide as a team",
                "Escalate immediately", "Fork the repo", "B",
                "Constructive, evidence-based discussion reaches better outcomes than unilateral action.");

        // ----- Expanded coding library (still study-only) -----
        coding(QuestionCategory.JAVA, QuestionDifficulty.MEDIUM,
                "Palindrome check",
                "Read a word and print whether it is a palindrome (reads the same forwards and backwards).",
                "A single line containing a string S (1 ≤ |S| ≤ 1000).",
                "'yes' or 'no' on one line.",
                "Compare case-insensitively; ignore nothing else.",
                "Input:  Level\nOutput: yes",
                "strings, two pointers");
        coding(QuestionCategory.PYTHON, QuestionDifficulty.EASY,
                "Sum of digits",
                "Read a non-negative integer and print the sum of its digits.",
                "A single integer N (0 ≤ N ≤ 10^9).",
                "The digit sum on one line.",
                "10^9 upper bound fits in a 64-bit int.",
                "Input:  1234\nOutput: 10",
                "loops, math");
        coding(QuestionCategory.SQL, QuestionDifficulty.HARD,
                "Nth highest salary",
                "Write a query returning the Nth highest distinct salary from an 'employees' table.",
                "Table employees(id, name, salary); a parameter N.",
                "A single value: the Nth highest salary or NULL.",
                "Distinct salaries; N ≥ 1.",
                "For salaries 300,200,200,100 and N=2 the answer is 200.",
                "sql, window functions");
        coding(QuestionCategory.JAVASCRIPT, QuestionDifficulty.MEDIUM,
                "Valid parentheses",
                "Given a string of brackets, return whether every bracket is properly closed in order.",
                "A string containing only the characters ( ) { } [ ].",
                "true or false.",
                "Empty string is valid.",
                "Input:  '{[]}'\nOutput: true",
                "stack, strings");
        coding(QuestionCategory.AI, QuestionDifficulty.MEDIUM,
                "Mean squared error",
                "Given two equal-length lists of actual and predicted values, compute the mean squared error.",
                "Two lists: actual[], predicted[] of equal length n ≥ 1.",
                "A single float: the MSE.",
                "Same length; values are numeric.",
                "actual=[1,2], pred=[2,2] → MSE = (1+0)/2 = 0.5",
                "regression, metrics");

        questionRepository.flush();
    }

    private void seedMockTest() {
        List<PracticeQuestion> mcqs = questionRepository
                .findByTypeAndPublishedTrueOrderByCreatedAtDesc(QuestionType.MCQ);
        List<PracticeQuestion> picks = mcqs.stream()
                .filter(q -> List.of("Which keyword is used to inherit a class in Java?",
                        "Which JOIN returns only rows matching in BOTH tables?",
                        "Which AWS service provides virtual servers (instances)?",
                        "What is 20% of 250?",
                        "Find the next number: 2, 4, 8, 16, …",
                        "What does the STAR method structure?")
                        .contains(q.getQuestionText()))
                .limit(6)
                .toList();
        if (picks.size() < 2) {
            return;
        }
        int totalMarks = picks.stream().mapToInt(PracticeQuestion::getMarks).sum();
        mockTestRepository.save(MockTest.builder()
                .title("Foundation Mock Test")
                .description("A quick 10-minute mix of technical and aptitude questions to benchmark your preparation.")
                .durationMinutes(10)
                .passingMarks((int) Math.ceil(totalMarks * 0.5))
                .published(true)
                .questions(new ArrayList<>(picks))
                .build());
    }

    private void mcq(QuestionCategory category, String topic, QuestionDifficulty difficulty,
                     String question, String a, String b, String c, String d,
                     String correct, String explanation) {
        if (!seenTexts.add(question)) {
            return; // already in the bank (idempotent)
        }
        questionRepository.save(PracticeQuestion.builder()
                .type(QuestionType.MCQ)
                .category(category)
                .topic(topic)
                .difficulty(difficulty)
                .questionText(question)
                .optionA(a)
                .optionB(b)
                .optionC(c)
                .optionD(d)
                .correctOption(correct)
                .explanation(explanation)
                .marks(1)
                .published(true)
                .build());
    }

    private void coding(QuestionCategory category, QuestionDifficulty difficulty, String title,
                        String statement, String input, String output,
                        String constraints, String examples, String tags) {
        String text = title + " — " + statement;
        if (!seenTexts.add(text)) {
            return; // already in the bank (idempotent)
        }
        questionRepository.save(PracticeQuestion.builder()
                .type(QuestionType.CODING)
                .category(category)
                .difficulty(difficulty)
                .questionText(text)
                .inputFormat(input)
                .outputFormat(output)
                .constraintsText(constraints)
                .examples(examples)
                .tags(tags)
                .published(true)
                .build());
    }
}

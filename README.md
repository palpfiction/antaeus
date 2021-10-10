# Solution
[Scroll down to the original README](#antaeus)

This is my solution to the Antaeus challenge. 

## Process

Solving the challenge first started with a careful read of the instructions given. Because the requirements give so much freedom, knowing the amount of work to put into the solution was the first obstacle to overcome: it can be done with a few lines of code, or it can even be solved by using complex frameworks, lots of classes or even new modules, depending on the level of the detail desired. Given that this is a take home test in an early stage of the interview, I aimed to spend no more than 6 hours of work, an amount of time that seemed reasonable in order to solve the task in a way that gives a hint about how we could work together.

After looking at the code, writing down some ideas and narrowing what I wanted to do, I started by setting up the development environment. IntelliJ versions of the JDK/Kotlin SDK were a headache during all the implementation, but I could make it work at the end. I selected what seemed more important to me, BillingService. Here was going to be the business logic that handled all billing in the application, and although there was going to be only one behaviour (charging an invoice), in a real application it would be core to the domain and thus should be written with care. That's why I decided to go with TDD and made all the progress by creating the tests, making them pass and refactoring continuously, until getting to the implementation you can see here.  

When the BillingService was defined and implemented, the next thing was scheduling a job. I have a lot of experience with batch jobs using Spring Batch, a very powerful and scalable solution for this kind of tasks, but it seemed really overkill to use it this challenge. After researching about scheduling libraries and even the Java's own library, I decided to go for Quartz, which I have used under Spring Batch and is a pretty good solution either for little jobs like this or for production services. I created a Quartz job with a cron trigger to charge all the pending invoices at the first day of the month. The job itself is using kotlin coroutines in order to process multiple invoices at the same time. Retries are handled in a very simple way (by just sleeping the coroutine, I know there are far better solutions), but rate limit is not as I decided it was out of the scope of this task. 

When the job was up and running, I created an endpoint to trigger the job as needed (in order to help test the challenge, not as a real solution: in production it should have a lot more security and concerns) and I added some domain events, which I think are indispensable these days with the decoupled architecture of microservices. By producing events of the actions that take place in our application, we can let other services know about what has happened and perform actions with them, update their knowledge or generate reports.

## Decisions

- The BillingService only handles the charging of the invoice, it does not know anything about the monthly payments or jobs. This way, that simple behaviour gets isolated from the rest of the application and can be reused by other use cases. It also makes it easier to test and thus, easier to change.
- I have added two more statuses to the InvoiceStatus: `PAYMENT_FAILED`, for when the payment is unsuccessful (maybe not enough balance?) and `INCONSISTENT_DATA`, for when there is no customer with that id or the currency cannot be converted. I thought about just putting `PAYMENT_FAILED` and an error message but enums behave better: they are typed, contrary to messages, so no mistakes, easier handling and faster database queries, among other advantages.
- If the payment provider raises a NetworkException, the BillingServices rethrows it because the handling may be different depending on the use case. For example, in this case, I decided that we could try again 1s later, but in other kind of process that choice could be different, and we allow it easily by doing this.
- A currency converter external service was added to improve the rate of success and speed of the invoice payment. Another solution was to store the invoice in a special state `CURRENCY_MISMATCH` and have other job fix invoices in this state, or even, have this fixed by another service consuming an event, but this solution was simpler and in the scope of the challenge.
  - Regarding this, I think that the best way to handle the case when we convert the currency successfully is to update the state of the mismatching invoice with `DISCARDED` or something along those lines and creating a new one with the correct currency, but this was discarded for brevity.
- I decided trying to avoid the exceptions that the PaymentProvider could raise: CurrencyMismatch and CustomerNotFound. By checking if the customer exists and that currency matches, we can discard some requests that would be unsuccessful, relieving the provider of some work.

## Next steps 

- Integration testing: due to the nature of the implementation I estimated that it was going to take some more time than what I had estimated for the challenge.
- A job that handles the failed payments: try charging every x days in order to recover from the error.
- More logging: the state of the job could be persisted to database and that info could be showed via the REST API or a dashboard.
- Introduce domain driven design and hexagonal architecture: it would help keeping the software close to the business/product team and make the growth of the application easier.
- Configuration: the number of coroutines launched should be more easily configurable, maybe through an external file or a configuration service.
- Handle the payments that could not be processed due to network exceptions. Currently, they are kept the same, with status PENDING, but there should be a better way to keep track and recover them.

This solution took me around six and a half hours, distributed in three different days.

You can try the solution launching the application and making a POST request to the `/rest/v1/scheduling/charge-invoices-monthly`, that will trigger the job, and shortly it's possible to see that the invoices have been paid. The domain events are printed to the console, so you can see the process easily.

---

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

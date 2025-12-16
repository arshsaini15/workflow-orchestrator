Workflow Orchestrator – Backend Distributed Task Engine

A backend workflow orchestration system designed to execute tasks based on DAG (Directed Acyclic Graph) dependencies with support for concurrency, retries, and fault tolerance.

🚀 Key Features

Executes workflows where tasks depend on one another using DAG-based scheduling

Asynchronous task execution with configurable thread pools

Robust task lifecycle management (READY, IN_PROGRESS, COMPLETED, FAILED)

Redis-based distributed locking to avoid duplicate task execution in concurrent environments

Retry mechanism with exponential backoff for failed tasks

Pagination, sorting, and filtering APIs for workflows and tasks

Event-driven architecture planned using Kafka for real-time task state tracking

🛠 Tech Stack

Backend: Java, Spring Boot

Data Store: MySQL

Caching & Locks: Redis

Messaging (Planned): Kafka

Build Tool: Maven

🧠 System Design Concepts Used

Distributed locking

Thread pools and concurrency control

State machines

Fault tolerance and retries

Event-driven architecture (in progress)

📌 Current Status

Kafka integration is in progress. Core workflow execution engine is complete and functional.

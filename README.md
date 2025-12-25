Workflow Orchestrator – Backend Distributed Task Engine

A backend workflow orchestration system designed to execute tasks based on DAG (Directed Acyclic Graph) dependencies, with support for concurrent execution, retries, and fault tolerance in a distributed environment.

🚀 Key Features

Executes workflows with interdependent tasks using DAG-based scheduling to enforce execution order

Asynchronous task execution using configurable, bounded thread pools

Robust task lifecycle management (READY, IN_PROGRESS, COMPLETED, FAILED)

Redis-based distributed locking and idempotency to prevent duplicate task execution across concurrent or multi-instance deployments

Retry mechanism with exponential backoff for fault-tolerant task execution

Event-driven state propagation using Kafka to decouple task execution from workflow state tracking and downstream consumers

REST APIs supporting pagination, sorting, and filtering for workflows and tasks

🛠 Tech Stack

Backend: Java, Spring Boot

Database: MySQL

Distributed Locking & Idempotency: Redis

Messaging: Kafka

Build Tool: Maven

🧠 System Design Concepts Used

Distributed locking and idempotent processing

Bounded thread pools and concurrency control

Task lifecycle state machines

Fault tolerance with retries and backoff

Event-driven architecture using Kafka

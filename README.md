**Workflow Orchestrator – Backend Distributed Task Engine**

A backend workflow orchestration system designed to execute tasks based on DAG (Directed Acyclic Graph) dependencies, with support for concurrent execution, retries, and fault tolerance in a distributed environment.

**🚀 Key Features**

Executes workflows with interdependent tasks using DAG-based scheduling to strictly enforce execution order

Asynchronous task execution using configurable, bounded thread pools for controlled parallelism

Robust task lifecycle management with well-defined states: READY, IN_PROGRESS, COMPLETED, FAILED

Redis-based distributed locking and idempotency to prevent duplicate task execution across concurrent threads or multiple application instances

Retry mechanism with exponential backoff to handle transient failures gracefully

Event-driven state propagation using Kafka to decouple task execution from workflow state tracking and downstream consumers

REST APIs supporting pagination, sorting, and filtering for workflows and tasks

**🛠 Tech Stack**

Backend: Java, Spring Boot

Database: MySQL

Distributed Locking & Idempotency: Redis

Messaging: Kafka

Build Tool: Maven

**🧠 System Design Concepts Used**

Distributed locking and idempotent processing

Bounded thread pools and concurrency control

Task lifecycle state machines

Fault tolerance with retries and exponential backoff

Event-driven architecture using Kafka

**📐 Architecture Overview**

Client
  |
  | REST API
  v
Workflow Controller
  |
  v
Workflow Engine
  |
  |---> Redis (Distributed Locks + Idempotency)
  |
  |---> MySQL (Workflow & Task State)
  |
  |---> Thread Pool (Concurrent Task Execution)
  |
  |---> Kafka Producer (Task / Workflow Events)
                  |
                  v
           Kafka Topic (workflow-events)
                  |
                  v
           Kafka Consumer
                  |
                  v
        State Tracking / Monitoring

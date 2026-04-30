# System Design with Code

This repository contains system design exercises implemented in code. Each directory corresponds to a different system design problem, with detailed design documents and code implementations.

## Directory Structure

Each problem lives in its own top-level directory and follows the same layout:

```
<problem>/
├── design/   # design doc (requirements, architecture, ERDs, API, trade-offs)
├── code/     # backend implementation (services, infra, docker-compose)
└── frontend/ # optional UI (where present)
```

Current problems:

- [booking/](./booking) — online hotel booking system
- [chatting/](./chatting) — real-time chat system
- [food-ordering/](./food-ordering) — food ordering and delivery
- [url-shortner/](./url-shortner) — URL shortener

## Implementations

The current backends are written in **Java (Spring Boot)**. Read the per-problem `code/README.md` for run instructions.

## Future Work

- Implement projects services in **Go** to compare a goroutine/channel concurrency model against reactive Java.
- Implement select services in **PHP** (Laravel ) to demonstrate the same designs in a request-per-process runtime.
- Side-by-side benchmarks (latency, throughput, resource use) across language implementations of the same service.

# Multithreaded Data Processor

## Опис
Багатопотоковий застосунок для виконання складних обчислень.
Реалізовано:
- Використання `ExecutorService` та `Callable/Future`.
- Спільне використання даних через `ConcurrentHashMap` та `CopyOnWriteArrayList`.
- Синхронізація через `ReentrantLock` та `AtomicLong`.
- Демонстрація та вирішення проблеми **Deadlock**.

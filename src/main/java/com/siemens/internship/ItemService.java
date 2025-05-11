package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    // Thread-safe collection for processed items
    private final ConcurrentLinkedQueue<Item> processedItems = new ConcurrentLinkedQueue<>();

    // Executor for async tasks
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public Item save(Item item) {
        if (item.getEmail() != null && !isValidEmail(item.getEmail())) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }
    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
     /* Ensures:
            * - Thread-safe handling of shared state
     * - All items are processed before completion
     * - Exceptions are handled and logged
     */
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> processItem(id), executor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(voidResult ->
                        futures.stream()
                                .map(CompletableFuture::join) // Safe since all futures completed
                                .filter(item -> item != null)
                                .collect(Collectors.toList())
                )
                .exceptionally(ex -> {
                    System.err.println("Error processing items: " + ex.getMessage());
                    ex.printStackTrace();
                    return List.of(); // Return empty list on error
                });
    }

    /**
     * Processes an individual item.
     * @return processed item or null if processing failed
     */
    private Item processItem(Long id) {
        try {
            Thread.sleep(100); // Simulate processing
            Optional<Item> optionalItem = itemRepository.findById(id);
            if (optionalItem.isEmpty()) return null;

            Item item = optionalItem.get();
            item.setStatus("PROCESSED");
            return itemRepository.save(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while processing ID " + id);
        } catch (Exception e) {
            System.err.println("Error processing item " + id + ": " + e.getMessage());
        }
        return null;
    }
}


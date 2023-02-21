//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#pragma once

#include <condition_variable>
#include <mutex>
#include <queue>


/**
 * Blocking FIFO queue for passing work from one thread to another.
 *
 * The queue is unbounded.
 */
template<typename T>
class blocking_queue {
public:
    /**
     * Indicates if the queue is empty.
     *
     * @return 'true' if the queue is empty, 'false' if the queue contains elements
     */
    bool empty() const {
        std::lock_guard lock(guard);
        return queue.empty();
    }
    
    /**
     * Adds the item to the end of the queue.
     *
     * @param item item to add
     */
    void put(const T& item) {
        {
            std::lock_guard lock(guard);
            queue.push(item);
        }
        
        signal.notify_one();
    }
    
    /**
     * Takes the oldest item from the queue and removes it.
     *
     * Waits until an item is available.
     *
     * @return item removed from queue
     */
    T take() {
        std::unique_lock<std::mutex> lock(guard);
        while (queue.empty())
            signal.wait(lock);

        T item = queue.front();
        queue.pop();
        return item;
    }

private:
    std::queue<T> queue;
    mutable std::mutex guard;
    std::condition_variable signal;
};


#ifndef DAVESTEST_CIRCULARBUFFER_H
#define DAVESTEST_CIRCULARBUFFER_H

#include <atomic>
#include <thread>

constexpr size_t nextSlot(const size_t n, const size_t N) { return (n + 1) % N; }

template<typename T, size_t N>
class CircularBuffer {
private:
    alignas(64) std::atomic<size_t> head;
    alignas(64) std::atomic<size_t> tail;
    T buffer[N];

public:
    CircularBuffer() : head(0), tail(0) {}

    size_t size() const {
        const auto hd = head.load(std::memory_order::relaxed);
        const auto tl = tail.load(std::memory_order::relaxed);
        return (tl - hd + N) % N;
    }

    bool isFull() const {
        const auto hd = head.load(std::memory_order::relaxed);
        const auto currentTail = tail.load(std::memory_order::relaxed);
        return nextSlot(currentTail, N) == hd;
    }

    bool isEmpty() const {
        const auto hd = head.load(std::memory_order::relaxed);
        const auto tl = tail.load(std::memory_order::relaxed);
        return hd == tl;
    }

    bool push(T toPush) {
        const auto currentTail = tail.load(std::memory_order::relaxed);
        if (isFull()) {
            // the queue is full, so we return false to indicate failure
            return false;
        }
        buffer[currentTail] = toPush;
        tail.store(nextSlot(currentTail, N), std::memory_order_release);
        return true;
    }

    bool pop(T& valRef) {
        const auto hd = head.load(std::memory_order::relaxed);
        int spinWait = 0;
        while (isEmpty()) {
            if (spinWait < 1000) {
                spinWait++;
            } else if (spinWait < 2000) {
                std::this_thread::yield();
                spinWait++;
            } else {
                std::this_thread::sleep_for(std::chrono::milliseconds(1));
            }
        }

        valRef = buffer[hd];
        head.store(nextSlot(hd, N), std::memory_order_release);
        return true;
    }
};


#endif //DAVESTEST_CIRCULARBUFFER_H

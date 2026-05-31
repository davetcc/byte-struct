#include <iostream>
#include <thread>
#include <random>

#include "MockSimNative/TradingSystem.h"

TradingSystem tradingSystem;

int main() {

    std::jthread thread2([](const std::stop_token& stopToken) {
        while (!stopToken.stop_requested()) {
            auto prcMsg = PriceMessage();
            tradingSystem.getNextPrice(prcMsg);
            std::cout << "Price Rx " << prcMsg.getTicker() << " " << prcMsg.getSystem() << " " <<
                static_cast<double>(prcMsg.getTicks()) / 100.0 << std::endl;
        }
    });

    //thread2.request_stop();
    thread2.join();

    return 0;
}

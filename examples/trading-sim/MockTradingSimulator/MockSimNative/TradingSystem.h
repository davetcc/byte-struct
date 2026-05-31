
#ifndef DAVESTEST_TRADINGSYSTEM_H
#define DAVESTEST_TRADINGSYSTEM_H
#include <map>
#include <random>
#include <mutex>
#include <thread>

// On Windows we need to export all the functions we may use.
#if defined(_MSC_VER)
  #ifdef MOCKSIMNATIVE_EXPORTS
    #define TRADING_API __declspec(dllexport)
  #else
    #define TRADING_API __declspec(dllimport)
  #endif
#else
  #define TRADING_API
#endif

#include "CircularBuffer.h"
#include "MessageStructures.h"

constexpr const char* ourSourceName = "IntL";
constexpr const char* tickerNames[] = {"APPL", "CORN", "MSFT", "TSLA", "тикер", "FGBL", "STH26"};
constexpr const char* priceSourceNames[] = {"джерело", "ひらがな", ourSourceName};
constexpr int tickerSize = std::size(tickerNames);
constexpr int priceSourcesSize = std::size(priceSourceNames);

class TRADING_API TradingSystem {
private:
    std::random_device randomDevice;
    std::mt19937_64 RandomNumberGen;
    std::once_flag initFlag;
    std::map<std::string_view, StaticMessage> staticMap;
    CircularBuffer<PriceMessage, 50> priceBuffer;
    std::jthread priceTickerThread;

public:
    void init();
    TradingSystem() : RandomNumberGen(randomDevice()) {}
    /// Used by external actors such as gateway acquisition classes to inform the system
    /// of a new price.
    /// @param source the source for the price
    /// @param ticker the ticker
    /// @param priceTicks
    void priceReceivedFromGateway(const char* source, const char* ticker, uint64_t priceTicks);

    // this will be called by the Java layer to get the next price, it will spin block
    bool getNextPrice(PriceMessage& toCopyInto);

    const std::map<std::string_view, StaticMessage>& getMap() {
        ensureInitialized();
        return staticMap;
    }
protected:
    void ensureInitialized();
    void priceFromTradingSystem(const StaticMessage& whatTraded, uint64_t priceTicks);
    uint64_t getSecureRandomNumber(uint64_t min, uint64_t max);
    uint64_t getRandomizedPrice(const StaticMessage& whichContract);
};

#endif //DAVESTEST_TRADINGSYSTEM_H

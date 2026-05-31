//
// Created by Dave Cherry on 21/05/2026.
//

#include "TradingSystem.h"
#include <chrono>
#include <random>


uint64_t TradingSystem::getSecureRandomNumber(uint64_t min, uint64_t max) {
    std::uniform_int_distribution<uint64_t> dist(min, max);
    return dist(RandomNumberGen);
}

uint64_t TradingSystem::getRandomizedPrice(const StaticMessage& whichContract) {
    const auto midPoint = whichContract.getTicksPerPoint() * 99;
    const auto range = whichContract.getTicksPerPoint() * 2;
    return midPoint + getSecureRandomNumber(0, range);
}

uint64_t epochNow() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

/**
 * Initializes the static map within the trading system with predefined static messages.
 * For thread safety this init() method MUST be called BEFORE any thread operations.
 *
 * This method populates `staticMap` with a set of key-value pairs, where:
 * - The key is a ticker symbol as a string.
 * - The value is a `StaticMessage` object containing the ticker symbol and a `TradingInformation`
 *   object built with specific properties such as the trading venue, product type,
 *   and (optional) ticks per point.
 *
 * This method is responsible for preparing and configuring the system
 * with static information about specific tradable instruments before runtime operations.
 */
void TradingSystem::init() {
    staticMap["APPL"] = StaticMessage("APPL", TradingInformationBuilder().withVenue(TradableVenue::NASDAQ).withProductType(ProductType::STOCK).build());
    staticMap["CORN"] = StaticMessage("CORN", TradingInformationBuilder().withVenue(TradableVenue::CBOT).withProductType(ProductType::FUTURE).withTicksPerPoint(32).build());
    staticMap["MSFT"] = StaticMessage("MSFT", TradingInformationBuilder().withVenue(TradableVenue::NASDAQ).withProductType(ProductType::STOCK).build());
    staticMap["TSLA"] = StaticMessage("TSLA", TradingInformationBuilder().withVenue(TradableVenue::NASDAQ).withProductType(ProductType::STOCK).build());
    staticMap["тикер"] = StaticMessage("тикер", TradingInformationBuilder().withVenue(TradableVenue::EUREX).withProductType(ProductType::FUTURE).withTicksPerPoint(10).build());
    staticMap["FGBL"] = StaticMessage("FGBL", TradingInformationBuilder().withVenue(TradableVenue::EUREX).withProductType(ProductType::FUTURE).build());
    staticMap["STH26"] = StaticMessage("STH26", TradingInformationBuilder().withVenue(TradableVenue::LIFFE).withProductType(ProductType::FUTURE).build());

    priceTickerThread = std::jthread([this](const std::stop_token& stopToken) {
        while (!stopToken.stop_requested()) {
            for (const auto& [ticker, staticMessage] : staticMap) {
                const auto price = getRandomizedPrice(staticMessage);
                priceReceivedFromGateway(priceSourceNames[price % priceSourcesSize], ticker.data(), price);
                std::this_thread::sleep_for(std::chrono::microseconds(20));
            }
        }
    });
}

void TradingSystem::ensureInitialized() {
    std::call_once(initFlag, [&] {
        init();
    });
}

void TradingSystem::priceReceivedFromGateway(const char* source, const char* ticker, uint64_t priceTicks) {
    ensureInitialized();
    priceBuffer.push(PriceMessage(ticker, source, epochNow(), priceTicks));
}

bool TradingSystem::getNextPrice(PriceMessage& toCopyInto) {
    ensureInitialized();
    return priceBuffer.pop(toCopyInto);
}

void TradingSystem::priceFromTradingSystem(const StaticMessage& whatTraded, uint64_t priceTicks) {
    ensureInitialized();
    priceBuffer.push(PriceMessage(whatTraded.getTicker(), ourSourceName, epochNow(), priceTicks));
}

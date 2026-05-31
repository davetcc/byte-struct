#ifndef DAVESTEST_MESSAGESTRUCTURES_H
#define DAVESTEST_MESSAGESTRUCTURES_H

#include <cstdint>
#include <string>

/**
 * A kind of marker interface that suggests a class has a binary representation that can be published as is. It generally
 * means it is struct wrapped as a class, and has trivial construction and copy semantics.
 */
class BinaryPublishableAsSelf {
};

enum TradableVenue: uint8_t {
    NYSE = 1,
    NASDAQ = 2,
    AMEX = 3,
    LSE = 4,
    CBOT = 5,
    LIFFE = 6,
    EUREX = 7,
    INTERNAL = 8,
    UNKNOWN = 0
};

enum ProductType: uint8_t {
    STOCK = 0,
    FUTURE = 1,
};

/**
 * A bit packed structure with the core trading information compacted into a 4 byte block.
 */
struct TradingInformation {
    uint32_t isTradable: 1;      //0
    uint32_t isPreMarket: 1;     //1
    uint32_t flaggedAsBlocked: 1;//2
    uint32_t tradableVenue : 6;  //3-8
    uint32_t productType : 6;    //9-14
    uint32_t ticksPerPoint : 16; //15-31
};

/**
 * @class StaticMessage
 *
 * @brief Represents a static message containing metadata about a financial instrument.
 *
 * The StaticMessage class encapsulates essential information related to financial instruments,
 * including their ticker and trading details. It extends the BinaryPublishableAsSelf base
 * class and provides methods to access key instrument-specific attributes.
 *
 * The class is designed to be lightweight and efficient, with all trading information
 * stored in a compact TradingInformation struct. Accessor methods offer details such
 * as product type, tradable status, venue, and other metadata indicators, making it
 * suitable for use in real-time trading systems.
 */
class StaticMessage : public BinaryPublishableAsSelf {
private:
    char ticker[32];
    TradingInformation tradeInfo;
public:
    StaticMessage() = default;
    StaticMessage(const StaticMessage& other) = default;
    StaticMessage& operator=(const StaticMessage& other) = default;
    StaticMessage(const char* ticker, TradingInformation tradeInfo)
        : ticker(), tradeInfo(tradeInfo) {
        strncpy(this->ticker, ticker, sizeof(this->ticker) - 1);
        this->ticker[sizeof(this->ticker) - 1] = '\0';
    }

    [[nodiscard]] const char* getTicker() const { return ticker; }
    [[nodiscard]] uint32_t getTicksPerPoint() const { return tradeInfo.ticksPerPoint; }
    [[nodiscard]] ProductType getProductType() const { return static_cast<ProductType>(tradeInfo.productType); }
    [[nodiscard]] TradableVenue getTradableVenue() const { return static_cast<TradableVenue>(tradeInfo.tradableVenue); }
    [[nodiscard]] bool isTradable() const { return static_cast<ProductType>(tradeInfo.isTradable); }
    [[nodiscard]] bool isPreMarket() const { return static_cast<ProductType>(tradeInfo.isPreMarket); }
    [[nodiscard]] bool isFlaggedAsBlocked() const { return static_cast<ProductType>(tradeInfo.flaggedAsBlocked); }
};

/**
 * @class TradingInformationBuilder
 *
 * @brief A builder class to construct instances of TradingInformation in a flexible and customizable manner.
 *
 * This class provides a fluent interface for setting various properties of the TradingInformation structure,
 * allowing for a more readable and maintainable way to create configured instances of TradingInformation.
 */
class TradingInformationBuilder {
    TradingInformation info{};
public:
    TradingInformationBuilder() {
        info.isTradable = true;
        info.ticksPerPoint = 100;
    }

    TradingInformationBuilder& withTicksPerPoint(uint32_t tpp) {
        info.ticksPerPoint = tpp;
        return *this;
    }

    TradingInformationBuilder& withTradingOptions(const bool tradable, const bool preMarket) {
        info.isTradable = tradable;
        info.isPreMarket = preMarket;
        return *this;
    }
    TradingInformationBuilder& withProductType(const ProductType productType) {
        info.productType = productType;
        return *this;
    }

    TradingInformationBuilder& withVenue(const TradableVenue venue) {
        info.tradableVenue = venue;
        return *this;
    }

    [[nodiscard]] TradingInformation build() const {
        return info;
    }
};

class PriceMessage : public BinaryPublishableAsSelf {
private:
    char ticker[32]; // E.G. vod.l
    char system[16]; // E.G. Internal
    uint64_t millisEpoch;
    uint64_t ticks;
public:
    PriceMessage() = default;
    PriceMessage(const PriceMessage&) = default;
    PriceMessage& operator=(const PriceMessage&) = default;
    PriceMessage(const char* ticker, const char* system, const uint64_t millisEpoch, const uint64_t ticks) : millisEpoch(millisEpoch), ticks(ticks) {
        strncpy(this->ticker, ticker, sizeof(this->ticker) - 1);
        this->ticker[sizeof(this->ticker) - 1] = '\0';
        strncpy(this->system, system, sizeof(this->system) - 1);
        this->system[sizeof(this->system) - 1] = '\0';
    }

    [[nodiscard]] const char* getTicker() const { return ticker; }
    [[nodiscard]] const char* getSystem() const { return system; }
    [[nodiscard]] uint64_t getMillisEpoch() const { return millisEpoch; }
    [[nodiscard]] uint64_t getTicks() const { return ticks; }
};


enum Side {
    SIDE_SIM_BUY, SIDE_SIM_SELL
};

enum TradeType {
    TRADE_TYPE_SIM_MARKET, TRADE_TYPE_SIM_LIMIT
};

struct TradeParams {
    uint32_t side: 2;
    uint32_t tradeType: 6;
    uint32_t venue: 6;
    TradeParams() = default;
    TradeParams(const Side side, const TradeType tradeType) : side(side), tradeType(tradeType), venue(INTERNAL) {}
};


class TradeBookingMessage : public BinaryPublishableAsSelf {
public:
    TradeBookingMessage(): ticker{}, price(0), quantity(0), tradeInfo(SIDE_SIM_BUY, TRADE_TYPE_SIM_MARKET) {}
    TradeBookingMessage(const char* ticker, uint64_t price, uint32_t quantity, const TradeParams& tradeInfo)
        : ticker{}, price(price),
          quantity(quantity),
          tradeInfo(tradeInfo) {
        strncpy(this->ticker, ticker, sizeof(this->ticker) - 1);
        this->ticker[sizeof(this->ticker) - 1] = '\0';
    }
    TradeBookingMessage(const TradeBookingMessage& other) = default;
    TradeBookingMessage& operator=(const TradeBookingMessage& other) = default;

    [[nodiscard]] const char* getTicker() const { return ticker; }
    [[nodiscard]] uint64_t getPrice() const { return price; }
    [[nodiscard]] uint32_t getQuantity() const { return quantity; }
    [[nodiscard]] Side getSide() const { return static_cast<Side>(tradeInfo.side); }
    [[nodiscard]] TradableVenue getVenue() const { return static_cast<TradableVenue>(tradeInfo.venue); }
    [[nodiscard]] TradeType getTradeType() const { return static_cast<TradeType>(tradeInfo.tradeType); }
private:
    char ticker[32];
    uint64_t price;
    uint32_t quantity;
    TradeParams tradeInfo;
};

#endif //DAVESTEST_MESSAGESTRUCTURES_H

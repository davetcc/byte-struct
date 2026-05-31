import {useEffect, useState, memo} from "react";

export interface ProductStaticPrice {
    ticker: string;
    blocked: boolean;
    preMarket: boolean;
    priceType: string;
    tradingVenue: string;
    price: string;
    source: string;
}

interface PriceUpdate {
    ticker: string;
    price: string;
    source: string;
}

// A simple event emitter to distribute price updates without re-rendering the whole list
class PriceEmitter extends EventTarget {
    emit(update: PriceUpdate) {
        this.dispatchEvent(new CustomEvent('update', { detail: update }));
    }
}

const priceEmitter = new PriceEmitter();

const PriceCard = memo(({ staticInfo }: { staticInfo: ProductStaticPrice }) => {
    const [price, setPrice] = useState(staticInfo.price);
    const [source, setSource] = useState(staticInfo.source);

    useEffect(() => {
        const handler = (event: any) => {
            const update = event.detail as PriceUpdate;
            if (update.ticker === staticInfo.ticker) {
                setPrice(update.price);
                setSource(update.source);
            }
        };

        priceEmitter.addEventListener('update', handler);
        return () => priceEmitter.removeEventListener('update', handler);
    }, [staticInfo.ticker]);

    return (
        <div className="card">
            <h3>{staticInfo.ticker}</h3>
            <p>Venue: {staticInfo.tradingVenue}</p>
            <p>Type: {staticInfo.priceType}</p>
            <div className="price-row">
                <span className="price">{price ?? "N/A"}</span>
                <span className="source">{source ?? "Not Available"}</span>
            </div>
        </div>
    );
});

export function ShowPricesPage() {
    const [productInfo, setProductInfo] = useState<ProductStaticPrice[]>([]);

    useEffect(() => {
        fetch("/api/trading/acquireStatic", {
            method: "GET", headers: {"Content-Type": "application/json"}
        }).then(response => {
            if(response.ok) {
                return response.json()
            } else {
                throw new Error("Failed to fetch price static data")
            }
        }).then(data => {
            setProductInfo(data);
        }).catch(err => console.error(err));

        const eventSource = new EventSource('/api/trading/priceUpdates');
        
        eventSource.addEventListener('price-update', (event) => {
            try {
                const priceData = JSON.parse(event.data) as PriceUpdate;
                priceEmitter.emit(priceData);
            } catch (err) {
                console.error("Error parsing price update data", err);
            }
        });

        eventSource.onerror = (err) => {
            console.error("EventSource failed:", err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, []);

    return (
        <section id="center">
            <h2>Market Prices</h2>
            <div className="card-container">
                {productInfo.map((element: ProductStaticPrice) => (
                    <PriceCard key={element.ticker} staticInfo={element} />
                ))}
            </div>
        </section>
    );
}
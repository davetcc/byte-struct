import {useEffect, useState} from "react";

interface StatisticsData {
    messagesReceivedFromCpp: number;
    numberOfConflations: number;
    currentJavaHeapTotal: number;
    totalJvmRuntimeSoFar: number;
    distributionBlocking: number;
}

export default function ShowStatisticsPage() {
    const [stats, setStats] = useState<StatisticsData>({
        messagesReceivedFromCpp: 0,
        numberOfConflations: 0,
        currentJavaHeapTotal: 0,
        totalJvmRuntimeSoFar: 0,
        distributionBlocking: 0,
    });

    useEffect(() => {
        const eventSource = new EventSource('/api/trading/stats');

        eventSource.addEventListener('stats', (event) => {
            try {
                const data = JSON.parse(event.data) as StatisticsData;
                setStats(data);
            } catch (err) {
                console.error("Error parsing statistics update data", err);
            }
        });

        eventSource.onerror = (err) => {
            console.error("Statistics EventSource failed:", err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, []);

    const formatMemory = (bytes: number) => {
        if (bytes === 0) return "0 B";
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    };

    const formatUptime = (ms: number) => {
        const seconds = Math.floor((ms / 1000) % 60);
        const minutes = Math.floor((ms / (1000 * 60)) % 60);
        const hours = Math.floor((ms / (1000 * 60 * 60)) % 24);
        const days = Math.floor(ms / (1000 * 60 * 60 * 24));

        return `${days}d ${hours}h ${minutes}m ${seconds}s`;
    };

    return (
        <section id="center">
            <div className="statistics-page">
                <h2>System Statistics</h2>
                <div className="stats-container">
                    <div className="stat-pair">
                        <span className="stat-label">JVM memory used:</span>
                        <span className="stat-value">{formatMemory(stats.currentJavaHeapTotal)}</span>
                    </div>
                    <div className="stat-pair">
                        <span className="stat-label">Cpp Messages Received:</span>
                        <span className="stat-value">{stats.messagesReceivedFromCpp.toLocaleString()}</span>
                    </div>
                    <div className="stat-pair">
                        <span className="stat-label">Avg Messages/sec:</span>
                        <span className="stat-value">{(stats.messagesReceivedFromCpp / (stats.totalJvmRuntimeSoFar / 1000))
                            .toFixed(1)}</span>
                    </div>
                    <div className="stat-pair">
                        <span className="stat-label">Collations performed:</span>
                        <span className="stat-value">{stats.numberOfConflations.toLocaleString()}</span>
                    </div>
                    <div className="stat-pair">
                        <span className="stat-label">Distribution blocks:</span>
                        <span className="stat-value">{stats.distributionBlocking.toLocaleString()}</span>
                    </div>
                    <div className="stat-pair">
                        <span className="stat-label">Collation ratio:</span>
                        <span className="stat-value">{((stats.numberOfConflations / stats.messagesReceivedFromCpp)*100).toLocaleString()}%</span>
                    </div>
                    <div className="stat-pair">
                        <span className="stat-label">Uptime:</span>
                        <span className="stat-value">{formatUptime(stats.totalJvmRuntimeSoFar)}</span>
                    </div>
                </div>
            </div>
        </section>
    );
}
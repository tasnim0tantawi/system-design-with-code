"""
Generate architecture and flow diagrams for the URL Shortener using the
`diagrams` Python library (https://diagrams.mingrammer.com).

Run from the design/ directory:
    python3 diagrams/generate.py

Outputs PNG files into the same diagrams/ folder.
"""

import os

from diagrams import Cluster, Diagram, Edge
from diagrams.onprem.client import Client, Users
from diagrams.onprem.database import Cassandra, PostgreSQL
from diagrams.onprem.inmemory import Redis
from diagrams.onprem.queue import Kafka
from diagrams.programming.framework import Spring


HERE = os.path.dirname(os.path.abspath(__file__))
GRAPH_ATTR = {
    "fontsize": "16",
    "bgcolor": "white",
    "pad": "0.5",
    "splines": "spline",
}
NODE_ATTR = {"fontsize": "12"}


# ──────────────────────────────────────────────────────────────────
# 1. System Architecture Overview
# ──────────────────────────────────────────────────────────────────
def system_overview():
    with Diagram(
        "URL Shortener -- Architecture Overview",
        filename=os.path.join(HERE, "01-system-overview"),
        show=False,
        direction="TB",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        users = Users("Users / Browsers")

        with Cluster("url-shortener service (Spring Boot 4 + WebFlux)"):
            shorten = Spring("ShortenerController\nPOST /api/urls")
            redirect = Spring("RedirectController\nGET /{shortCode}")
            stats = Spring("AnalyticsController\nGET /api/urls/{c}/stats")
            allocator = Spring("TokenRangeAllocator\n(in-memory range)")
            producer = Spring("ClickEventProducer\n(fire-and-forget)")
            consumer = Spring("ClickEventConsumer\n@KafkaListener")

        with Cluster("Stores"):
            pg = PostgreSQL("PostgreSQL\nurl_mapping\ntoken_range\n(Liquibase)")
            redis = Redis("Redis\nurl:{code} -> longUrl\nTTL 60m")
            cass = Cassandra("Cassandra\nclick_events\nclick_counts (counter)")

        kafka = Kafka("Kafka\nclick-events")

        users >> Edge(label="HTTPS") >> [shorten, redirect, stats]

        shorten >> allocator
        allocator >> Edge(label="UPDATE token_range\nRETURNING (per 1000 ids)") >> pg
        shorten >> Edge(label="INSERT url_mapping") >> pg
        shorten >> Edge(label="SET url:{code}") >> redis

        redirect >> Edge(label="GET url:{code}", color="darkgreen") >> redis
        redirect >> Edge(label="SELECT (cache miss)", color="grey") >> pg
        redirect >> Edge(label="publish click", color="orange") >> producer
        producer >> Edge(label="produce") >> kafka

        kafka >> Edge(label="consume", color="darkblue") >> consumer
        consumer >> Edge(label="INSERT event\nUPDATE counter") >> cass

        stats >> Edge(label="SELECT click_counts") >> cass


# ──────────────────────────────────────────────────────────────────
# 2. Token Range Allocator (sequence-style flow)
# ──────────────────────────────────────────────────────────────────
def token_allocator():
    with Diagram(
        "Token Range Allocator (counter-based ID generation)",
        filename=os.path.join(HERE, "02-token-allocator"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        clients = Client("N concurrent\nshorten() calls")

        with Cluster("Per-pod in-memory state"):
            atomic = Spring("AtomicLong next\n+ endExclusive")
            inflight = Spring("AtomicReference\n<Mono> inFlight\n(coalesces refresh)")

        pg = PostgreSQL("PostgreSQL token_range\nUPDATE next_value += 1000\nRETURNING next_value")

        clients >> Edge(label="next()") >> atomic
        atomic >> Edge(label="if exhausted", color="firebrick") >> inflight
        inflight >> Edge(label="single round-trip\nshared by all callers") >> pg
        pg >> Edge(label="returns end of new range\n[start, start+1000)", color="darkgreen") >> atomic
        atomic >> Edge(label="hand out IDs in-memory\nbase62-encode -> short code", color="darkblue") >> clients


# ──────────────────────────────────────────────────────────────────
# 3. Write Flow (shorten)
# ──────────────────────────────────────────────────────────────────
def write_flow():
    with Diagram(
        "Write Flow -- Shorten a URL",
        filename=os.path.join(HERE, "03-write-flow"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        client = Client("Client")
        app = Spring("ShortenerService")
        token = Spring("TokenRangeAllocator")
        pg = PostgreSQL("PostgreSQL\nurl_mapping +\ntoken_range")
        redis = Redis("Redis\nurl:{code}")

        client >> Edge(label="1. POST /api/urls\n{longUrl}") >> app
        app >> Edge(label="2. next()") >> token
        token >> Edge(label="(rare) UPDATE token_range\nRETURNING", style="dashed", color="grey") >> pg
        token >> Edge(label="3. id (e.g. 12345)") >> app
        app >> Edge(label="4. INSERT url_mapping\n(short_code, long_url, expires_at)", color="darkgreen") >> pg
        app >> Edge(label="5. SET url:{code} EX 3600", color="darkgreen") >> redis
        app >> Edge(label="6. 201 {shortCode, shortUrl, expiresAt}") >> client


# ──────────────────────────────────────────────────────────────────
# 4. Read Flow (redirect)
# ──────────────────────────────────────────────────────────────────
def read_flow():
    with Diagram(
        "Read Flow -- Redirect (cache-aside + fire-and-forget click)",
        filename=os.path.join(HERE, "04-read-flow"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        client = Client("Browser")
        app = Spring("RedirectController")
        redis = Redis("Redis\nurl:{code}")
        pg = PostgreSQL("PostgreSQL\nurl_mapping")
        producer = Spring("ClickEventProducer")
        kafka = Kafka("Kafka\nclick-events")

        client >> Edge(label="1. GET /{shortCode}") >> app
        app >> Edge(label="2. GET url:{code}") >> redis
        redis >> Edge(label="hit -> longUrl", color="darkgreen") >> app
        app >> Edge(label="(miss) SELECT long_url, expires_at\nWHERE short_code=?", color="grey", style="dashed") >> pg
        app >> Edge(label="3. publish click\n(fire-and-forget)", color="orange") >> producer
        producer >> kafka
        app >> Edge(label="4. 302 Location: longUrl", color="darkblue") >> client


# ──────────────────────────────────────────────────────────────────
# 5. Analytics Pipeline (Kafka -> Cassandra)
# ──────────────────────────────────────────────────────────────────
def analytics_flow():
    with Diagram(
        "Analytics Pipeline -- Kafka to Cassandra",
        filename=os.path.join(HERE, "05-analytics-flow"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        with Cluster("Producer side (every redirect)"):
            redirect = Spring("Redirect handler")
            producer = Spring("ClickEventProducer")

        kafka = Kafka("Kafka\nclick-events\n(key = short_code)")

        with Cluster("Consumer side (@KafkaListener)"):
            consumer = Spring("ClickEventConsumer")

        with Cluster("Cassandra (keyspace url_analytics)"):
            events = Cassandra("click_events\npartition: short_code\ncluster: clicked_at DESC, event_id")
            counts = Cassandra("click_counts\n(counter)\nper (short_code, day)")

        api = Spring("AnalyticsController\nGET /api/urls/{c}/stats")

        redirect >> Edge(label="emit") >> producer
        producer >> Edge(label="produce") >> kafka
        kafka >> Edge(label="consume\n(group url-shortener-analytics)") >> consumer
        consumer >> Edge(label="INSERT full event row") >> events
        consumer >> Edge(label="UPDATE click_counts\nSET count = count + 1\nWHERE short_code=? AND bucket_day=?",
                         color="purple") >> counts
        api >> Edge(label="SELECT bucket_day, count\nFROM click_counts WHERE short_code=?",
                    color="darkgreen") >> counts


# ──────────────────────────────────────────────────────────────────
# 6. Storage Layout
# ──────────────────────────────────────────────────────────────────
def storage_layout():
    with Diagram(
        "Polyglot Storage -- one store per access pattern",
        filename=os.path.join(HERE, "06-storage-layout"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        app = Spring("url-shortener")

        with Cluster("Strongly consistent (transactional)"):
            pg = PostgreSQL("PostgreSQL\n+ Liquibase\n----\nurl_mapping (short_code PK)\ntoken_range (counter row)")

        with Cluster("Cache (low-latency reads)"):
            redis = Redis("Redis\n----\nurl:{shortCode} -> longUrl\nTTL 60 min, cache-aside")

        with Cluster("Streaming (decoupling)"):
            kafka = Kafka("Kafka\n----\nclick-events topic\nkey = short_code\n(per-URL ordering)")

        with Cluster("Analytics (write-heavy)"):
            cass = Cassandra("Cassandra\n----\nclick_events (raw log)\nclick_counts (COUNTER)")

        app >> Edge(label="ACID writes") >> pg
        app >> Edge(label="read-through cache") >> redis
        app >> Edge(label="fire-and-forget") >> kafka
        kafka >> Edge(label="@KafkaListener") >> cass
        app >> Edge(label="aggregate reads", color="darkgreen") >> cass


if __name__ == "__main__":
    print("Generating diagrams into", HERE)
    system_overview()
    token_allocator()
    write_flow()
    read_flow()
    analytics_flow()
    storage_layout()
    print("Done.")

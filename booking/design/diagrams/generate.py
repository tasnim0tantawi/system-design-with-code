"""
Generate architecture and flow diagrams for the Hotel Booking System using
the `diagrams` Python library (https://diagrams.mingrammer.com).

Run from the design/ directory:
    python3 diagrams/generate.py

Outputs PNG files into the same diagrams/ folder.
"""

import os

from diagrams import Cluster, Diagram, Edge
from diagrams.onprem.client import Client, Users
from diagrams.onprem.compute import Server
from diagrams.onprem.database import Cassandra, PostgreSQL
from diagrams.onprem.inmemory import Redis
from diagrams.onprem.network import Internet
from diagrams.onprem.queue import Kafka
from diagrams.elastic.elasticsearch import Elasticsearch as Meilisearch  # closest icon
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
        "Hotel Booking System -- Architecture Overview",
        filename=os.path.join(HERE, "01-system-overview"),
        show=False,
        direction="TB",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        users = Users("Users / Browsers")

        with Cluster("Edge"):
            gateway = Spring("API Gateway\n(WebFlux + JWT filter)")

        with Cluster("Service Discovery"):
            eureka = Server("Eureka Server")

        with Cluster("Microservices (Spring Boot 4 + WebFlux)"):
            auth = Spring("auth-service\n:8081")
            hotel = Spring("hotel-service\n:8082")
            booking = Spring("booking-service\n:8083")
            search = Spring("search-service\n:8084")
            review = Spring("review-service\n:8085")
            notif = Spring("notification-service\n:8086")

        with Cluster("Data Stores"):
            postgres = PostgreSQL("PostgreSQL\n5 logical DBs")
            redis = Redis("Redis\ncache + tokens")
            cassandra = Cassandra("Cassandra\nbooking_history")
            meili = Meilisearch("MeiliSearch\nhotel index")

        with Cluster("Messaging"):
            kafka = Kafka("Kafka (KRaft)")

        users >> Edge(label="HTTPS") >> gateway
        gateway >> Edge(label="lb://", style="dashed", color="grey") >> eureka

        gateway >> Edge() >> [auth, hotel, booking, search, review, notif]

        auth >> postgres
        hotel >> postgres
        booking >> postgres
        review >> postgres
        notif >> postgres

        auth >> redis
        hotel >> redis
        search >> redis
        gateway >> Edge(label="blocklist check", color="firebrick") >> redis

        booking >> cassandra
        search >> meili

        booking >> Edge(label="booking.*", color="darkgreen") >> kafka
        review >> Edge(label="review.*", color="darkgreen") >> kafka
        hotel >> Edge(label="hotel.*", color="darkgreen") >> kafka
        kafka >> Edge(label="consume", color="darkblue") >> notif
        kafka >> Edge(label="consume", color="darkblue") >> search


# ──────────────────────────────────────────────────────────────────
# 2. Auth & Token Flow (login → access+refresh → refresh → logout)
# ──────────────────────────────────────────────────────────────────
def auth_token_flow():
    with Diagram(
        "Auth: Access + Refresh Token Lifecycle",
        filename=os.path.join(HERE, "02-auth-token-flow"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        client = Client("Client")
        gateway = Spring("API Gateway")
        auth = Spring("auth-service")
        redis = Redis("Redis")
        pg = PostgreSQL("auth DB")

        client >> Edge(label="1. POST /login\n{email, pw}", color="darkblue") >> gateway
        gateway >> Edge(color="darkblue") >> auth
        auth >> Edge(label="verify hash", color="grey") >> pg
        auth >> Edge(label="2. SET refresh:{tok}\nTTL=7d", color="darkgreen") >> redis
        auth >> Edge(label="3. {access JWT (15m),\n   refresh tok (7d)}", color="darkblue") >> client

        client >> Edge(label="4. POST /refresh\n{refresh}", color="orange") >> gateway
        gateway >> Edge(color="orange") >> auth
        auth >> Edge(label="GETDEL refresh:{tok}\n(rotation)", color="orange") >> redis
        auth >> Edge(label="5. new {access, refresh}", color="orange") >> client

        client >> Edge(label="6. POST /logout\nAuthz: Bearer access\n+ {refresh}", color="firebrick") >> gateway
        gateway >> Edge(color="firebrick") >> auth
        auth >> Edge(label="SET blocklist:{jti}\nTTL=remaining\nDEL refresh:{tok}", color="firebrick") >> redis


# ──────────────────────────────────────────────────────────────────
# 3. Per-Request Token Validation at Gateway
# ──────────────────────────────────────────────────────────────────
def gateway_request_path():
    with Diagram(
        "Per-Request: Gateway Token Validation",
        filename=os.path.join(HERE, "03-gateway-request-path"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        client = Client("Client")
        gateway = Spring("API Gateway\nJwtAuthFilter")
        redis = Redis("Redis\nblocklist")
        downstream = Spring("Downstream Service\n(hotel/booking/...)")

        client >> Edge(label="GET /api/...\nAuthz: Bearer access") >> gateway
        gateway >> Edge(label="HS256 verify\nparse jti", style="dashed") >> gateway
        gateway >> Edge(label="EXISTS blocklist:{jti}", color="firebrick") >> redis
        gateway >> Edge(
            label="strip Authz,\nadd X-User-Id / X-User-Role",
            color="darkblue"
        ) >> downstream
        downstream >> Edge(label="trusts gateway headers", color="darkgreen") >> client


# ──────────────────────────────────────────────────────────────────
# 4. Booking Flow (create → pay → kafka → notification + history)
# ──────────────────────────────────────────────────────────────────
def booking_flow():
    with Diagram(
        "Booking & Payment Flow",
        filename=os.path.join(HERE, "04-booking-flow"),
        show=False,
        direction="TB",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        client = Client("User")

        with Cluster("Edge"):
            gateway = Spring("API Gateway")

        with Cluster("Booking"):
            booking = Spring("booking-service")
            booking_pg = PostgreSQL("bookings DB")
            cassandra = Cassandra("booking_history\n(append-only)")

        with Cluster("Hotel"):
            hotel = Spring("hotel-service")
            hotel_pg = PostgreSQL("hotels DB\n(room_availability)")

        kafka = Kafka("Kafka\nbooking-events")

        with Cluster("Notification"):
            notif = Spring("notification-service")
            notif_pg = PostgreSQL("notifications DB")

        client >> Edge(label="1. POST /bookings") >> gateway >> booking
        booking >> Edge(label="2. INSERT booking\nstatus=PENDING") >> booking_pg
        booking >> Edge(label="3. POST /internal/availability/decrement") >> hotel
        hotel >> Edge(label="UPDATE available_count\nWHERE > 0") >> hotel_pg

        client >> Edge(label="4. POST /bookings/{id}/payment") >> gateway >> booking
        booking >> Edge(label="5. status=CONFIRMED") >> booking_pg
        booking >> Edge(label="6. INSERT history\nkey=(userId, ts, id)", color="purple") >> cassandra
        booking >> Edge(label="7. publish booking.confirmed", color="darkgreen") >> kafka

        kafka >> Edge(label="@KafkaListener", color="darkblue") >> notif
        notif >> Edge(label="INSERT notification") >> notif_pg


# ──────────────────────────────────────────────────────────────────
# 5. Search Index Sync (hotel events → kafka → indexer → meilisearch)
# ──────────────────────────────────────────────────────────────────
def search_index_sync():
    with Diagram(
        "Search Index Synchronization (CQRS Read Model)",
        filename=os.path.join(HERE, "05-search-index-sync"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        manager = Client("Manager")
        guest = Client("Guest")

        with Cluster("Write Path"):
            gateway_w = Spring("API Gateway")
            hotel = Spring("hotel-service")
            hotel_pg = PostgreSQL("hotels DB\n(source of truth)")

        kafka = Kafka("Kafka\nhotel-events")

        with Cluster("Read Path"):
            gateway_r = Spring("API Gateway")
            search = Spring("search-service")
            redis = Redis("Redis\nquery cache 60s")
            meili = Meilisearch("MeiliSearch\nhotel index")

        manager >> Edge(label="POST /hotels") >> gateway_w >> hotel
        hotel >> Edge(label="INSERT hotel") >> hotel_pg
        hotel >> Edge(label="hotel.created\n{id, name, location, ...}", color="darkgreen") >> kafka

        kafka >> Edge(label="HotelIndexer\n@KafkaListener", color="darkblue") >> search
        search >> Edge(label="POST /indexes/hotels/documents", color="purple") >> meili

        guest >> Edge(label="GET /api/hotels/search?...") >> gateway_r >> search
        search >> Edge(label="GET cache key", color="orange") >> redis
        search >> Edge(label="POST /indexes/hotels/search\n(on cache miss)", color="purple") >> meili


# ──────────────────────────────────────────────────────────────────
# 6. Database Ownership / Polyglot Persistence
# ──────────────────────────────────────────────────────────────────
def database_ownership():
    with Diagram(
        "Polyglot Persistence -- Database per Service",
        filename=os.path.join(HERE, "06-database-ownership"),
        show=False,
        direction="LR",
        graph_attr=GRAPH_ATTR,
        node_attr=NODE_ATTR,
    ):
        with Cluster("PostgreSQL (R2DBC)"):
            pg_auth = PostgreSQL("auth\nusers, roles")
            pg_hotel = PostgreSQL("hotels\nrooms, availability")
            pg_book = PostgreSQL("bookings\nactive bookings")
            pg_review = PostgreSQL("reviews\nreviews + aggregates")
            pg_notif = PostgreSQL("notifications\nuser inbox")

        with Cluster("Specialized Stores"):
            cassandra = Cassandra("Cassandra\nbooking_history\n(append-only)")
            meili = Meilisearch("MeiliSearch\nhotel search index\n(read model)")
            redis = Redis("Redis\nhotel cache + search cache\nrefresh tokens + blocklist")

        Spring("auth-service") >> [pg_auth, redis]
        Spring("hotel-service") >> [pg_hotel, redis]
        Spring("booking-service") >> [pg_book, cassandra]
        Spring("review-service") >> [pg_review]
        Spring("notification-service") >> [pg_notif]
        Spring("search-service") >> [meili, redis]
        Spring("api-gateway") >> [redis]


if __name__ == "__main__":
    print("Generating diagrams into", HERE)
    system_overview()
    auth_token_flow()
    gateway_request_path()
    booking_flow()
    search_index_sync()
    database_ownership()
    print("Done.")

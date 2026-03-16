#!/usr/bin/env python3
"""Create randomized orders against the order service."""

from __future__ import annotations

import argparse
import json
import random
import string
import sys
import threading
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any


FIRST_NAMES = [
    "Alex",
    "Avery",
    "Casey",
    "Daniel",
    "Emma",
    "Finn",
    "Harper",
    "Jordan",
    "Lea",
    "Mia",
    "Noah",
    "Olivia",
    "Paul",
    "Sofia",
    "Theo",
]

LAST_NAMES = [
    "Anderson",
    "Becker",
    "Fischer",
    "Garcia",
    "Hoffmann",
    "Keller",
    "Lopez",
    "Meyer",
    "Nguyen",
    "Parker",
    "Quinn",
    "Schmidt",
    "Taylor",
    "Wagner",
    "Zimmermann",
]

NOTES = [
    "Leave the parcel at reception.",
    "Please ring once.",
    "Call before delivery.",
    "Front door code is 2048.",
    "Deliver during office hours only.",
    "No special delivery instructions.",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Send randomized order creation requests."
    )
    parser.add_argument(
        "count",
        type=int,
        nargs="?",
        default=1000,
        help="Number of orders to create.",
    )
    parser.add_argument(
        "--order-base-url",
        default="http://localhost:8081",
        help="Base URL of the order service. Default: %(default)s",
    )
    parser.add_argument(
        "--inventory-base-url",
        default="http://localhost:8082",
        help="Base URL of the inventory service. Used to discover products. Default: %(default)s",
    )
    parser.add_argument(
        "--customer-count",
        type=int,
        default=50,
        help="Size of the reusable customer pool. Default: %(default)s",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=8,
        help="Number of concurrent order requests. Default: %(default)s",
    )
    parser.add_argument(
        "--min-items",
        type=int,
        default=2,
        help="Minimum number of products per order. Default: %(default)s",
    )
    parser.add_argument(
        "--max-items",
        type=int,
        default=4,
        help="Maximum number of products per order. Default: %(default)s",
    )
    parser.add_argument(
        "--min-quantity",
        type=int,
        default=1,
        help="Minimum quantity per line item. Default: %(default)s",
    )
    parser.add_argument(
        "--max-quantity",
        type=int,
        default=10,
        help="Maximum quantity per line item. Default: %(default)s",
    )
    parser.add_argument(
        "--currency",
        default="EUR",
        help="Currency value for created orders. Default: %(default)s",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Optional random seed for reproducible runs.",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        help="HTTP timeout in seconds. Default: %(default)s",
    )
    return parser.parse_args()


def request_json(
    method: str,
    url: str,
    *,
    payload: dict[str, Any] | None = None,
    timeout: float,
) -> Any:
    data = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read()
        if not body:
            return None
        return json.loads(body.decode("utf-8"))


def build_customer_pool(count: int, rng: random.Random) -> list[dict[str, Any]]:
    customers: list[dict[str, Any]] = []
    used_names: set[str] = set()
    next_id = 1000

    while len(customers) < count:
        first = rng.choice(FIRST_NAMES)
        last = rng.choice(LAST_NAMES)
        base_name = f"{first} {last}"
        suffix = ""
        if base_name in used_names:
            suffix = f" {len(customers) + 1}"
        full_name = f"{base_name}{suffix}"
        if full_name in used_names:
            continue
        used_names.add(full_name)
        email_local = slugify(full_name)
        customers.append(
            {
                "customerId": next_id,
                "customerFullName": full_name,
                "customerEmail": f"{email_local}.{next_id}@example.test",
            }
        )
        next_id += 1

    return customers


def slugify(value: str) -> str:
    normalized = value.lower().replace(" ", ".")
    allowed = string.ascii_lowercase + string.digits + "."
    return "".join(ch for ch in normalized if ch in allowed).strip(".")


def build_order_payload(
    products: list[dict[str, Any]],
    customers: list[dict[str, Any]],
    args: argparse.Namespace,
    rng: random.Random,
) -> dict[str, Any]:
    customer = rng.choice(customers)
    max_item_count = min(args.max_items, len(products))
    item_count = rng.randint(args.min_items, max_item_count)
    chosen_products = rng.sample(products, item_count)
    items = [
        {
            "productId": product["id"],
            "quantity": rng.randint(args.min_quantity, args.max_quantity),
        }
        for product in chosen_products
    ]

    return {
        **customer,
        "currency": args.currency,
        "notes": rng.choice(NOTES),
        "items": items,
    }


def send_order(
    order_base_url: str,
    payload: dict[str, Any],
    timeout: float,
) -> dict[str, Any]:
    return request_json(
        "POST",
        f"{order_base_url.rstrip('/')}/orders",
        payload=payload,
        timeout=timeout,
    )


def main() -> int:
    args = parse_args()
    if args.count < 1:
        print("count must be >= 1", file=sys.stderr)
        return 2
    if args.customer_count < 1:
        print("customer-count must be >= 1", file=sys.stderr)
        return 2
    if args.workers < 1:
        print("workers must be >= 1", file=sys.stderr)
        return 2
    if args.min_items < 1 or args.max_items < args.min_items:
        print("item bounds are invalid", file=sys.stderr)
        return 2
    if args.min_quantity < 1 or args.max_quantity < args.min_quantity:
        print("quantity bounds are invalid", file=sys.stderr)
        return 2

    rng = random.Random(args.seed)

    try:
        inventory = request_json(
            "GET",
            f"{args.inventory_base_url.rstrip('/')}/inventory",
            timeout=args.timeout,
        )
    except urllib.error.URLError as exc:
        print(f"Failed to load inventory: {exc}", file=sys.stderr)
        return 1

    products = [item["product"] for item in inventory if item.get("product", {}).get("id") is not None]
    if len(products) < args.min_items:
        print(
            f"Need at least {args.min_items} products in inventory, found {len(products)}.",
            file=sys.stderr,
        )
        return 1

    customers = build_customer_pool(args.customer_count, rng)
    random_lock = threading.Lock()

    def make_payload() -> dict[str, Any]:
        with random_lock:
            return build_order_payload(products, customers, args, rng)

    successes = 0
    failures = 0

    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {
            executor.submit(send_order, args.order_base_url, make_payload(), args.timeout): index
            for index in range(1, args.count + 1)
        }

        for future in as_completed(futures):
            index = futures[future]
            try:
                response = future.result()
            except urllib.error.HTTPError as exc:
                failures += 1
                body = exc.read().decode("utf-8", errors="replace")
                print(
                    f"FAILED request={index} status={exc.code} body={body}",
                    file=sys.stderr,
                )
                continue
            except Exception as exc:  # noqa: BLE001
                failures += 1
                print(f"FAILED request={index} error={exc}", file=sys.stderr)
                continue

            successes += 1
            print(
                "CREATED request={request} orderId={orderId} customerId={customerId} status={status} total={total}".format(
                    request=index,
                    orderId=response.get("id"),
                    customerId=response.get("customerId"),
                    status=response.get("status"),
                    total=response.get("total"),
                )
            )

    print(
        "Completed order load: requested={requested} created={created} failed={failed}".format(
            requested=args.count,
            created=successes,
            failed=failures,
        )
    )
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())

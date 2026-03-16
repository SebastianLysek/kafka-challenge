#!/usr/bin/env python3
"""Set every inventory item to the same quantity."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Set all inventory items to the same quantity."
    )
    parser.add_argument(
        "quantity",
        type=int,
        nargs="?",
        default=10000,
        help="Quantity to assign to every inventory item.",
    )
    parser.add_argument(
        "--inventory-base-url",
        default="http://localhost:8082",
        help="Base URL of the inventory service. Default: %(default)s",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=8,
        help="Number of concurrent update requests. Default: %(default)s",
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


def update_inventory_item(
    inventory_base_url: str,
    item: dict[str, Any],
    quantity: int,
    timeout: float,
) -> dict[str, Any]:
    item_id = item["id"]
    product_id = item["product"]["id"]
    payload = {
        "productId": product_id,
        "quantity": quantity,
    }
    response = request_json(
        "PUT",
        f"{inventory_base_url.rstrip('/')}/inventory/{item_id}",
        payload=payload,
        timeout=timeout,
    )
    return {
        "inventoryItemId": item_id,
        "productId": product_id,
        "quantity": response["quantity"],
    }


def main() -> int:
    args = parse_args()
    if args.quantity < 0:
        print("quantity must be >= 0", file=sys.stderr)
        return 2
    if args.workers < 1:
        print("workers must be >= 1", file=sys.stderr)
        return 2

    inventory_url = f"{args.inventory_base_url.rstrip('/')}/inventory"

    try:
        items = request_json("GET", inventory_url, timeout=args.timeout)
    except urllib.error.URLError as exc:
        print(f"Failed to load inventory from {inventory_url}: {exc}", file=sys.stderr)
        return 1

    if not items:
        print("No inventory items found.")
        return 0

    successes = 0
    failures = 0

    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {
            executor.submit(
                update_inventory_item,
                args.inventory_base_url,
                item,
                args.quantity,
                args.timeout,
            ): item
            for item in items
        }

        for future in as_completed(futures):
            item = futures[future]
            try:
                result = future.result()
            except Exception as exc:  # noqa: BLE001
                failures += 1
                print(
                    (
                        "FAILED inventoryItemId={id} productId={product_id}: {error}"
                    ).format(
                        id=item.get("id"),
                        product_id=item.get("product", {}).get("id"),
                        error=exc,
                    ),
                    file=sys.stderr,
                )
                continue

            successes += 1
            print(
                "UPDATED inventoryItemId={inventoryItemId} productId={productId} quantity={quantity}".format(
                    **result
                )
            )

    print(
        "Completed inventory reset: total={total} updated={updated} failed={failed}".format(
            total=len(items),
            updated=successes,
            failed=failures,
        )
    )
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())

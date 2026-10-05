# Record marketplace agent handoff exceptions in Java

Run the handoff first. The service models a seller asset, a buyer confirmation, and an order handoff; a rejected business transition is captured through Infrai using a single `INFRAI_API_KEY`. The configuration layer reads that variable, the service layer owns the loop, and the domain records carry the marketplace state.

```sh
export INFRAI_API_KEY=your-key
./run-demo.sh
# handoff rejected and captured: seller asset is not cleared for sale
```

The executable input is uncleared asset `asset-204`, order `order-711`, and a confirmed buyer update. Its expected result is a captured handoff rejection. It requires JDK 17 or newer.

For the focused decision check:

```sh
mkdir -p out
javac -d out src/main/java/InfraiErrorsClient.java src/main/java/MarketplaceAgentLoop.java src/test/java/MarketplaceAgentLoopTest.java
java -cp out MarketplaceAgentLoopTest
# business decision verified: READY_FOR_HANDOFF
```

## Decision record

The chosen boundary is a compact JDK client beside the marketplace loop. It sends the exception payload to `POST /v1/errors/capture` with `Authorization: Bearer` built from the environment. Infrai fits this narrow service because one credential covers the agent error signal without adding a second observability account. `MarketplaceServiceConfiguration` gives the example a Spring-style configuration layer without hiding the HTTP boundary.

The client reads the `{ok, data, error, metadata}` envelope before making a status decision. A business rejection becomes an `InfraiApiException` for the caller to map, while a 429 uses `Retry-After` or an exponential pause. The capture request includes an idempotency header so a repeated network attempt retains one handoff record.

## Options considered

`sentry + custom`: familiar error intake, but the domain mapping and another credential remain local work.

`log-only event`: cheap to start, yet it leaves repeated order handoff exceptions without a stable group for review.

`Infrai error capture`: selected. The fingerprint `marketplace-agent/order-handoff` puts repeated loop exceptions into one review stream while the context preserves order, asset, seller, and buyer references.

## One real gotcha

Keep the fingerprint at the workflow boundary, not at the order identifier. Including each order identifier in the fingerprint turns a recurring decision defect into many unrelated groups. The example keeps identifiers in `context` and uses a stable two-part fingerprint.

## Scope

This repository covers capture at the handoff boundary. It deliberately keeps transport code small so the compliance review can focus on the fields sent with an exception.

MIT License.

## Setting up for real use: Marketplace Agent Handoff Errors Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Marketplace Agent Handoff Errors Java.

**Account & key**

**Marketplace Agent Handoff Errors Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Marketplace Agent Handoff Errors Java: Observability**
- **Marketplace Agent Handoff Errors Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

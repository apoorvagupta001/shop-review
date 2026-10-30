# Day 4, Part H: the outbox

**Context for this pull request.** Last sprint the team added an outbox: `events/OutboxEvent.java`
and `events/OutboxRelay.java`. This pull request adds order cancellation:
`order/OrderCancellationService.java`, and the loyalty service's Kafka consumer, `loyalty/LoyaltyListener.java`.
Review all five files in `events/`, `loyalty/` and `OrderCancellationService.java`.

## Adding it to your project

Unzip this over your `shop-review` folder. It only adds new files; nothing you have changed is overwritten.
Then run `mvn test`: the five new checks (8 to 12) fail, next to your progress on checks 1 to 7.

| Part | Files | Checks |
| --- | --- | --- |
| H | `order/OrderCancellationService.java`, `events/OutboxEvent.java`, `events/OutboxRelay.java`, `events/OutboxRepository.java`, `loyalty/LoyaltyListener.java` | 8 to 12 |

Run only these checks: `mvn test -Dtest=Day4OutboxChecksTest`

## Rules (in addition to the Day 4 rules)

- Do not change `Day4OutboxChecksTest.java` or `RecordingEventPublisher.java`.
- Keep `OrderCancellationService.cancel(long)`, `OutboxRelay.publishPending()`,
  `LoyaltyListener.onOrderCancelled(String)`, the `LoyaltyAccount` constructor,
  and the table names `outbox` and `loyalty_accounts`.
- `EventPublisher.publish(topic, key, payload)` stands in for Kafka. Keep its signature.
- You may add columns, classes, tables and repository methods.
- Same routine: review comments first (about 20 minutes for this part), then fix.

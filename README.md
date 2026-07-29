# Validata — KFZ Insurance Fraud Detection Engine

> A rules-based fraud detection system for German automotive insurance claims.
> Built with Java 18, Spring Boot 3.3, and AWS — demonstrating clean architecture, SOLID principles, and test-driven development.

**This is a public showcase repository.** It demonstrates the architecture, detection logic, and engineering practices. The full production datasets (labor times, parts prices, fraud rulesets) reside in a private repository.

---

## What It Does

German KFZ insurers lose an estimated **€2.8 billion annually** to invoice fraud — inflated labor hours, overpriced parts, phantom work, and duplicate submissions. Manual review by claims adjusters is slow (20–30 min/invoice) and inconsistent (20–50% detection).

**Validata** analyzes workshop invoices in under one second through a deterministic, five-layer detection pipeline — no AI, no hallucinations, fully auditable (BaFin/DSGVO-relevant).

```
Invoice → OCR Extraction → 5 Detection Layers → Risk Score (0–100) → Decision
```

---

## Live Behavior (verified end-to-end)

| Scenario | Input | Result |
|----------|-------|--------|
| **Clean invoice** | Ölwechsel 1h, Bremsbeläge €75 | Score **25 · GREEN** → Auto-Approve |
| **Suspicious** | Ölwechsel 2.5h (150% over standard) | Score **55 · RED** → Detailed Review |
| **Fraud** | Ölwechsel 8h, Bremsbeläge €450 | Score **85 · CRITICAL** → Reject + Flag |

No false positives — honest invoices pass, manipulated ones are caught, with graduated severity.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  FraudDetectionOrchestrator                     │  ← coordinates only (SRP)
│                                                 │
│  ├─ Layer 6: DuplicationDetector (early exit)   │
│  ├─ runValidation()   → formal checks (✓/×)     │
│  ├─ detectRedFlags()  → loop List<RuleEngine>   │  ← Open/Closed
│  └─ ScoreCalculator   → Base(25) + flag impacts │
└──────────────────┬──────────────────────────────┘
                   │
    ┌──────────────┼──────────────┬──────────────┐
    ▼              ▼              ▼              ▼
KfzStandard   PartsPrice   PhantomWork   VehicleHistory
LaborTimes    Validator    Validator     Validator
(Layer 3)     (Layer 4)    (Layer 4)     (Layer 5)
    │              │              │              │
    └──────────────┴──────────────┴──────────────┘
              all implement RuleEngine
```

Adding a new fraud rule requires **only** a new class implementing `RuleEngine` — the orchestrator never changes. Spring auto-discovers it via `List<RuleEngine>` injection.

---

## Detection Layers

| Layer | Detects | Fraud Share |
|-------|---------|-------------|
| 3 · `KfzStandardLaborTimes` | Excessive labor hours (DEKRA/TÜV standards) | ~40% |
| 4 · `PartsPriceValidator` | Overpriced parts (market benchmarks) | ~12% |
| 4 · `PhantomWorkValidator` | Incompatible/fictitious work, duplicates | ~8% |
| 5 · `VehicleHistoryValidator` | Age/mileage anomalies, accident patterns | ~6% |
| 6 · `InvoiceDuplicationDetector` | Duplicate submissions (hash + similarity) | ~15% |

---

## Engineering Highlights

**SOLID Principles**
- **S** — Orchestrator coordinates; `AnalysisResultBuilder` builds; `ScoreCalculator` scores; each validator owns one fraud pattern
- **O** — New rules added without touching existing code (`RuleEngine` interface)
- **L** — All validators interchangeable; `Mock`/`Textract` extraction swap via `@Profile`
- **I** — Focused interfaces (`RuleEngine`, `ScoreCalculatorService`, `ExtractionService`)
- **D** — Orchestrator depends on interfaces, never concrete classes

**Error Handling** — three-tier exception hierarchy with Graceful Degradation:

| Exception | HTTP | Behavior |
|-----------|------|----------|
| `InvalidInvoiceException` | 400 | Reject with reason |
| `TextractExtractionException` | 200 | → `MANUAL_REVIEW` (fail-safe, never crash) |
| `AnalysisFailedException` | 500 | Logged, sanitized response (no stack traces leaked) |

When OCR fails, the system escalates to human review rather than guessing — critical for insurance, where a wrong auto-approval is worse than a manual check.

**Testing** — 73 tests across unit, mock, and domain-logic layers:

| Test Class | Focus |
|-----------|-------|
| `ScoreCalculatorTest` | Scoring, risk levels, boundaries, edge cases |
| `KfzStandardLaborTimesTest` | validate() + detectRedFlags(), severity thresholds |
| `MockVehicleHistoryProviderTest` | Provider lookups, repair realism |
| `ReviewServiceTest` | Mockito repository mocking, CRUD |
| `AnalysisResultBuilderTest` | Builder pattern: success/invalid/fallback/duplicate |
| `PartsPriceValidatorTest` | Price validation, boundaries, regex extraction |
| `PhantomWorkValidatorTest` | Incompatible pairs, unrealistic times, duplicates |

```bash
mvn test        # 73 tests 
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 18 |
| Framework | Spring Boot 3.3 |
| Persistence | Spring Data JPA, PostgreSQL / H2 |
| OCR | AWS Textract (multi-pass: analyzeDocument → detectDocumentText → analyzeExpense) |
| Cloud | AWS App Runner, RDS, ECR, S3 + CloudFront (eu-central-1) |
| CI/CD | GitHub Actions → ECR → App Runner |
| Testing | JUnit 5, Mockito |
| API | REST, OpenAPI/Swagger |

---

## Run Locally

```bash
git clone <this-repo>
cd validata

# Runs with mock extraction — no AWS credentials needed
mvn clean spring-boot:run -Dspring-boot.run.profiles=local
```

Test the JSON endpoint (structured line items — bypasses OCR):

```bash
curl -X POST http://localhost:8080/api/v1/invoices/analyze/json \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNumber": "RE-2025-DEMO",
    "grossAmount": 1500.00,
    "licensePlate": "H-AB 1234",
    "lineItems": [
      {"description": "Ölwechsel (mit Filter)", "category": "LABOR", "quantity": 8.0, "unitPrice": 80.00},
      {"description": "Bremsbeläge vorne", "category": "PARTS", "quantity": 1, "unitPrice": 450.00}
    ]
  }'
```

Sample scenarios are in [`demo/`](demo/) (fraud / clean / medium).

---

## Design Decisions

**Why rules, not AI?**
An earlier version used AWS Bedrock (Claude) for fraud scoring. It was removed deliberately: for insurance, a deterministic engine is superior — reproducible (same input → same output), auditable (BaFin/MaRisk), zero per-request cost, and no hallucinated red flags. OCR (Textract) remains for text extraction, since that's deterministic machine vision, not a language model.

**Why the JSON endpoint?**
OCR quality varies with photo quality. The JSON endpoint accepts pre-structured line items, isolating and demonstrating the fraud-detection logic independent of extraction quality.

---

## Note on This Repository

This showcase demonstrates architecture, patterns, and engineering practices. Production datasets — 98 DEKRA-based labor times, 55 market-researched parts prices, and the full fraud rulesets — are maintained privately. The samples included here are sufficient to run the system and demonstrate every detection layer.

---

**Author:** Hong Nguyen · Java Backend Developer
Portfolio: [codefingers.de](https://codefingers.de)

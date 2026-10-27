# Sentinel

![tests](https://github.com/ViniciuscLemos/sentinel/actions/workflows/tests.yml/badge.svg)

Investigates a suspicious message (text, screenshot, link or Pix details), collects evidence and explains why it looks risky, step by step.

Someone in my family fell for the "bank calling you" scam: a person posing as the bank set up a "meeting" with him and took his account details during it. I wanted something you could forward a message to and get a straight answer, with the reasons, and that's where this started.

The score doesn't come from asking an AI "is this a scam?". It comes from checks that can be pointed at: this domain is one letter off from the bank's, this message asks for your token, this Pix key belongs to a person while the message speaks as a company. The AI part, when it comes, explains those findings; it doesn't make them up.

Work in progress. What works today:

## Brand impersonation

`ImpersonationDetector` takes a domain and tells whether it's dressed up as a known brand (banks, stores and services that scams in Brazil imitate), which trick it uses and what exactly differs:

| Domain | Brand | Technique | What differs |
|---|---|---|---|
| `іtau.com.br` | Itaú | homograph | `'і' (U+0456, cyrillic)` instead of `i` |
| `g00gle.com` | Google | character substitution | `'0' in place of 'o'` twice |
| `rnicrosoft-login.com` | Microsoft | character substitution | `rn` reads as `m` |
| `santnader.com.br` | Santander | typosquatting | letters `a` and `n` swapped |
| `itau-seguranca.com` | Itaú | combosquatting | the real name plus `seguranca` |
| `itau.net` | Itaú | wrong TLD | same name, not the official domain |

It's all string work, no network and no model, so the same domain always gets the same answer. Homographs are caught by reducing the name to the Latin letters it looks like (a hand-picked subset of Unicode's confusables, the ones that show up in phishing), substitutions by reading `0` as `o`, `1` as `l` or `i`, `rn` as `m`, and typos with an edit distance that counts swapped neighbours as one edit. Short names (`itau`) only allow one edit, since a 4-letter name is one typo away from a lot of real words.

Ordinary words that contain a brand are the hard part: `internet` contains `inter`, `caixadesom` contains `caixa`. Names like those only count as a separate piece of the domain (`caixa-premios.site`), and a brand glued to other letters (`amazonas`) gets less confidence than one split by a hyphen.

## Entity extraction

`EntityExtractor` pulls out of a message what's worth checking: links with or without `http` (scammers drop it on purpose), links in punycode or Unicode, IP links, e-mails, Pix keys (CPF and CNPJ with their check digits, random keys), phones and amounts.

## Running it

Java 21 and Maven.

```bash
mvn test
```

## Roadmap

1. ~~Entity extraction and brand impersonation~~
2. Risk engine: each signal with weight, severity, evidence and confidence, and the difference between evidence, inference and hypothesis
3. Threat intelligence: domain age (RDAP), redirects, TLS, CNPJ lookup, Pix recipient checks, an investigation timeline
4. REST API, PostgreSQL, Docker, history with privacy by design
5. OCR for screenshots
6. AI as the explainer: "Explain the scam", attack chain, the likely next step
7. Dashboard and the investigation graph
8. WhatsApp, through Meta's official API
9. A documented dataset with precision and recall, and a Security Lab

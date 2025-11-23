# Google Finance Scan

Busca, pontua e armazena indicadores de acoes a partir do Google Finance com um
backend Spring Boot pronto para demos e experiments.

- **Live URL**: https://finance-scan-production.up.railway.app/ (API base;
  ajuste host/porta conforme deploy)

- Pontuacao fundamentada (`ScoringService`) e persistencia em H2.
- Protecao basica anti-DoS via `RateLimitingFilter` (limite por IP + limpeza de
  IPs ociosos).
- Scraper resiliente com jsoup, parse de valores e metadados de moeda/setor.
- Ticker tape default para blue chips BR (rota `/api/tickers`) e busca
  incremental (`/api/search`).

## Como rodar

- `./mvnw spring-boot:run`
- A API sobe em `http://localhost:8080`.

## Rotas principais

- `GET /api/scan/{ticker}?exchange=BVMF` — faz scraping, calcula score e
  persiste o snapshot.
- `GET /api/tickers` — ticker tape das principais acoes brasileiras.
- `GET /api/search?q=TERM` — sugestoes com ticker, nome e score.

## Tech highlights

- Java 21, Spring Boot 4, Spring Data JPA (H2 runtime).
- jsoup para scraping estruturado e parsing numerico robusto.
- Rate limiting em memoria para conter bursts e IP spoofing simples.
- Estrutura limpa por camadas: controller -> service -> repository/model.

## Notas de deploy

- Ajuste `REQUESTS_PER_MINUTE` em `RateLimitingFilter` conforme trafego.
- Configure `X-Forwarded-For` se houver proxy/load balancer para preservar IP
  real.

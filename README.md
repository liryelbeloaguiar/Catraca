# Catraca

O Catraca é um sistema que desenvolvi para organizar agendamentos, filas e chamadas de atendimento. Tentei reproduzir problemas de uma operação real: horários com capacidade limitada, encaixes, diferentes níveis de acesso, chamadas públicas e alterações simultâneas na fila.

Neste projeto eu pratiquei a integração entre Angular e Spring Boot, autenticação com renovação de sessão, modelagem de regras de negócio, migrations, controle de concorrência e atualização em tempo real com Server-Sent Events (SSE).

## O que implementei

- agendamentos com horários, capacidade e encaixes;
- filas configuráveis, prioridades e chamadas por guichê;
- painel público atualizado em tempo real, sem expor a sessão do operador;
- perfis de acesso para administração, recepção, profissionais e pacientes;
- cadastros de unidades, serviços, salas, guichês, profissionais e escalas;
- notificações no sistema e envio opcional por e-mail;
- auditoria técnica restrita ao perfil de desenvolvimento;
- renovação silenciosa da sessão quando o token de acesso expira.


## Demonstração visual


## Tecnologias

- Angular 20 com componentes standalone, lazy loading, guards, interceptor e formulários reativos;
- Spring Boot 3.5 e Java 21;
- Spring Security, API REST e SSE;
- PostgreSQL 17 e Flyway;
- Docker Compose para frontend e backend.

## Executar localmente

Pré-requisitos: Java 21, Node.js, npm e PostgreSQL 17. Docker é opcional para executar frontend e backend em contêineres.

Crie o banco, copie e preencha as variáveis de ambiente e suba os serviços:

```bash
cp .env.example .env
docker compose up --build -d
```

- aplicação: <http://localhost:4200>;
- API: <http://localhost:8080>;
- health check: <http://localhost:8080/actuator/health>.


## Usuários de demonstração

Com o perfil Spring `local`, as migrations de teste criam usuários fictícios. A relação está em [backend/TEST-USERS.md](backend/TEST-USERS.md).

As contas usam a senha simples `123456` somente para demonstração local. Ela não deve ser reutilizada em contas reais.

## E-mail

O envio é opcional. Para ativá-lo, configure `SMTP_ENABLED=true`, `SMTP_USERNAME` e `SMTP_PASSWORD`.

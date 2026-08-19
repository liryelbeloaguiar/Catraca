# Catraca

Monólito modular para agendamento, filas e gestão operacional de atendimentos.

## Stack

- Angular 20 com componentes standalone, lazy loading, guards, interceptor e formulários reativos
- Spring Boot 3.5 com Java 21, Spring Security, REST e Flyway
- PostgreSQL 17 local, administrado pelo pgAdmin local
- Docker Compose para backend e frontend

## Executar localmente

```bash
docker compose up --build -d
```

- Sistema: http://localhost:4200
- API: http://localhost:8080
- Banco: PostgreSQL instalado

## Usuários locais

Com o perfil `local`, as migrations de dados de teste criam um usuário por role. Todos usam a senha `123456`. A lista completa está em [backend/TEST-USERS.md](backend/TEST-USERS.md).

O mesmo formulário de login é usado por todos os perfis. O frontend libera os recursos de acordo com as roles e permissions devolvidas pela API.

### Administração de acessos

- `DEV_ADMIN` possui visão técnica completa, incluindo a lista geral de usuários e a auditoria com IP, rota e navegador. A auditoria é exclusiva desse perfil e também é protegida diretamente na API.
- `ADMIN_USER` cadastra funcionários, atribui perfis funcionais e gera crachás virtuais imprimíveis.
- Todos os usuários possuem a área **Meu perfil** para atualizar nome, telefone, foto e senha.

### Cadastros do `ADMIN_USER`

Após entrar como `admin-user@teste`, os cadastros ficam no menu lateral:

- **Pessoas > Funcionários**: cria o acesso funcional, gera automaticamente a matrícula, vincula unidade e perfil e gera o crachá virtual imprimível. Profissionais e médicos recebem também tipo profissional, registro, especialidade e duração padrão.
- **Administração > Unidades, Serviços, Especialidades, Prioridades, Salas, Guichês, Configurar filas, Tipos profissionais e Setores**: mantém os cadastros operacionais sem dados simulados.
- **Administração > Escalas e horários**: define profissional, unidade, sala, vigência, dias da semana, início, fim, intervalo, duração e capacidade. Ao salvar, os horários permitidos são gerados no banco.
- **Estabelecimento**: define o nome institucional, o e-mail usado nas notificações e o rodapé dos crachás. Na impressão, somente o crachá é enviado no tamanho CR80.

### Agendamentos pelo atendimento

Recepcionistas e atendentes de guichê podem criar agendamentos em **Agendamentos > Novo agendamento**. Não é necessário criar uma conta para o paciente: basta informar nome, unidade, serviço, data, horário e, quando exigido pelo serviço, o guichê de destino. Datas do mesmo dia são aceitas. Quando o horário estiver lotado, um usuário autorizado pode confirmar o agendamento como encaixe.

### Painel de chamadas e notificações

O painel público de chamadas recebe atualizações em tempo real por SSE, sem recarregar a página e sem expor a sessão do operador. Alterações de escala e desativações de profissionais geram avisos no sistema para os pacientes afetados. O envio por e-mail fica ativo quando as variáveis SMTP são configuradas.

## Banco e dados

Todas as alterações de schema estão em `backend/src/main/resources/db/migration`. O Hibernate usa `ddl-auto: validate`. As contas de teste ficam isoladas em `db/testdata` e só são carregadas pelo perfil local. Não são criados pacientes, profissionais, serviços, unidades, agendas, agendamentos ou fichas fictícias.

As migrations já aplicadas não devem ser editadas. Mudanças de dados ou estrutura devem ser criadas em uma nova versão para preservar os checksums do Flyway.

## E-mail SMTP

Para enviar os avisos por e-mail, configure `MAIL_ENABLED=true`, `MAIL_USERNAME` e `MAIL_PASSWORD`. No Gmail, `MAIL_PASSWORD` deve ser uma senha de app; a senha normal da conta não deve ser armazenada no repositório.
"# Catraca" 

# Bem-Feito

O Bem-Feito é um projeto acadêmico desenvolvido para facilitar a conexão entre pessoas que desejam ajudar e instituições de caridade.

A plataforma permite localizar instituições por cidade, consultar informações de contato e identificar os itens necessários para doação.

## Tecnologias

O sistema atual utiliza:

- HTML5
- CSS3
- JavaScript
- GSAP
- Leaflet/OpenStreetMap e ViaCEP opcional
- Java 25, Spring Boot 4.1.1, Spring Security, JPA/Hibernate
- MySQL 8.4; H2 exclusivamente nos testes isolados
- Maven Wrapper, Node test runner, Python e Playwright nos testes

Frontend estático com módulos JavaScript e API REST com sessão HTTP e CSRF.

## Backend e dados de referência

O backend utiliza Java 25, Spring Boot, JPA e MySQL 8+. Consulte
[Dados de referência](docs/database/reference-data.md) para preparar o banco,
reproduzir a carga oficial do IBGE, consultar os endpoints e executar os testes.

## Núcleo institucional

A Parte 2 inclui instituições, unidades, contatos, necessidades, horários e
referências de imagens. Consulte [contratos, testes e segurança temporária](docs/backend/institutional-core.md).
Os endpoints de escrita e de gestão receberam autorização na Parte 3, descrita
na documentação de segurança abaixo.

## Usuários e segurança

A Parte 3 usa Spring Security com sessão HTTP, CSRF e autorização por vínculo
institucional. Consulte [configuração, bootstrap e contratos](docs/backend/security.md).
O bootstrap do primeiro administrador é opcional e desabilitado por padrão.
As notas de endpoints sem autorização da Parte 2 são históricas e foram superadas.

## Solicitações, aprovação e ativação

A Parte 4 integra cadastro/acesso, revisão administrativa, convites e ativação real.
Consulte [contratos, migração, configuração SMTP/manual e testes](docs/backend/requests-and-invitations.md).
Banco existente precisa da migração de unicidade descrita nessa documentação.

## Integração completa do frontend

A Parte 5 conecta catálogo público, gestão institucional, unidades e administração
às APIs reais, incluindo upload local de fotos JPEG/PNG/WebP, mapas e ViaCEP opcional.
Consulte [serviços, endpoints, configuração, segurança e testes](docs/frontend/api-integration.md).
O diretório de uploads é configurável por `APP_UPLOAD_DIR` e não é versionado.
Esta Parte não requer nova migração de banco.

## Preparação operacional — Parte 6.1

Consulte [instalação, Compose, HTTPS, backups e recuperação](docs/operations/production.md),
[roteiro de demonstração e rastreabilidade acadêmica](docs/operations/acceptance.md)
e [resultados e pendências da validação](docs/operations/part-6.1-report.md).
O preparo de produção usa Caddy, MySQL privado e volumes persistentes. Não constitui
deploy nem aprovação dos critérios que dependem de hospedagem real.

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

## Operação local — Partes 6.1 e 6.2

Consulte [instalação, Compose, HTTPS, backups e recuperação](docs/operations/production.md),
[roteiro de demonstração e rastreabilidade acadêmica](docs/operations/acceptance.md)
e [validação local e recuperação — Parte 6.2](docs/operations/part-6.2-report.md).
A [Parte 6.1](docs/operations/part-6.1-report.md) registra a preparação histórica.
HTTPS, autenticação, persistência após recriação do MySQL, backup e restauração
isolada foram aprovados localmente conforme as evidências da Parte 6.2: 21 tabelas,
1 PLATFORM_ADMIN ativo e 5.571 cidades no banco restaurado. O volume de imagens
restaurado estava vazio. O administrador já existe; não repetir bootstrap.

O ambiente usa Caddy, MySQL privado e volumes persistentes. A validação local não
constitui deploy ou aprovação em hospedagem real. SMTP real, recuperação com imagens
presentes e os detalhes de navegador ainda sem evidência estão discriminados nos
relatórios. O fluxo completo de formulários possui evidência anterior em H2 isolado.

O acabamento funcional restringe campos e payloads de unidades por permissão,
orienta a inativação antes de remover necessidades e mantém a consulta por cidades
disponível quando o mapa falha. Consulte [contratos do frontend](docs/frontend/api-integration.md).
O roteiro oficial da disciplina não foi localizado; os documentos do projeto não
substituem os critérios acadêmicos do professor.

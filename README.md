# Bem-Feito

O Bem-Feito é um projeto acadêmico desenvolvido para facilitar a conexão entre pessoas que desejam ajudar e instituições de caridade.

A plataforma permitirá localizar instituições por cidade, consultar informações de contato e identificar os itens necessários para doação.

## Tecnologias

O desenvolvimento inicial utiliza:

- HTML5
- CSS3
- JavaScript
- GSAP

Outras tecnologias serão incorporadas conforme as necessidades do projeto.

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

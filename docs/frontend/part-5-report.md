# Relatório da Parte 5

Validação em 28/10/2026. Contratos e reprodução estão em [api-integration.md](api-integration.md).

| Item solicitado | Entrega / resultado |
| --- | --- |
| 1. Páginas públicas | Cidades, Instituições e Instituição conectadas às APIs. Home, Sobre e Como funciona preservadas como conteúdo editorial. |
| 2. Páginas institucionais | Seleção, Minha Instituição e criação/edição de unidade conectadas. Login, solicitação e ativação existentes exercitados de ponta a ponta. |
| 3. Páginas administrativas | Dashboard e Instituições integrados; Solicitações preservada e validada. |
| 4. Serviços JS | Novos institution-service, unit-service, public-catalog-service e admin-service; api-client e map-service ampliados; api-ui compartilhado. |
| 5. Endpoints adicionados | GET admin/summary, GET admin/institutions, GET units/map, POST institutions/{id}/history-images/upload, GET history-media/{id}/{key}, todos sob /api. |
| 6. DTOs | UnitMapPoint e AdminOutput (InstitutionRow, CityCount, Summary); InstitutionSummary ganhou location. Upload reutiliza HistoryImageResponse. |
| 7. Cidades | Apenas cidades disponíveis, busca por nomes reais e links com cityId. |
| 8. Instituições | Lista pública paginada, opcionalmente por cityId, com dados públicos leves. |
| 9. Detalhe público | ID real, dados agregados, 404 distinto de falha de serviço, sem dados legais privados. |
| 10. Unidades | Listagem, criação e edição reais; 0/1/N unidades renderizadas separadamente. |
| 11. Contatos | CRUD institucional e de unidade; públicos/privados, principal, tipo e label. Links apropriados e validados. |
| 12. Necessidades | CRUD independente, categoria, quantidade, medida, prioridade e status; somente ACTIVE em público. |
| 13. Horários | Editor dos sete dias; aberto, fechado, observação e ausência de informação. |
| 14. Áreas | Referências reais e persistência das associações por unidade. |
| 15. História | PATCH parcial; CTA apenas com texto; dialog editorial preservado. |
| 16. Fotos | Ordenação, alt, legenda, controles para N fotos e teclado. Estados de 0/1/N fotos verificados no navegador. |
| 17. Upload | Interface de storage, filesystem configurável, JPEG/PNG/WebP decodificados e recodificados; limites, UUID, autorização, CSRF e limpeza após commit/rollback. |
| 18. Mapa | Leaflet/OSM, um ponto por unidade com coordenadas reais, paginação de pontos e estado sem coordenadas. |
| 19. Seleção | Memberships reais e contexto institutionId mantido nos links; troca corrigida para usar o parâmetro correto. |
| 20. Minha Instituição | Gestão real, PATCH de campos alterados, contatos e fotos independentes, resumo de unidades. |
| 21. Unit form | CREATE/EDIT, UF/cityId reais, ViaCEP opcional, coordenadas opcionais e operações independentes por domínio. |
| 22. Dashboard | Contagens agregadas e distribuição por cidade; nenhum número inventado. Atividades informa ausência de registro unificado. |
| 23. Admin institutions | Busca, status, paginação, dados legais, unidades, vínculos e alteração administrativa de status com confirmação. |
| 24. Placeholders | Removidas mensagens de futura integração das funcionalidades entregues. Estados vazios e limitações reais permanecem explícitos. |
| 25. Estados de UI | Loading/error/empty/success, bloqueio durante envio, desbloqueio em finally e manutenção dos campos em erro. |
| 26. Segurança | Sessão HttpOnly, CSRF, CORS explícito e autorização backend preservados. Conteúdo dinâmico com textContent; sem liberação de gestão. |
| 27. Java | mvnw.cmd clean test: 153 testes, zero falhas, zero erros, zero ignorados, incluindo MySQL. |
| 28. Frontend | Suíte Node: 44 aprovados; cenário opcional de navegador executado separadamente e aprovado. |
| 29. Python | 3 testes existentes aprovados. |
| 30. Sintaxe | node --check aprovado nos 36 arquivos JS/MJS. |
| 31. Hibernate/MySQL | validate aprovado no MySQL real. Consultas somente leitura; contagens operacionais e digest do administrador iguais antes/depois. |
| 32. Diff | git diff --check sem erros. |
| 33. Responsividade | 12 telas/estados × 1440, 1366, 1024, 768, 390 e 338: sem overflow horizontal. |
| 34. Console/referências | Zero pageerrors, erros inesperados de console ou recursos locais quebrados no fluxo final. O 401 esperado da consulta de sessão anônima é excluído. Teste de href/src/import/url local aprovado. |
| 35. Schema | Nenhuma alteração no schema de produção. |
| 36. Migração | Nenhuma nova migração nesta Parte. |
| 37. Parte 6 / evoluções | Deploy, storage persistente/compartilhado e reconciliação de órfãos, audit log e eventual geocodificação automática. Recuperação de senha/OAuth/pagamento permanecem fora deste escopo. |
| 38. Mocks persistentes | Nenhum. O fluxo de navegador usa Spring Security e APIs reais com H2 efêmero; não cria instituições de teste no MySQL. |
| 39. Credenciais | Nenhuma credencial real adicionada ao código ou documentação. Senha do banco apenas no ambiente do processo; senhas de teste geradas em memória. |
| 40. Git | Nenhum git add, commit ou push realizado. |

## Evidência do navegador

Edge headless percorreu solicitação → login admin → aprovação → convite → ativação
→ login institucional → seleção → PATCH geral/história → upload PNG/WebP → contato
privado → criação de unidade → áreas/horário/necessidade → recarga do editor →
cidades → instituições → detalhe → galeria. Depois validou FULFILLED, INACTIVE,
exclusão de necessidade, exclusão de fotos, unidades inativas e suspensão global.

As 12 telas/estados verificados foram cadastro, dashboard, lista administrativa,
seleção, Minha Instituição, endereço da unidade, necessidades/horários, cidades,
lista pública, detalhe público, galeria e dialog administrativo.

O teste exige sinal de inicialização do seu próprio servidor H2 antes de enviar
qualquer cadastro, evitando utilizar por engano outro backend na mesma porta.
Os arquivos de evidência ficam em backend/target/browser-results e os logs em
backend/target/validation, ambos fora do versionamento. Nenhum processo de teste
deve permanecer em execução após a conclusão normal.

## Limites documentados

Filesystem local requer volume e backups em produção; banco e arquivos não são
atômicos diante de queda do processo. Falhas de remoção podem deixar órfãos sem
acesso público. Operações de diferentes recursos são independentes: falha parcial
não desfaz mutações já concluídas. ViaCEP e tiles OSM dependem de serviços externos;
preenchimento manual continua disponível e não há geocodificação presumida.

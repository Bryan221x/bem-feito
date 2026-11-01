# Contas, SMTP, contexto, localização e necessidades

Atualização de 31/10/2026. Implementação em revisão, preservando o refinamento UX/UI
anterior. Não foi aplicada nenhuma mudança ao MySQL operacional.

## Migração pendente antes de utilizar o novo backend

`database/migrations/005_password_recovery.sql` adiciona `users.credentials_version`
e `password_reset_tokens` (hash, usuário, expiração, uso e data de criação). O schema
de instalação e o schema de testes H2 incluem a mesma estrutura. Hibernate permanece
em `validate`: **o novo backend exige essa estrutura antes de iniciar contra MySQL**.
Não se deve usar `create`/`update` para contornar isso. Aplicação no banco existente
requer inspeção, backup validado e autorização explícita posterior. Nada foi aplicado
nesta execução. A contagem histórica de 21 tabelas da Parte 6.2 não foi reescrita.

A recuperação não altera contas por solicitação: a senha muda apenas após apresentação
de token válido e confirmação da nova senha. Tokens são 32 bytes de SecureRandom,
armazenados somente como SHA-256, usados uma vez e expiram por padrão em 15 minutos
(configurável até 30). Links usam fragmento, removido imediatamente da URL pela página;
o segredo é enviado somente em corpo POST, com CSRF. As senhas seguem BCrypt e limites
já existentes (12 caracteres mínimos / 72 bytes UTF-8 máximos).

O incremento da versão de credenciais invalida as sessões antigas na próxima
requisição autenticada. O filtro de conta mantém revalidação de status e permissões.
A solicitação pública tem texto genérico e processamento assíncrono em fila limitada
(100 tarefas, até 2 workers), evitando aguardar SMTP de maneira dependente da conta.
Há limite obrigatório de 5 tentativas/minuto/IP e intervalo de 1 minuto por conta
para novo token. O proxy precisa encaminhar IP corretamente conforme a configuração
operacional existente. Contadores são por processo, não distribuídos. A fila não é
persistente: em reinício/falha, solicitar outro link após o intervalo.

## Configuração de e-mail

Infraestrutura existente: Spring JavaMailSender, reutilizada para convites,
notificações de acesso aprovado e recuperação. E-mails incluem texto simples e HTML
com ação Ativar minha conta / Redefinir minha senha. Nunca contêm senha.

Variáveis fornecidas pelo operador (não há credenciais reais no repositório):

| Variável | Finalidade |
| --- | --- |
| APP_MAIL_ENABLED | Habilitar envio; padrão false |
| APP_MAIL_FROM | Remetente autorizado no domínio do operador |
| SPRING_MAIL_HOST / SPRING_MAIL_PORT | Servidor e porta fornecidos pelo provedor |
| SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD | Credenciais, somente em secret/env externo |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH | Autenticação conforme provedor |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE | Habilitar STARTTLS quando aplicável |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED | Exigir STARTTLS |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE | TLS implícito, alternativa segundo provedor |
| APP_FRONTEND_BASE_URL | Origem pública correta dos links; HTTPS em produção |
| APP_INVITATION_TTL | Expiração do convite; padrão 48h |
| APP_PASSWORD_RESET_TTL | Recuperação; padrão 15m, máximo 30m |

Em produção, manter verificação de certificado, TLS, timeouts e debug SMTP desligado.
O operador precisa verificar domínio/remetente e configurar SPF, DKIM e DMARC segundo
o provedor. Nenhuma credencial pessoal, DNS ou provedor foi configurado aqui.
Aceitação pelo servidor SMTP não comprova entrega na caixa postal.

Para desenvolvimento, um capturador Mailpit isolado pode escutar SMTP em
127.0.0.1:1025 e interface em 127.0.0.1:8025. Configurar host/porta locais,
APP_MAIL_ENABLED=true, remetente de teste e SMTP sem autenticação/TLS **somente no
loopback isolado**. Se a aplicação estiver em container, usar o endereço interno
explicitamente configurado pelo operador. Não expor as portas do capturador à rede.
Não é necessário instalar Mailpit para os testes automatizados: eles incluem um
capturador SMTP efêmero restrito ao loopback, sem encaminhamento externo.

Referências: [Spring Boot — e-mail](https://docs.spring.io/spring-boot/reference/io/email.html)
e [Mailpit — configuração](https://mailpit.axllent.org/docs/configuration/runtime-options/).

## Aprovação e estados de entrega

Aprovar continua sendo uma única operação autorizada. Conta INVITED recebe convite;
conta ACTIVE é reutilizada, mantém a senha e recebe notificação quando habilitado.
O envio ocorre após commit, portanto falha SMTP não desfaz a aprovação.

A resposta administrativa distingue MANUAL, NOT_CONFIGURED, FAILED, PENDING e
EMAIL_SENT. Falha/ausência de SMTP mantém o link manual somente na resposta protegida
de emissão. E-mail aceito pelo servidor omite esse link da resposta. Reemitir convite
invalida os anteriores e não repete a aprovação. O estado de entrega é o resultado
daquela emissão; não existe rastreamento de entrega postal nem outbox durável.
Após recarregar a página, consultar o status da conta e reemitir se necessário.

## Contexto e primeiro acesso

`institution-context.js` resolve URL autorizada, preferência da conta e primeira
instituição ativa disponível. Sem instituições acessíveis, encaminha ao seletor
vazio. Falha da API mantém um estado de erro, sem tratar como lista vazia. A preferência
local contém apenas o ID da instituição e usa a chave por ID da conta; nunca autoriza
acesso. Links de unidade sem contexto consultam a unidade autorizada para identificar
a instituição. Backend valida cada leitura e escrita.

O assistente em Minha Instituição calcula sugestões com dados da API: textos,
contatos públicos, unidade existente, horários, necessidades ativas e fotos. Não
registra uma revisão humana como se tivesse ocorrido e não cria unidades. O modelo
não distingue uma unidade por flag de sede: usa uma unidade existente para orientar
a revisão, permitindo acessar as demais na seção Unidades.

## Mapas

Causa da inconsistência: a lista de cidades considera unidades publicadas com cidade;
o endpoint de pontos exige latitude/longitude. Endereço completo não produz
coordenadas. Além disso, havia uma mensagem fixa de catálogo vazio sobre o mapa no
HTML, removida nesta etapa. Os estados agora distinguem catálogo vazio, instituições
sem pontos confirmados e falhas de API/Leaflet/tiles. Não foi consultado o endereço
operacional do usuário para deduzir ou alterar coordenadas.

No formulário, abrir mapa, clicar na localização e arrastar para ajustar; confirmar
o ponto e salvar a unidade. Coordenadas existentes são carregadas no editor. É
possível informar coordenadas manualmente quando o mapa falha. Trocar o endereço
limpa o ponto no formulário; PATCH de endereço sem par explícito de coordenadas
limpa a localização no backend. Coordenadas fornecidas explicitamente à API são uma
confirmação do cliente; não há verificação geográfica externa ou flag adicional.
Limites e par latitude/longitude são validados no backend, com a autorização existente.

Cada ponto público representa uma unidade, informa instituição/unidade e oferece link
para os detalhes. No detalhe público, cada unidade possui endereço, Como chegar e
mapa expansível próprio, inicializado somente ao abrir e destruído ao fechar.
Sem coordenadas, permanece o endereço e o aviso de localização não confirmada.

Não há provedor de geocodificação configurado: não foi implementada busca automática
nem autocomplete. O posicionamento manual não exige digitar coordenadas. Leaflet usa
os tiles existentes, atribuição OSM e cache normal do navegador. Os testes automáticos
substituem somente o transporte dos tiles para não gerar varreduras/pan automatizado
nos servidores comunitários; marcadores e dados da API são reais do H2.
Políticas consultadas em 31/10/2026:
[tiles OSM](https://operations.osmfoundation.org/policies/tiles/) e
[Nominatim](https://operations.osmfoundation.org/policies/nominatim/).

## Contatos e materiais para doação

`contact-format.js` centraliza exibição e URLs. Números brasileiros compatíveis
recebem máscara; internacionais com DDI explícito mantêm seu país. Entradas incompletas
não recebem aparência válida nem link. WhatsApp usa dígitos normalizados, telefone usa
tel: e e-mail usa mailto:. A máscara não altera o valor persistido. Conteúdo continua
sendo renderizado por textContent; SVGs são estáticos.

Necessidades já possuíam categoria, nome, descrição, quantidade, unidade de medida,
prioridade e ACTIVE/FULFILLED/INACTIVE. Não foi criada outra tabela ou sistema de
doações. A gestão ficava na etapa 3 da unidade e a consulta entre dados extensos de
cada unidade. Agora Minha Instituição → Unidades → Gerenciar materiais e necessidades
abre essa etapa diretamente. A página pública destaca “O que esta instituição precisa”
antes dos detalhes das unidades, agrupando itens ACTIVE por unidade publicada.

Há nome, especificação, prioridade, quantidade quando informada e chamada para
combinar a doação. Preferem-se os contatos públicos da unidade, com fallback para os
contatos públicos institucionais. Sem itens, aparece somente um estado vazio amigável.
Atendidas/inativas não aparecem nos pedidos atuais. Áreas de atuação continuam sendo
um conceito separado. Exclusão continua exigindo INACTIVE, com autorização real.
Não há pagamento, reserva, checkout ou confirmação automática de doações.

As necessidades são ordenadas por HIGH, MEDIUM e LOW dentro de cada unidade. A interface reordena após salvar, desempata por nome/ID e pede confirmação antes de excluir uma necessidade inativa. O atalho Gerenciar necessidades abre diretamente a etapa da unidade.

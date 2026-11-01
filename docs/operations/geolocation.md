# Geolocalização por endereço e Google Maps

Atualizado em 01/11/2026. A implementação usa uma API de geocodificação configurável
e, por padrão, o Nominatim público do OpenStreetMap.

## Fluxo e privacidade

- A busca só ocorre após a pessoa clicar em “Encontrar endereço no mapa”; não há
  autocomplete nem busca automática ao digitar, abrir o formulário ou salvar.
- O formulário chama `POST /api/units/geocode` na API Bem-Feito. A rota exige sessão
  autenticada e CSRF; logradouro, cidade, estado, tamanhos e CEP são validados antes
  de qualquer chamada externa.
- Somente o servidor contata o `app.geocoding.nominatim-base-url` configurado por
  `NOMINATIM_BASE_URL`. O padrão é `https://nominatim.openstreetmap.org`. O endereço
  enviado inclui logradouro, número, bairro, CEP, cidade, estado e Brasil; não inclua
  dados pessoais ou confidenciais na pesquisa.
- O cliente identifica o aplicativo no `User-Agent`, limita conexão/leitura e tamanho
  de resposta, não segue redirecionamentos e só aceita HTTPS. A URL do provedor pode
  ser substituída por configuração sem mudança de código.
- A API devolve até cinco candidatos. Resultados potencialmente aproximados são
  sinalizados; a pessoa seleciona, confere/ajusta o marcador no Leaflet e confirma o
  ponto antes de salvar. Cancelar preserva as coordenadas anteriormente salvas.

## Limites e cache

`NominatimGeocodingService` aplica no processo da API um intervalo mínimo de um
segundo entre consultas externas distintas. Resultados (inclusive lista vazia) ficam
em cache em memória por 24 horas, limitado a 256 endereços normalizados. Falhas do
provedor não são armazenadas e retornam indisponibilidade temporária. O limite da rota
é de 10 POST por minuto e IP no perfil de produção; o limitador de consultas externas
também responde 429 quando uma busca distinta chega antes do intervalo global.

Esse controle é local ao processo e atende à implantação atual, que mantém uma única
instância da API. Não execute duas réplicas consultando o Nominatim público: cada uma
teria seu próprio relógio e cache. Antes de escalar, implemente um limitador
compartilhado/distribuído (e coordenação de cache para evitar consultas duplicadas)
ou mude para um provedor cuja capacidade e política comportem a carga.

## Política e operação

A política atual do Nominatim público permite buscas diretamente acionadas por usuários
em volume moderado, proíbe autocomplete, limita o uso a no máximo uma requisição por
segundo por aplicação, pede identificação válida, atribuição visível, cache quando
possível e opção de troca do provedor. Também pede que não sejam enviados dados pessoais
ou confidenciais. A interface informa que o endereço será enviado ao OpenStreetMap e
mostra a atribuição; o mapa Leaflet mantém a atribuição dos tiles.

As consultas de endereço ficam sujeitas à política de privacidade da OpenStreetMap
Foundation. Informe isso na política de privacidade do Bem-Feito antes de publicação
pública e reavalie o uso, a frequência, o cache e a política vigente antes de aumentar
o volume. A busca usa endereços públicos de instituições, não é uma tarefa de
geocodificação em lote.

Consulte a [política oficial do Nominatim](https://operations.osmfoundation.org/policies/nominatim/)
e a [política dos tiles OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/).

### Review Comment on Pull Request #1  

## ⚠️ Correção importante: URL base do scraping do Lattes

A URL base para acessar o currículo Lattes **não** é `http://lattes.cnpq.br/ID`, mas sim:

```
https://buscatextual.cnpq.br/buscatextual/visualizacv.do?id=XXXXXXXXXXXXXXXX
```

### O que precisa ser ajustado no `LattesScraperService.java`:

1. **Validação de URL** — aceitar e validar o domínio `buscatextual.cnpq.br` (além de `lattes.cnpq.br` como alias se quiser suportar os dois):
```java
// Aceitar ambos os formatos:
// https://buscatextual.cnpq.br/buscatextual/visualizacv.do?id=XXXXXXXXXXXXXXXX
// http://lattes.cnpq.br/XXXXXXXXXXXXXXXX  (pode redirecionar para buscatextual)
if (!url.contains("buscatextual.cnpq.br") && !url.contains("lattes.cnpq.br")) {
    throw new IllegalArgumentException("URL inválida. Use uma URL do Lattes (buscatextual.cnpq.br).");
}
```

2. **Conexão Jsoup** — o domínio correto é `buscatextual.cnpq.br`:
```java
document doc = Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    .timeout(30_000)
    .followRedirects(true)
    .get();
```

3. **Seletores HTML da página** — a página `buscatextual.cnpq.br/buscatextual/visualizacv.do` usa a seguinte estrutura HTML:
   - Nome: `div#nome` ou `h2.nome`
   - Seções: `div.layout-cell-pad-main > div` com `h1` indicando o título da seção (ex: "Formação acadêmica e titulação", "Artigos completos publicados em periódicos", etc.)
   - Cada item de produção dentro das seções usa `div.cita-artigo`, `div.layout-cell-01`, `div.layout-cell-02`
   - Resumo: `div#resumo` ou `p` dentro de `div.resumo`

4. **Frontend** — atualizar o placeholder e a validação no `LattesUrlInput.jsx`:
```jsx
// Placeholder correto:
placeholder="https://buscatextual.cnpq.br/buscatextual/visualizacv.do?id=XXXXXXXXXXXXXXXX"

// Validação:
const isValid = url.includes('buscatextual.cnpq.br') || url.includes('lattes.cnpq.br')
```

5. **README** — atualizar o exemplo de URL para usar o formato correto:
```
https://buscatextual.cnpq.br/buscatextual/visualizacv.do?id=XXXXXXXXXXXXXXXX
```

Por favor, aplique essas correções antes de finalizar a implementação.
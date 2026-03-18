# CV Lattes → Currículo Vitae

Converte um currículo da **Plataforma Lattes (CNPq)** — via upload de XML ou link direto — em PDF ou Word profissional.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 17 · Spring Boot 3.2 · Maven |
| Parsing XML | Java DOM (javax.xml) |
| Scraping HTML | Jsoup 1.17.2 |
| Geração de PDF | Thymeleaf (template HTML) + Flying Saucer |
| Geração de Word | Apache POI (`poi-ooxml` 5.2.5) |
| Frontend | React 18 · Vite 5 · CSS Modules |

## Como executar

### Requisitos

- Java 17+
- Maven 3.8+
- Node.js 18+

### 1. Backend (porta 8080)

```bash
cd backend
mvn spring-boot:run
```

### 2. Frontend (porta 5173)

```bash
cd frontend
npm install
npm run dev
```

Acesse: **http://localhost:5173**

## Uso

### Modo 1 — Upload de arquivo XML

1. Abra `http://localhost:5173` no navegador.
2. Selecione a aba **📤 Upload XML**.
3. Exporte seu currículo da Plataforma Lattes: acesse [lattes.cnpq.br](https://lattes.cnpq.br), abra seu currículo e use **Exportar → XML**.
4. Arraste o arquivo `.xml` para a área de upload (ou clique para selecionar).
5. Visualize o preview das seções extraídas.
6. Clique em **"Baixar Currículo em PDF"** ou **"Baixar Currículo em Word"** para gerar o arquivo.

### Modo 2 — URL do Lattes (scraping)

1. Abra `http://localhost:5173` no navegador.
2. Selecione a aba **🔗 URL do Lattes**.
3. Cole a URL pública do perfil Lattes (ex: `https://buscatextual.cnpq.br/buscatextual/visualizacv.do?id=XXXXXXXXXXXXXXXX`).
4. Clique em **"Buscar e Converter"**.
5. Visualize o preview e faça o download em PDF ou Word.

> **Nota:** O perfil Lattes deve estar configurado como **público** para que o scraping funcione.

## Seções convertidas

- Dados pessoais (nome, e-mail, ORCID, ID Lattes, instituição)
- Resumo
- Áreas de atuação
- Formação acadêmica (Doutorado, Mestrado, Graduação, Especialização, etc.)
- Formação complementar (cursos, extensão)
- Experiência profissional
- Artigos publicados
- Livros publicados / organizados
- Capítulos de livros
- Projetos de pesquisa
- Orientações (concluídas e em andamento)
- Prêmios e distinções
- Idiomas

## API Endpoints

### Upload XML

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/parse` | Recebe XML, retorna JSON com os dados do currículo |
| `POST` | `/api/convert` | Recebe XML, retorna PDF para download |
| `POST` | `/api/convert/word` | Recebe XML, retorna Word (.docx) para download |

Todos aceitam `multipart/form-data` com um campo chamado `file`.

### Scraping por URL

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/scrape` | Recebe `{ "url": "..." }`, retorna JSON com os dados do currículo |
| `POST` | `/api/scrape/convert` | Recebe `{ "url": "..." }`, retorna PDF para download |
| `POST` | `/api/scrape/convert/word` | Recebe `{ "url": "..." }`, retorna Word (.docx) para download |

Todos aceitam `application/json` com o campo `url` apontando para um perfil público do domínio `buscatextual.cnpq.br` ou `lattes.cnpq.br`.

**Exemplo de request:**
```json
POST /api/scrape
Content-Type: application/json

{ "url": "https://buscatextual.cnpq.br/buscatextual/visualizacv.do?id=XXXXXXXXXXXXXXXX" }
```

## Segurança

- O parser XML tem proteção contra ataques XXE (eXternal Entity Injection).
- Upload máximo: 10 MB.
- Apenas arquivos `.xml` são aceitos no modo de upload.
- CORS configurado apenas para `localhost:5173` e `localhost:3000`.
- Scraping por URL: apenas URLs dos domínios `lattes.cnpq.br` e `buscatextual.cnpq.br` são aceitas (validação no backend e no frontend), prevenindo SSRF.

## Limitações do scraping

- O perfil Lattes deve estar configurado como **público** no CNPq.
- O Jsoup captura o HTML estático da página. Seções renderizadas exclusivamente via JavaScript podem não ser capturadas.
- O CNPq pode alterar a estrutura HTML da página do Lattes a qualquer momento, o que pode impactar a extração de dados.

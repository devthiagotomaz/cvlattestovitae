# CV Lattes → Currículo Vitae

Converte um currículo exportado da **Plataforma Lattes (CNPq)** em PDF, com layout profissional.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 17 · Spring Boot 3.2 · Maven |
| Parsing XML | Java DOM (javax.xml) |
| Geração de PDF | Thymeleaf (template HTML) + Flying Saucer |
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

1. Abra `http://localhost:5173` no navegador.
2. Exporte seu currículo da Plataforma Lattes: acesse [lattes.cnpq.br](https://lattes.cnpq.br), abra seu currículo e use **Exportar → XML**.
3. Arraste o arquivo `.xml` para a área de upload (ou clique para selecionar).
4. Visualize o preview das seções extraídas.
5. Clique em **"Baixar Currículo em PDF"** para gerar o arquivo.

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

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/parse` | Recebe XML, retorna JSON com os dados do currículo |
| `POST` | `/api/convert` | Recebe XML, retorna PDF para download |

Ambos aceitam `multipart/form-data` com um campo chamado `file`.

## Segurança

- O parser XML tem proteção contra ataques XXE (eXternal Entity Injection).
- Upload máximo: 10 MB.
- Apenas arquivos `.xml` são aceitos.
- CORS configurado apenas para `localhost:5173` e `localhost:3000`.

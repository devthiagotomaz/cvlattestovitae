# Instruções para Claude Code

## Estilo de resposta
- Responda sempre em português
- Respostas curtas e diretas (máximo 5 linhas, salvo quando pedido detalhe)
- Após concluir uma tarefa: confirme apenas com ✅ e o resultado essencial
- Nunca listar arquivos modificados a menos que solicitado
- Logs de build/compilação: mostrar apenas erros ou a linha final de sucesso
- Sem introduções, sem resumos ao final, sem explicações não solicitadas

## Contexto do projeto
- Backend: Spring Boot (Java 17), porta 8080, iniciado com `mvn spring-boot:run` no diretório `backend/`
- Frontend: React + Vite, porta 5173, iniciado com `npm run dev` no diretório `frontend/`
- Scraper: `LattesScraperService.java` — extrai dados de currículos Lattes via HTML (buscatextual.cnpq.br)
- Parser: `LattesParserService.java` — extrai dados via XML exportado do Lattes

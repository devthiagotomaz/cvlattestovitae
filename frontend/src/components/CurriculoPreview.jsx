import SectionCard from './SectionCard'
import styles from './CurriculoPreview.module.css'

export default function CurriculoPreview({ curriculo }) {
  if (!curriculo) return null

  return (
    <div className={styles.container}>
      {/* ---- HEADER PESSOAL ---- */}
      <div className={styles.profileHeader}>
        <h1 className={styles.name}>{curriculo.nomeCompleto || '—'}</h1>
        <div className={styles.meta}>
          {curriculo.lattesId && (
            <span className={styles.lattesTag}>Lattes: {curriculo.lattesId}</span>
          )}
          {curriculo.orcid && <span>ORCID: {curriculo.orcid}</span>}
          {curriculo.email && <span>✉ {curriculo.email}</span>}
          {curriculo.homePage && <span>🌐 {curriculo.homePage}</span>}
        </div>
        {curriculo.nomeInstituicaoEndereco && (
          <div className={styles.institution}>
            {curriculo.nomeInstituicaoEndereco}
            {curriculo.cidadeEndereco && ` · ${curriculo.cidadeEndereco}/${curriculo.ufEndereco}`}
          </div>
        )}
      </div>

      {/* ---- RESUMO ---- */}
      {curriculo.resumo && (
        <SectionCard title="Resumo" count={null} defaultOpen={true}>
          <p className={styles.summary}>{curriculo.resumo}</p>
        </SectionCard>
      )}

      {/* ---- ÁREAS DE ATUAÇÃO ---- */}
      {curriculo.areasAtuacao?.length > 0 && (
        <SectionCard title="Áreas de Atuação" count={curriculo.areasAtuacao.length}>
          <div className={styles.tagList}>
            {curriculo.areasAtuacao.map((a, i) => (
              <span key={i} className={styles.tag}>
                {[a.grandeArea, a.area, a.subArea, a.especialidade].filter(Boolean).join(' › ')}
              </span>
            ))}
          </div>
        </SectionCard>
      )}

      {/* ---- FORMAÇÃO ACADÊMICA ---- */}
      {curriculo.formacoes?.length > 0 && (
        <SectionCard title="Formação Acadêmica" count={curriculo.formacoes.length}>
          {curriculo.formacoes.map((f, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryHeader}>
                <span className={styles.badge}>{f.tipo}</span>
                <span className={styles.entryTitle}>{f.nomeCurso}</span>
              </div>
              <div className={styles.entryInstitution}>{f.nomeInstituicao}</div>
              <div className={styles.entryMeta}>
                {f.anoInicio && `${f.anoInicio} – ${f.anoConclusao || 'Atual'}`}
                {f.statusCurso === 'EM_ANDAMENTO' && ' (Em andamento)'}
              </div>
              {f.tituloDissertacao && (
                <div className={styles.entryDesc}>
                  <strong>Título:</strong> {f.tituloDissertacao}
                </div>
              )}
              {f.nomeOrientador && (
                <div className={styles.entryMeta}>
                  <strong>Orientador(a):</strong> {f.nomeOrientador}
                </div>
              )}
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- FORMAÇÃO COMPLEMENTAR ---- */}
      {curriculo.formacoesComplementares?.length > 0 && (
        <SectionCard title="Formação Complementar" count={curriculo.formacoesComplementares.length} defaultOpen={false}>
          {curriculo.formacoesComplementares.map((fc, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryHeader}>
                <span className={styles.badge}>{fc.tipo}</span>
                <span className={styles.entryTitle}>{fc.nomeCurso}</span>
              </div>
              <div className={styles.entryInstitution}>{fc.nomeInstituicao}</div>
              <div className={styles.entryMeta}>
                {fc.anoInicio}{fc.anoConclusao ? ` – ${fc.anoConclusao}` : ''}
                {fc.cargaHoraria && ` | ${fc.cargaHoraria}h`}
              </div>
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- EXPERIÊNCIA PROFISSIONAL ---- */}
      {curriculo.atuacoes?.length > 0 && (
        <SectionCard title="Experiência Profissional" count={curriculo.atuacoes.length}>
          {curriculo.atuacoes.map((a, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryTitle}>{a.nomeInstituicao}</div>
              <div className={styles.entryInstitution}>{a.tipoVinculo}</div>
              <div className={styles.entryMeta}>
                {a.anoInicio && (
                  <>
                    {a.mesInicio ? `${a.mesInicio}/` : ''}{a.anoInicio}
                    {a.anoFim ? ` – ${a.mesFim ? `${a.mesFim}/` : ''}${a.anoFim}` : ' – Atual'}
                  </>
                )}
              </div>
              {a.descricao && <div className={styles.entryDesc}>{a.descricao}</div>}
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- ARTIGOS ---- */}
      {curriculo.artigosPublicados?.length > 0 && (
        <SectionCard title="Artigos Publicados" count={curriculo.artigosPublicados.length} defaultOpen={false}>
          {curriculo.artigosPublicados.map((p, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryHeader}>
                <span className={styles.numBadge}>{i + 1}</span>
                <span className={styles.entryTitle}>{p.titulo}</span>
              </div>
              {p.autores && <div className={styles.entryDesc}>{p.autores}</div>}
              <div className={styles.entryMeta}>
                {p.veiculo && `${p.veiculo}`}
                {p.volume && `, v.${p.volume}`}
                {p.numero && `, n.${p.numero}`}
                {p.paginas && `, p.${p.paginas}`}
                {p.ano && ` (${p.ano})`}
              </div>
              {p.doi && <div className={styles.doi}>DOI: {p.doi}</div>}
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- LIVROS ---- */}
      {curriculo.livros?.length > 0 && (
        <SectionCard title="Livros Publicados / Organizados" count={curriculo.livros.length} defaultOpen={false}>
          {curriculo.livros.map((p, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryHeader}>
                <span className={styles.numBadge}>{i + 1}</span>
                <span className={styles.entryTitle}>{p.titulo}</span>
              </div>
              {p.autores && <div className={styles.entryDesc}>{p.autores}</div>}
              <div className={styles.entryMeta}>
                {p.editora}{p.cidade ? `, ${p.cidade}` : ''}{p.ano ? ` (${p.ano})` : ''}
              </div>
              {p.doi && <div className={styles.doi}>DOI: {p.doi}</div>}
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- CAPÍTULOS ---- */}
      {curriculo.capitulosLivro?.length > 0 && (
        <SectionCard title="Capítulos de Livros" count={curriculo.capitulosLivro.length} defaultOpen={false}>
          {curriculo.capitulosLivro.map((p, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryHeader}>
                <span className={styles.numBadge}>{i + 1}</span>
                <span className={styles.entryTitle}>{p.titulo}</span>
              </div>
              {p.autores && <div className={styles.entryDesc}>{p.autores}</div>}
              <div className={styles.entryMeta}>
                {p.veiculo && `In: ${p.veiculo}`}
                {p.editora && `. ${p.editora}`}
                {p.paginas && `, p.${p.paginas}`}
                {p.ano && ` (${p.ano})`}
              </div>
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- PROJETOS ---- */}
      {curriculo.projetos?.length > 0 && (
        <SectionCard title="Projetos de Pesquisa" count={curriculo.projetos.length} defaultOpen={false}>
          {curriculo.projetos.map((p, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryTitle}>{p.titulo}</div>
              {p.nomeOrgao && <div className={styles.entryInstitution}>{p.nomeOrgao}</div>}
              <div className={styles.entryMeta}>
                {p.anoInicio && `${p.anoInicio} – ${p.anoFim || 'Atual'}`}
                {p.situacao && ` | ${p.situacao}`}
              </div>
              {p.descricao && <div className={styles.entryDesc}>{p.descricao}</div>}
              {p.linhasPesquisa?.length > 0 && (
                <div className={styles.tagList}>
                  {p.linhasPesquisa.map((l, j) => <span key={j} className={styles.tag}>{l}</span>)}
                </div>
              )}
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- ORIENTAÇÕES ---- */}
      {curriculo.orientacoes?.length > 0 && (
        <SectionCard title="Orientações" count={curriculo.orientacoes.length} defaultOpen={false}>
          {curriculo.orientacoes.map((o, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryHeader}>
                <span className={styles.badge}>{o.tipo}</span>
                <span className={styles.entryTitle}>{o.titulo}</span>
              </div>
              {o.nomeOrientando && (
                <div className={styles.entryInstitution}>Orientando(a): {o.nomeOrientando}</div>
              )}
              <div className={styles.entryMeta}>
                {o.nomeInstituicao}{o.anoConclusao ? ` (${o.anoConclusao})` : ''}
              </div>
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- PRÊMIOS ---- */}
      {curriculo.premios?.length > 0 && (
        <SectionCard title="Prêmios e Distinções" count={curriculo.premios.length} defaultOpen={false}>
          {curriculo.premios.map((p, i) => (
            <div key={i} className={styles.entry}>
              <div className={styles.entryTitle}>{p.nome}</div>
              <div className={styles.entryInstitution}>{p.entidade}</div>
              <div className={styles.entryMeta}>{p.ano}</div>
            </div>
          ))}
        </SectionCard>
      )}

      {/* ---- IDIOMAS ---- */}
      {curriculo.idiomas?.length > 0 && (
        <SectionCard title="Idiomas" count={curriculo.idiomas.length} defaultOpen={false}>
          <div className={styles.idiomaGrid}>
            {curriculo.idiomas.map((id, i) => (
              <div key={i} className={styles.idiomaRow}>
                <strong>{id.nome}</strong>
                <span>
                  {id.proficienciaLeitura && `Leitura: ${id.proficienciaLeitura}`}
                  {id.proficienciaEscrita && ` · Escrita: ${id.proficienciaEscrita}`}
                  {id.proficienciaConversacao && ` · Fala: ${id.proficienciaConversacao}`}
                  {id.proficienciaCompreensao && ` · Compreensão: ${id.proficienciaCompreensao}`}
                </span>
              </div>
            ))}
          </div>
        </SectionCard>
      )}
    </div>
  )
}

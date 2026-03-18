import { useState } from 'react'
import styles from './SectionCard.module.css'

export default function SectionCard({ title, count, children, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen)

  if (!count && count !== 0) return null

  return (
    <div className={styles.card}>
      <button
        className={styles.header}
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        <span className={styles.title}>{title}</span>
        {count != null && <span className={styles.badge}>{count}</span>}
        <span className={styles.chevron}>{open ? '▲' : '▼'}</span>
      </button>
      {open && <div className={styles.body}>{children}</div>}
    </div>
  )
}

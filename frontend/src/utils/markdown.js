function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

function inlineHtml(text) {
  return escapeHtml(text)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

/**
 * 简易 Markdown → HTML 解析器。
 * 支持：# 标题、**粗体**、*斜体*、- 无序列表、1. 有序列表、> 引用、--- 分隔线、段落。
 * 只用于渲染项目自带的静态 .md 教程，因此不做 XSS 过滤（内容可控）。
 */
export function parseMarkdown(md) {
  if (!md) return ''
  const raw = String(md).replace(/\r\n/g, '\n').trim()
  if (!raw) return ''

  const blocks = raw.split(/\n\n+/)

  return blocks.map((block) => {
    const lines = block.split('\n')
    const first = lines[0]

    // 标题：# ～ ######
    const headingMatch = first.match(/^(#{1,6}) /)
    if (headingMatch && lines.length === 1) {
      const level = headingMatch[1].length
      return `<h${level}>${inlineHtml(first.slice(level + 1))}</h${level}>`
    }

    // 分隔线
    if (/^-{3,}$|^\*{3,}$/.test(block.trim())) {
      return '<hr />'
    }

    // 无序列表
    if (lines.every((line) => /^- /.test(line))) {
      const items = lines
        .map((line) => `<li>${inlineHtml(line.slice(2))}</li>`)
        .join('')
      return `<ul>${items}</ul>`
    }

    // 有序列表
    if (lines.every((line) => /^\d+\. /.test(line))) {
      const items = lines
        .map((line) => `<li>${inlineHtml(line.replace(/^\d+\. /, ''))}</li>`)
        .join('')
      return `<ol>${items}</ol>`
    }

    // 引用
    if (lines.every((line) => /^> /.test(line))) {
      const content = lines
        .map((line) => inlineHtml(line.slice(2)))
        .join('<br />')
      return `<blockquote>${content}</blockquote>`
    }

    // 普通段落（保留段内换行）
    return `<p>${inlineHtml(lines.join('<br />'))}</p>`
  }).join('\n')
}

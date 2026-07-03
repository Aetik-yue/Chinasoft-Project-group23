export const currentParrot = {
  id: 'sun-001',
  name: '小太阳 · 啾啾',
  avatarType: 'avatar-orange',
  status: '站立',
  route: '/parrots/sun-001',
}

export const entryCards = {
  archive: {
    key: 'archive',
    title: '鹦鹉档案',
    theme: 'purple',
    visual: 'archive',
    route: '/archive',
  },
  growth: {
    key: 'growth',
    title: '成长报告',
    theme: 'purple',
    visual: 'growth',
    route: '/growth-report',
    badge: 2,
  },
  settings: {
    key: 'settings',
    title: '用户设置',
    theme: 'green',
    visual: 'settings',
    route: '/settings',
  },
  medical: {
    key: 'medical',
    title: '医疗助手',
    theme: 'orange',
    visual: 'medical',
    route: '/medical-assistant',
    badge: 1,
  },
  ledger: {
    key: 'ledger',
    title: '记账本',
    theme: 'blue',
    visual: 'ledger',
    route: '/ledger',
  },
  handbook: {
    key: 'handbook',
    title: '饲养手册',
    theme: 'lavender',
    visual: 'handbook',
    route: '/care-handbook',
  },
}

export const primaryCards = {
  monitor: {
    key: 'monitor',
    title: '实时监控与状态',
    route: '/monitor',
    statusLabel: '当前状态：站立',
    online: true,
  },
}

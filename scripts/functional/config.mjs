/** 功能 / API 黑盒测试共享配置（可通过环境变量覆盖） */
export const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
export const OUT_DIR = process.env.OUT_DIR || 'test-results';

export const ACCOUNTS = {
  admin: {
    role: 'admin',
    username: process.env.ADMIN_USER || 'platformadmin',
    password: process.env.ADMIN_PASS || 'AdminSecurePassword123',
  },
  adminAlt: {
    role: 'admin',
    username: process.env.ADMIN_ALT_USER || 'admin',
    password: process.env.ADMIN_ALT_PASS || 'admin123',
  },
  merchant: {
    role: 'merchant',
    username: process.env.MERCHANT_USER || 'testmerchant',
    password: process.env.MERCHANT_PASS || 'testmerchantPASSWORD',
  },
  customer: {
    role: 'customer',
    username: process.env.CUSTOMER_USER || 'clientuser',
    password: process.env.CUSTOMER_PASS || 'ClientSecurePassword789',
  },
  customerAlt: {
    role: 'customer',
    username: process.env.CUSTOMER_ALT_USER || 'alice_test',
    password: process.env.CUSTOMER_ALT_PASS || 'AliceTestPassword123',
  },
  customerB: {
    role: 'customer',
    username: process.env.CUSTOMER_B_USER || 'bob_test',
    password: process.env.CUSTOMER_B_PASS || 'BobTestPassword123',
  },
  customerC: {
    role: 'customer',
    username: process.env.CUSTOMER_C_USER || 'carol_test',
    password: process.env.CUSTOMER_C_PASS || 'CarolTestPassword123',
  },
  customerD: {
    role: 'customer',
    username: process.env.CUSTOMER_D_USER || 'david_test',
    password: process.env.CUSTOMER_D_PASS || 'DavidTestPassword123',
  },
  customerE: {
    role: 'customer',
    username: process.env.CUSTOMER_E_USER || 'eve_test',
    password: process.env.CUSTOMER_E_PASS || 'EveTestPassword123',
  },
};

/** 匿名可访问的公开页面（黑盒冒烟） */
export const PUBLIC_PAGES = [
  { path: '/', name: '首页', expectText: null },
  { path: '/products', name: '商品列表', expectText: null },
  { path: '/explore', name: '探索', expectText: null },
  { path: '/new-arrivals', name: '新品', expectText: null },
  { path: '/top-selling', name: '热销', expectText: null },
  { path: '/help', name: '帮助', expectText: null },
  { path: '/about', name: '关于', expectText: null },
  { path: '/login', name: '登录页', expectText: '登录' },
  { path: '/register', name: '注册页', expectText: '注册' },
];

/** 各角色登录后可访问的受保护页面 */
export const ROLE_PAGES = {
  customer: [
    { path: '/cart', name: '购物车' },
    { path: '/favorites', name: '收藏' },
    { path: '/orders', name: '订单列表' },
    { path: '/profile', name: '个人资料' },
  ],
  merchant: [
    { path: '/merchant', name: '商家控制台' },
    { path: '/merchant/orders', name: '商家订单' },
    { path: '/merchant/customers', name: '商家客户' },
    { path: '/merchant/statistics', name: '商家统计' },
  ],
  admin: [
    { path: '/admin', name: '管理控制台' },
    { path: '/admin/users', name: '用户管理' },
    { path: '/admin/products', name: '商品管理' },
    { path: '/admin/orders', name: '订单管理' },
    { path: '/admin/analytics/dashboard', name: '数据分析' },
  ],
};

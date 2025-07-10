/**
 * 时间格式化工具函数
 */

/**
 * 格式化时间为友好的显示格式
 * @param {string|Date} date - 时间
 * @param {string} format - 格式类型：'full', 'date', 'time', 'relative'
 * @returns {string} 格式化后的时间字符串
 */
export function formatDate(date, format = 'full') {
  if (!date) return '';
  
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  const diffDays = Math.floor(diff / (1000 * 60 * 60 * 24));
  
  // 根据格式类型返回不同的格式
  switch (format) {
    case 'full':
      return formatFullDate(d);
    case 'date':
      return formatDateOnly(d);
    case 'time':
      return formatTimeOnly(d);
    case 'relative':
      return formatRelativeTime(d, now, diff, diffDays);
    case 'smart':
      return formatSmartDate(d, now, diffDays);
    default:
      return formatFullDate(d);
  }
}

/**
 * 格式化完整日期时间 (2025-01-06 14:30)
 */
function formatFullDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

/**
 * 格式化仅日期 (2025-01-06)
 */
function formatDateOnly(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
}

/**
 * 格式化仅时间 (14:30)
 */
function formatTimeOnly(date) {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  
  return `${hours}:${minutes}`;
}

/**
 * 格式化相对时间 (刚刚、5分钟前、1小时前等)
 */
function formatRelativeTime(date, now, diff, diffDays) {
  const diffSeconds = Math.floor(diff / 1000);
  const diffMinutes = Math.floor(diff / (1000 * 60));
  const diffHours = Math.floor(diff / (1000 * 60 * 60));
  
  if (diffSeconds < 60) {
    return '刚刚';
  } else if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`;
  } else if (diffHours < 24) {
    return `${diffHours}小时前`;
  } else if (diffDays < 30) {
    return `${diffDays}天前`;
  } else {
    return formatDateOnly(date);
  }
}

/**
 * 智能格式化时间 (今天显示时间，昨天显示"昨天"，更早显示日期)
 */
function formatSmartDate(date, now, diffDays) {
  const isToday = now.toDateString() === date.toDateString();
  const isYesterday = diffDays === 1;
  const isThisYear = now.getFullYear() === date.getFullYear();
  
  if (isToday) {
    return `今天 ${formatTimeOnly(date)}`;
  } else if (isYesterday) {
    return `昨天 ${formatTimeOnly(date)}`;
  } else if (diffDays < 7) {
    const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
    const weekDay = weekDays[date.getDay()];
    return `周${weekDay} ${formatTimeOnly(date)}`;
  } else if (isThisYear) {
    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${month}月${day}日 ${formatTimeOnly(date)}`;
  } else {
    return formatFullDate(date);
  }
}

/**
 * 格式化文件修改时间（专门用于文件列表）
 */
export function formatFileTime(date) {
  return formatDate(date, 'smart');
}

/**
 * 格式化分享时间（专门用于分享相关页面）
 */
export function formatShareTime(date) {
  return formatDate(date, 'full');
}

/**
 * 获取时间段描述
 */
export function getTimeDesc(date) {
  if (!date) return '';
  
  const d = new Date(date);
  const hours = d.getHours();
  
  if (hours >= 6 && hours < 12) {
    return '上午';
  } else if (hours >= 12 && hours < 18) {
    return '下午';
  } else if (hours >= 18 && hours < 24) {
    return '晚上';
  } else {
    return '凌晨';
  }
}

/**
 * 计算分享过期时间
 */
export function calculateExpireTime(shareTime, validType) {
  if (!shareTime || validType === 3) return '永久有效';
  
  const validDays = {
    0: 1,   // 1天
    1: 7,   // 7天
    2: 30   // 30天
  }[validType] || 0;
  
  const expireDate = new Date(shareTime);
  expireDate.setDate(expireDate.getDate() + validDays);
  
  const now = new Date();
  if (now > expireDate) {
    return '已过期';
  }
  
  return `${formatDate(expireDate, 'full')} 过期`;
}

/**
 * 判断时间是否为今天
 */
export function isToday(date) {
  if (!date) return false;
  const d = new Date(date);
  const today = new Date();
  return d.toDateString() === today.toDateString();
}

/**
 * 判断时间是否为昨天
 */
export function isYesterday(date) {
  if (!date) return false;
  const d = new Date(date);
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  return d.toDateString() === yesterday.toDateString();
} 
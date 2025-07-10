import { post, getHeader } from '@/net/index.js';
import axios from 'axios';

/**
 * 保存分享文件到用户网盘
 * @param {string} shareId - 分享ID
 * @param {string} shareFileIds - 要保存的文件ID（多个用逗号分隔）
 * @param {string} myFolderId - 目标文件夹ID
 * @param {string} code - 分享码
 * @returns {Promise} 
 */
export const saveShareToMyPan = async (shareId, shareFileIds, myFolderId, code) => {
  try {
    // 获取用户授权头
    const userHeaders = getHeader();
    
    // 构造请求头，包含用户认证信息
    const headers = {
      ...userHeaders,
      'Content-Type': 'application/json'
    };
    
    // 调用后端保存接口
    const response = await axios.post('/showShare/saveShare', {
      shareId,
      shareFileIds,
      myFolderId,
      code
    }, { headers });
    
    if (response.data.code === 200) {
      return response.data.data;
    } else {
      throw new Error(response.data.message || '保存失败');
    }
  } catch (error) {
    // 处理不同类型的错误
    if (error.response) {
      const { status, data } = error.response;
      switch (status) {
        case 401:
          // 不直接抛出错误，而是返回一个特殊的错误对象
          return { needRelogin: true, message: '登录已过期，请重新登录' };
        case 403:
          throw new Error('无权限操作或分享已失效');
        case 404:
          throw new Error('分享不存在');
        case 500:
          if (data.message && data.message.includes('自己分享的文件')) {
            throw new Error('不能保存自己分享的文件');
          }
          throw new Error(data.message || '服务器内部错误');
        default:
          throw new Error(data.message || '保存失败，请重试');
      }
    } else if (error.request) {
      throw new Error('网络连接异常，请检查网络后重试');
    } else {
      throw new Error(error.message || '未知错误');
    }
  }
};

/**
 * 检查分享码
 * @param {string} shareId - 分享ID
 * @param {string} code - 分享码
 * @returns {Promise}
 */
export const checkShareCode = (shareId, code) => {
  return post('/showShare/checkShareCode', { shareId, code });
};

/**
 * 获取分享信息
 * @param {string} shareId - 分享ID
 * @returns {Promise}
 */
export const getShareInfo = (shareId) => {
  return post('/showShare/getShareInfo', { shareId });
}; 
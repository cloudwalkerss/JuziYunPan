<template>
  <div class="video-preview">
    <div ref="playerRef"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { getHeader } from '@/net/index.js';
import DPlayer from 'dplayer';
import Hls from 'hls.js';

const props = defineProps({
  fileId: {
    type: String,
    required: true
  },
  visible: {
    type: Boolean,
    default: true
  },
  isAdmin: {
    type: Boolean,
    default: false
  }
});

const playerRef = ref(null);
let player = null;

// 销毁播放器
const destroyPlayer = () => {
  if (player) {
    player.pause();
    player.destroy();
    player = null;
  }
};

// 监听visible属性变化
watch(() => props.visible, (newValue) => {
  if (!newValue) {
    destroyPlayer();
  }
}, { immediate: true });

onMounted(() => {
  const headers = getHeader();
  const token = headers['Authorization'] || '';
  
  // 获取后端API基础URL
  const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  
  // 构建m3u8和ts请求的基础URL
  const m3u8Url = `${apiBaseUrl}/${props.isAdmin ? 'admin' : 'file'}/getFile/${props.fileId}`;
  
  // 判断是否是管理员模式，如果是，则需要处理用户ID和文件ID
  let userId = '';
  let fileId = '';
  if (props.isAdmin && props.fileId.includes('/')) {
    const parts = props.fileId.split('/');
    userId = parts[0];
    fileId = parts[1];
  }
  
  // 根据是否为管理员模式构建不同的ts基础URL
  const tsBaseUrl = props.isAdmin 
    ? `${apiBaseUrl}/admin/ts/getVideoInfo/${userId}` 
    : `${apiBaseUrl}/file/ts/getVideoInfo`;

  player = new DPlayer({
    container: playerRef.value,
    autoplay: false,
    theme: '#42b883',
    video: {
      url: m3u8Url,
      type: 'customHls',
      customType: {
        customHls: function (video, player) {
          if (Hls.isSupported()) {
            const hls = new Hls({
              xhrSetup: function (xhr, url) {
                xhr.setRequestHeader('Authorization', token);
              },
              maxBufferLength: 30,
              maxMaxBufferLength: 60,
              manifestLoadingMaxRetry: 2,
              manifestLoadingRetryDelay: 1000,
              manifestLoadingMaxRetryTimeout: 8000,
              fragLoadingMaxRetry: 2,
              fragLoadingRetryDelay: 1000,
              fragLoadingMaxRetryTimeout: 8000
            });

            // 先获取m3u8文件
            fetch(m3u8Url, {
              headers: {
                'Authorization': token
              }
            })
            .then(response => {
              if (response.ok) {
                return response.text();
              }
              console.error(`获取m3u8失败: 状态码=${response.status}, URL=${m3u8Url}`);
              throw new Error(`获取m3u8失败: ${response.status}`);
            })
            .then(m3u8Content => {
              if (m3u8Content.includes('#EXTM3U')) {
                // 根据是否为管理员模式处理ts文件路径
                console.log('成功获取m3u8内容，正在处理ts文件路径');
                console.log('tsBaseUrl:', tsBaseUrl);
                
                const modifiedM3u8 = m3u8Content.replace(
                  /(.*\.ts)/g,
                  (match) => `${tsBaseUrl}/${match}`
                );
                
                // 输出修改后的m3u8内容（仅用于调试）
                console.log('修改后的m3u8内容片段:', modifiedM3u8.substring(0, 200) + '...');
                
                const blob = new Blob([modifiedM3u8], { type: 'application/vnd.apple.mpegurl' });
                const m3u8BlobUrl = URL.createObjectURL(blob);
                
                hls.loadSource(m3u8BlobUrl);
                hls.attachMedia(video);

                hls.on(Hls.Events.MANIFEST_PARSED, function() {
                  URL.revokeObjectURL(m3u8BlobUrl);
                });

                // 添加HLS销毁逻辑
                player.on('destroy', () => {
                  hls.destroy();
                });
              } else {
                throw new Error('无效的m3u8文件');
              }
            })
            .catch(error => {
              console.error('视频加载失败:', error);
            });

          } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            // Safari原生支持HLS的处理...
          }
        }
      }
    }
  });

  // 播放器事件监听
  player.on('error', (e) => {
    console.error('DPlayer错误:', e);
  });

  player.on('loadstart', () => {
    console.log('开始加载视频');
  });

  player.on('loadeddata', () => {
    console.log('视频数据加载完成');
  });
});

onBeforeUnmount(() => {
  destroyPlayer();
});
</script>

<style>
.video-preview {
  width: 100%;
  height: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-preview > div {
  width: 100%;
  height: 100%;
  max-width: 1200px;
  aspect-ratio: 16 / 9;
}

/* DPlayer必要的样式 */
.dplayer {
  position: relative;
  overflow: hidden;
  user-select: none;
  line-height: 1;
}

.dplayer-video-wrap {
  position: relative;
  background: #000;
  font-size: 0;
  width: 100%;
  height: 100%;
}

.dplayer video {
  width: 100%;
  height: 100%;
}

.dplayer-controller {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50px;
  padding: 0 20px;
  user-select: none;
  transition: all 0.3s ease;
  background: rgba(0, 0, 0, 0.5);
}

.dplayer-bar-wrap {
  position: relative;
  height: 3px;
  width: 100%;
  background: rgba(255, 255, 255, 0.2);
  cursor: pointer;
}

.dplayer-bar {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: #42b883;
  transition: all 0.3s ease;
}

.dplayer-loaded {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.dplayer-controller .dplayer-icons {
  display: flex;
  align-items: center;
  height: 100%;
}

.dplayer-controller .dplayer-icon {
  width: 40px;
  height: 100%;
  border: none;
  background-color: transparent;
  outline: none;
  cursor: pointer;
  vertical-align: middle;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.dplayer-controller .dplayer-time {
  color: #eee;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5);
  font-size: 13px;
  cursor: default;
}

.dplayer-controller .dplayer-icon .dplayer-icon-content {
  opacity: .8;
}

.dplayer-controller .dplayer-icon:hover .dplayer-icon-content {
  opacity: 1;
}

.dplayer-notice {
  position: absolute;
  top: 20px;
  left: 20px;
  border-radius: 2px;
  background: rgba(28, 28, 28, 0.9);
  padding: 7px 20px;
  color: #fff;
  font-size: 12px;
  pointer-events: none;
  opacity: 0;
  transition: opacity .3s ease-in-out;
}

.dplayer-notice.dplayer-notice-show {
  opacity: 1;
}
</style> 
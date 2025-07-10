<template>
  <div class="audio-player">
    <div class="player-container">
      <div class="player-visualization">
        <div class="album-art">
          <div class="vinyl-record" :class="{ 'spinning': isPlaying }">
            <div class="vinyl-label">
              <div class="center-hole"></div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="player-controls">
        <div class="time-progress">
          <span class="time-text">{{ formatTime(currentTime) }}</span>
          <el-slider
            v-model="currentTime"
            :min="0"
            :max="duration || 100"
            :step="0.1"
            @change="setProgress"
            class="progress-slider"
          />
          <span class="time-text">{{ formatTime(duration) }}</span>
        </div>
        
        <div class="control-buttons">
          <div class="main-controls">
            <button class="control-btn play-btn" @click="togglePlay">
              <el-icon size="24">
                <component :is="isPlaying ? VideoPause : VideoPlay" />
              </el-icon>
            </button>
          </div>
          
          <div class="volume-controls" @mouseenter="showVolumeSlider = true" @mouseleave="showVolumeSlider = false">
            <button class="control-btn volume-btn" @click="toggleMute">
              <el-icon>
                <component :is="isMuted ? Mute : Microphone" />
              </el-icon>
            </button>
            <div class="volume-slider-container" v-show="showVolumeSlider">
              <el-slider
                v-model="volume"
                :min="0"
                :max="1"
                :step="0.01"
                @change="setVolume"
                vertical
                height="80px"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <audio
      ref="audioRef"
      :src="audioUrl"
      @timeupdate="updateProgress"
      @ended="handleEnded"
      @loadedmetadata="updateProgress"
      preload="metadata"
    ></audio>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { ElSlider } from 'element-plus';
import { VideoPlay, VideoPause, Mute, Microphone } from '@element-plus/icons-vue';

const props = defineProps({
  audioUrl: {
    type: String,
    required: true
  }
});

const audioRef = ref(null);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const volume = ref(1);
const isMuted = ref(false);
const showVolumeSlider = ref(false);

// 格式化时间
function formatTime(time) {
  const minutes = Math.floor(time / 60);
  const seconds = Math.floor(time % 60);
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

// 播放/暂停
function togglePlay() {
  if (audioRef.value) {
    if (isPlaying.value) {
      audioRef.value.pause();
    } else {
      audioRef.value.play();
    }
    isPlaying.value = !isPlaying.value;
  }
}

// 更新进度
function updateProgress() {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime;
    duration.value = audioRef.value.duration;
  }
}

// 设置进度
function setProgress(value) {
  if (audioRef.value) {
    audioRef.value.currentTime = value;
    currentTime.value = value;
  }
}

// 设置音量
function setVolume(value) {
  if (audioRef.value) {
    volume.value = value;
    audioRef.value.volume = value;
    isMuted.value = value === 0;
  }
}

// 切换静音
function toggleMute() {
  if (audioRef.value) {
    if (isMuted.value) {
      audioRef.value.volume = volume.value;
    } else {
      audioRef.value.volume = 0;
    }
    isMuted.value = !isMuted.value;
  }
}

// 监听音频结束
function handleEnded() {
  isPlaying.value = false;
  currentTime.value = 0;
}

// 组件卸载时停止播放
onUnmounted(() => {
  if (audioRef.value) {
    audioRef.value.pause();
  }
});

// 监听props变化
watch(() => props.audioUrl, () => {
  if (audioRef.value) {
    audioRef.value.load();
    isPlaying.value = false;
    currentTime.value = 0;
    duration.value = 0;
  }
});
</script>

<style scoped>
.audio-player {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.player-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.player-visualization {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
}

.album-art {
  width: 200px;
  height: 200px;
  position: relative;
}

.vinyl-record {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(45deg, #000000, #333333);
  position: relative;
  transition: transform 0.5s ease;
}

.vinyl-record.spinning {
  animation: spin 4s linear infinite;
}

.vinyl-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 35%;
  height: 35%;
  border-radius: 50%;
  background: linear-gradient(45deg, #e6e6e6, #ffffff);
}

.center-hole {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20%;
  height: 20%;
  border-radius: 50%;
  background: #000000;
}

.player-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.time-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-text {
  font-size: 14px;
  color: #666666;
  min-width: 45px;
}

.progress-slider {
  flex: 1;
}

.control-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.main-controls {
  display: flex;
  gap: 16px;
  align-items: center;
}

.control-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.play-btn {
  width: 48px;
  height: 48px;
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-btn:hover {
  background: #66b1ff;
  transform: scale(1.05);
}

.volume-controls {
  position: relative;
}

.volume-slider-container {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  padding: 12px 8px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 自定义滑块样式 */
:deep(.el-slider__runway) {
  background-color: #e4e7ed;
}

:deep(.el-slider__bar) {
  background-color: #409EFF;
}

:deep(.el-slider__button) {
  border-color: #409EFF;
}

/* 删除自定义图标相关样式 */
@font-face {
  font-family: 'iconfont';
  src: url('//at.alicdn.com/t/font_2385839_1234567890.woff2') format('woff2');
}

.iconfont {
  font-family: 'iconfont' !important;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.icon-play:before { content: '\e6b0'; }
.icon-pause:before { content: '\e6af'; }
.icon-volume-high:before { content: '\e6a8'; }
.icon-volume-low:before { content: '\e6a9'; }
.icon-volume-mute:before { content: '\e6aa'; }

/* 添加Element Plus图标样式 */
.play-btn :deep(.el-icon) {
  color: white;
}

.volume-btn :deep(.el-icon) {
  font-size: 20px;
  color: #606266;
}
</style> 
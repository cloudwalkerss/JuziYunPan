import axios from "axios";

axios.defaults.baseURL='http://localhost:8080'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 导入Element Plus
import ElementPlus from 'element-plus'
import { DatePicker } from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
// 导入Element Plus样式
import 'element-plus/dist/index.css'
// 如果需要，也可以导入Element Plus的暗黑模式
// import 'element-plus/theme-chalk/dark/css-vars.css'

const app = createApp(App)
// 使用Element Plus
app.use(ElementPlus)
app.use(router)
app.use(DatePicker)
app.mount('#app')
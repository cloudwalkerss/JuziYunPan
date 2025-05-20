import axios from "axios";
import router from "@/router/index.js";
axios.defaults.baseURL='http://localhost:8080'
import { createApp } from 'vue'
import App from './App.vue'


// 导入Element Plus
import ElementPlus from 'element-plus'
import { DatePicker } from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
// 导入Element Plus样式
import 'element-plus/dist/index.css'
import{createPinia} from "pinia";
// 如果需要，也可以导入Element Plus的暗黑模式
// import 'element-plus/theme-chalk/dark/css-vars.css'

const app = createApp(App)
// 使用Element Plus
app.use(ElementPlus)
app.use(router)
app.use(DatePicker)
app.use(createPinia())
app.mount('#app')
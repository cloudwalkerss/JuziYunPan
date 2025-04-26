import {createRouter,createWebHistory} from 'vue-router'
import {unAuthorize} from "@/net/index.js";
import {ElMessage} from "element-plus";

const router    =createRouter({
    history:createWebHistory(import.meta.env.BASE_URL),
    routes:[
        {
            path:'/',
            name:'login',
            component:()=>import('@/views/Login.vue'),

        },
        {
            path:'/index',
            name:'index',
            component:()   =>import ('@/views/index.vue')
        },
        {
            path:'/register',
            name:'register',
            component:()=>import('@/views/Register.vue')
        },{
              path:'/resetPassword',
            name:'resetpassword',
            component:()=>import('@/views/resetPassword.vue')

        }

        ]
})
router.beforeEach((to,from,next)=>{
    const isUnAuthorize=unAuthorize()
    if(!unAuthorize()&&to.name.startsWith('/')){

        next('index');
        ElMessage.warning('您已经登录过了，需要先退出才能回到此界面噢')

    }
    else if(isUnAuthorize&&to.fullPath.startsWith('/index')){
        next('/')
        ElMessage.warning('您还未登录，请先进行登录操作噢')

    }
    else{
        next()
    }


})
export  default  router//需要导出才能使用
import {createRouter,createWebHistory} from 'vue-router'
import {unAuthorize} from "@/net/index.js";
import {ElMessage} from "element-plus";
import {userStore} from "@/store/index.js";

const routes = [
    {
        path: '/',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/index',
        name: 'Index',
        component: () => import('@/views/index.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: 'files',
                name: 'Files',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'videos',
                name: 'Videos',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'images',
                name: 'Images',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'documents',
                name: 'Documents',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'music',
                name: 'Music',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'others',
                name: 'Others',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'recycleBin',
                name: 'RecycleBin',
                component: () => import('@/views/index.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'share',
                name: 'MyShares',
                component: () => import('@/views/share.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'admin/users',
                name: 'UserManage',
                component: () => import('@/views/admin/UserManage.vue'),
                meta: { requiresAuth: true, requiresAdmin: true }
            },
            {
                path: 'admin/files',
                name: 'FileManage',
                component: () => import('@/views/admin/FileManage.vue'),
                meta: { requiresAuth: true, requiresAdmin: true }
            },
            {
                path: 'admin/storage',
                name: 'StorageManage',
                component: () => import('@/views/admin/StorageManage.vue'),
                meta: { requiresAuth: true, requiresAdmin: true }
            },
            {
                path: 'admin/shares',
                name: 'ShareManage',
                component: () => import('@/views/admin/ShareManage.vue'),
                meta: { requiresAuth: true, requiresAdmin: true }
            }
        ]
    },
    {
        path: '/share/:shareId',
        name: 'PublicShare',
        component: () => import('@/views/PublicShare.vue')
    },
    {
        path: '/register',
        name: 'register',
        component: () => import('@/views/Register.vue')
    },
    {
        path: '/resetPassword',
        name: 'resetpassword',
        component: () => import('@/views/resetPassword.vue')
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

router.beforeEach((to, from, next) => {
    const isUnAuthorize = unAuthorize();
    const store = userStore();
    
    // 检查是否需要管理员权限
    if (to.matched.some(record => record.meta.requiresAdmin)) {
        if (!store.user || Number(store.user.role) !== 1) {
            ElMessage.error('无权访问该页面');
            next('/index');
            return;
        }
    }

    if (to.meta.requiresAuth && isUnAuthorize) {
        next('/');
    } else {
        next();
    }
});

export default router;
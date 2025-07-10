import axios from 'axios'
import {ElMessage} from "element-plus";
const authItemName = 'access_token'


const defaultFailure=(message,code,url)=>{
    console.warn(`请求地址：${url},状态码：${code},错误信息：${message}`)
    ElMessage.warning(message)
}

const  defaultError=(err)=>{
    console.error(err)

    ElMessage.warning('发生了一些错误，请联系管理员')

}

function internalPost(url, data, header, success, failure, error = defaultError) {
    return new Promise((resolve, reject) => {
        axios.post(url, data, { headers: header }).then(({ data }) => {
            if (data.code === 200) {
                if (success) success(data.data); // 兼容旧的回调方式
                resolve(data.data);
            } else {
                if (failure) failure(data.message, data.code, url); // 兼容旧的回调方式
                reject(data);
            }
        }).catch(err => {
            if (error) error(err);
            reject(err);
        });
    });
}

function internalGet(url, header, success, failure, error = defaultError) {
    return new Promise((resolve, reject) => {
        axios.get(url, { headers: header }).then(({ data }) => {
            if (data.code === 200) {
                if (success) success(data.data); // 兼容旧的回调方式
                resolve(data.data);
            } else {
                if (failure) failure(data.message, data.code, url); // 兼容旧的回调方式
                reject(data);
            }
        }).catch(err => {
            if (error) error(err);
            reject(err);
        });
    });
}

//保存token
function saveToken(token, remember, expire){
    if (!token) {
        console.warn('尝试保存空token')
        return
    }

    const authObj = {
        token,
        remember,
        expire: expire || new Date(Date.now() + 24*60*60*1000) // 如果没有过期时间，默认24小时
    }

    const str = JSON.stringify(authObj)
    console.log('保存token:', {remember, expire: authObj.expire})

    if(remember){
        localStorage.setItem(authItemName, str)
        sessionStorage.removeItem(authItemName) // 清除可能存在的session storage中的token
    } else {
        sessionStorage.setItem(authItemName, str)
        localStorage.removeItem(authItemName) // 清除可能存在的local storage中的token
    }
}

function deleteToken(){
    localStorage.removeItem(authItemName)
    sessionStorage.removeItem(authItemName)
}

//取出token
function takeAccessToken(){
    //从存储token的地方取出token
    const str = localStorage.getItem(authItemName)||sessionStorage.getItem(authItemName)

    if(!str) return null

    try {
        const authobj = JSON.parse(str)//转化回json对象
        
        // 检查token是否存在
        if (!authobj.token) {
            console.warn('Token对象中没有token字段')
            return null
        }

        // 检查过期时间
        if (authobj.expire) {
            const expireDate = new Date(authobj.expire)
            const now = new Date()
            if (expireDate <= now) {
                console.warn('Token已过期:', {expire: expireDate, now: now})
                deleteToken()
                return null
            }
        }

        // Token有效
        return authobj.token
    } catch (error) {
        console.error('Token解析错误:', error, '原始数据:', str)
        deleteToken()
        return null
    }
}

function login(username, password, remember,captcha,captchaId, success, failure = defaultFailure) {//将其暴露出去
    internalPost('user/login', {
        username:username,
        password:password,
        captcha:captcha,
        captchaId:captchaId

    },{
        'Content-Type':'application/x-www-form-urlencoded'
    },(data)=>{
        saveToken(data.token,remember,data.expire)
        ElMessage.success(`登录成功，欢迎 ${data.nickname} 进入橘子云盘`)
        success()
    },failure)
}
//获取请求头
function getHeader(){
    const token=takeAccessToken();
    return token?{'Authorization': `Bearer ${takeAccessToken()}`} : {}

}
//给外部用的
function get(url,success,failure=defaultFailure){
    return internalGet(url,getHeader(),success,failure)
}
function post(url,data,success,failure=defaultFailure){
    return internalPost(url,data,getHeader(),success,failure)
}


function logout(success,failure=defaultFailure){
    get('user/logout',()=>{

        deleteToken()

        ElMessage.success('退出登录成功，恭候您的再次使用')

        success()
    },failure)
}

//判断是否没有登录了
function  unAuthorize(){
    return !takeAccessToken();
}

export {login,logout,get,post,unAuthorize,getHeader}//暴露login这个函数，给其他地方使用
import axios from 'axios'
import {ElMessage} from "element-plus";
const authItemName ='access_token'

const defaultFailure=(message,code,url)=>{
    console.warn(`请求地址：${url},状态码：${code},错误信息：${message}`)
    ElMessage.warning(message)
}

const  defaultError=(err)=>{
    console.error(err)

    ElMessage.warning('发生了一些错误，请联系管理员')

}

function internalPost(url,data,header,success,failure,error=defaultError) {
    axios.post(url, data, {headers: header}).then(({data}) => {
        if (data.code === 200) {
            success(data.data)
        } else {
            failure(data.message, data.code, url)
        }
    }).catch(err => error(err))
}

function internalGet(url, header, success, failure, error = defaultError) {
    axios.get(url, {headers: header}).then(({data}) => {

        if (data.code === 200) {
            success(data.data)
        } else {

            failure(data.message, data.code, url)
        }
    }).catch(err => error(err))
}

//保存token
function saveToken(token,remember,expire){
    const authObj={token,remember,expire}

    const  str=JSON.stringify(authObj)//转成字符串,因为下面要存储

    if(remember){
        localStorage.setItem(authItemName,str)//只支持字符串的键值对存储
    }
    else{
        sessionStorage.setItem(authItemName,str)
    }
}

function deleteToken(){
    localStorage.removeItem(authItemName)
    sessionStorage.removeItem(authItemName)
}

//取出token
function  takeAccessToken(){
    //从存储token的地方取出token
    const str=localStorage.getItem(authItemName)||sessionStorage.getItem(authItemName)

    if(!str)return null


    const authobj=JSON.parse(str)//转化回json对象
    if(authobj.expire<=new Date()){
        //过期删除
        deleteToken()
        ElMessage.warning('登录状态已经过期了，请您重新登录')
        return null
    }
    return authobj.token


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

    internalGet(url,getHeader(),success,failure)

}
function post(url,data,success,failure=defaultFailure){
    internalPost(url,data,getHeader(),success,failure)
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
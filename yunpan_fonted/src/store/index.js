import{defineStore} from "pinia";
import axios from "axios";
export const userStore=defineStore('general',{
    state:()=>{
        return{
            user:{
                id:-1,
                username:'',
                email:'',
                avatar: null,
                role:0,
                registerTime:null,
                 nickname:'',

            }


        }
    }

})
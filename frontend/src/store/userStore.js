import { defineStore } from "pinia";

export const useUserStore = defineStore('email',{
    state : () => ({
        email : null
    }),

    actions :{
        setEmail(email){
            this.email = email;
        },
        clearEmail(){
            this.email=null;
        }
    }
})
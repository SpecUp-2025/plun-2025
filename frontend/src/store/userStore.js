import { defineStore } from "pinia";

export const useUserStore = defineStore('user',{
    state : () => ({
        user : null
    }),

    actions :{
        setUser(user){
            this.user = user;
        },
        removeUser(){
            this.user=null;
        },
        
    },
    persist: {
        storage : localStorage,
        paths:['users'],
    }
})